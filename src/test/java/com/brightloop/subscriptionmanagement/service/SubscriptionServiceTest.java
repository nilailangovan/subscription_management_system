package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineResult;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;
import com.brightloop.subscriptionmanagement.dto.request.CreateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.SubscriptionLineRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.*;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import com.brightloop.subscriptionmanagement.repository.SubscriptionRepository;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionService:
 * - Strict lifecycle status transitions (DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE -> CLOSED)
 * - Updates only allowed in DRAFT
 * - Plan minQuantity validation
 * - Date constraints (expiryDate > startDate)
 * - PricingService calculation delegation
 * - RBAC and customer ownership scoping
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User adminUser;
    private User staffUser;
    private User customer1;
    private User customer2;
    private Plan goldPlan;
    private Product erpProduct;
    private Subscription sampleSubscription;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@looppay.com")
                .role(Role.ADMIN)
                .active(true)
                .build();

        staffUser = User.builder()
                .id(2L)
                .firstName("Staff")
                .lastName("User")
                .email("staff@looppay.com")
                .role(Role.STAFF)
                .active(true)
                .build();

        customer1 = User.builder()
                .id(10L)
                .firstName("Alice")
                .lastName("Customer")
                .email("alice@example.com")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        customer2 = User.builder()
                .id(20L)
                .firstName("Bob")
                .lastName("Customer")
                .email("bob@example.com")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        erpProduct = Product.builder()
                .id(100L)
                .name("Cloud ERP")
                .type(ProductType.SOFTWARE)
                .salesPrice(new BigDecimal("500.00"))
                .costPrice(new BigDecimal("200.00"))
                .active(true)
                .build();

        goldPlan = Plan.builder()
                .id(50L)
                .name("Enterprise Annual")
                .price(new BigDecimal("1000.00"))
                .billingPeriod(BillingPeriod.YEARLY)
                .minQuantity(2)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusYears(1))
                .status(PlanStatus.ACTIVE)
                .active(true)
                .build();

        SubscriptionLine line = SubscriptionLine.builder()
                .id(1L)
                .product(erpProduct)
                .quantity(3)
                .unitPrice(new BigDecimal("500.00"))
                .tax(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .amount(new BigDecimal("1500.00"))
                .build();

        sampleSubscription = Subscription.builder()
                .id(500L)
                .subscriptionNumber("SUB-0001")
                .customer(customer1)
                .plan(goldPlan)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(12))
                .paymentTerms("Immediate Payment")
                .status(SubscriptionStatus.DRAFT)
                .subtotal(new BigDecimal("1500.00"))
                .totalTax(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("1500.00"))
                .lines(new ArrayList<>(List.of(line)))
                .active(true)
                .build();

        line.setSubscription(sampleSubscription);
    }

    // =========================================================================
    // CREATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Create subscription successfully in DRAFT status with auto numbering")
    void testCreateSubscription_Success() {
        CreateSubscriptionRequestDto request = CreateSubscriptionRequestDto.builder()
                .customerId(10L)
                .planId(50L)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .paymentTerms("Net 30")
                .lines(List.of(
                        SubscriptionLineRequestDto.builder()
                                .productId(100L)
                                .quantity(3)
                                .unitPrice(new BigDecimal("500.00"))
                                .build()
                ))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer1));
        when(planRepository.findByIdAndActiveTrue(50L)).thenReturn(Optional.of(goldPlan));
        when(productRepository.findByIdAndActiveTrue(100L)).thenReturn(Optional.of(erpProduct));
        when(subscriptionRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(subscriptionRepository.existsBySubscriptionNumber("SUB-0001")).thenReturn(false);

        PricingResult stubPricing = PricingResult.builder()
                .lineResults(List.of(
                        PricingLineResult.builder()
                                .productId(100L)
                                .quantity(3)
                                .unitPrice(new BigDecimal("500.00"))
                                .taxAmount(BigDecimal.ZERO)
                                .discountAmount(BigDecimal.ZERO)
                                .lineTotal(new BigDecimal("1500.00"))
                                .build()
                ))
                .subtotal(new BigDecimal("1500.00"))
                .totalTax(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("1500.00"))
                .build();

        when(pricingService.calculate(anyList())).thenReturn(stubPricing);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        SubscriptionResponseDto response = subscriptionService.createSubscription(request);

        assertNotNull(response);
        assertEquals("SUB-0001", response.getSubscriptionNumber());
        assertEquals(SubscriptionStatus.DRAFT, response.getStatus());
        assertEquals(new BigDecimal("1500.00"), response.getGrandTotal());
        assertEquals(1, response.getLines().size());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Create subscription fails when expiryDate is before or equal to startDate")
    void testCreateSubscription_InvalidDates() {
        CreateSubscriptionRequestDto request = CreateSubscriptionRequestDto.builder()
                .customerId(10L)
                .planId(50L)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().minusDays(1))
                .paymentTerms("Net 30")
                .lines(List.of(SubscriptionLineRequestDto.builder().productId(100L).quantity(2).build()))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.createSubscription(request));
        assertTrue(ex.getMessage().contains("Expiry date must be strictly after"));
    }

    @Test
    @DisplayName("Create subscription fails when total quantity is less than plan's minQuantity")
    void testCreateSubscription_MinQuantityViolation() {
        // Plan has minQuantity = 2, request has quantity = 1
        CreateSubscriptionRequestDto request = CreateSubscriptionRequestDto.builder()
                .customerId(10L)
                .planId(50L)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(6))
                .paymentTerms("Immediate Payment")
                .lines(List.of(
                        SubscriptionLineRequestDto.builder()
                                .productId(100L)
                                .quantity(1)
                                .build()
                ))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer1));
        when(planRepository.findByIdAndActiveTrue(50L)).thenReturn(Optional.of(goldPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.createSubscription(request));
        assertTrue(ex.getMessage().contains("cannot be less than the plan's minimum required quantity"));
    }

    @Test
    @DisplayName("Create subscription fails when customer does not exist")
    void testCreateSubscription_CustomerNotFound() {
        CreateSubscriptionRequestDto request = CreateSubscriptionRequestDto.builder()
                .customerId(999L)
                .planId(50L)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(6))
                .paymentTerms("Immediate Payment")
                .lines(List.of(SubscriptionLineRequestDto.builder().productId(100L).quantity(2).build()))
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                subscriptionService.createSubscription(request));
    }

    // =========================================================================
    // UPDATE RULES (Only allowed in DRAFT)
    // =========================================================================

    @Test
    @DisplayName("Update subscription succeeds when in DRAFT status")
    void testUpdateSubscription_SuccessInDraft() {
        UpdateSubscriptionRequestDto request = UpdateSubscriptionRequestDto.builder()
                .planId(50L)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(6))
                .paymentTerms("Net 15")
                .lines(List.of(
                        SubscriptionLineRequestDto.builder()
                                .productId(100L)
                                .quantity(2)
                                .unitPrice(new BigDecimal("500.00"))
                                .build()
                ))
                .build();

        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));
        when(planRepository.findByIdAndActiveTrue(50L)).thenReturn(Optional.of(goldPlan));
        when(productRepository.findByIdAndActiveTrue(100L)).thenReturn(Optional.of(erpProduct));

        PricingResult pricingResult = PricingResult.builder()
                .lineResults(List.of(
                        PricingLineResult.builder()
                                .productId(100L)
                                .quantity(2)
                                .unitPrice(new BigDecimal("500.00"))
                                .taxAmount(BigDecimal.ZERO)
                                .discountAmount(BigDecimal.ZERO)
                                .lineTotal(new BigDecimal("1000.00"))
                                .build()
                ))
                .subtotal(new BigDecimal("1000.00"))
                .totalTax(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("1000.00"))
                .build();

        when(pricingService.calculate(anyList())).thenReturn(pricingResult);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sampleSubscription);

        SubscriptionResponseDto response = subscriptionService.updateSubscription(500L, request);

        assertNotNull(response);
        assertEquals("Net 15", response.getPaymentTerms());
        verify(subscriptionRepository).save(sampleSubscription);
    }

    @Test
    @DisplayName("Update subscription fails when status is not DRAFT")
    void testUpdateSubscription_ThrowsWhenNotInDraft() {
        sampleSubscription.setStatus(SubscriptionStatus.QUOTATION);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        UpdateSubscriptionRequestDto request = UpdateSubscriptionRequestDto.builder()
                .planId(50L)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(6))
                .paymentTerms("Net 15")
                .lines(List.of(SubscriptionLineRequestDto.builder().productId(100L).quantity(2).build()))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.updateSubscription(500L, request));
        assertTrue(ex.getMessage().contains("only be updated while in DRAFT status"));
    }

    // =========================================================================
    // STRICT LIFECYCLE STATUS TRANSITION TESTS
    // =========================================================================

    @Test
    @DisplayName("Transition: DRAFT -> QUOTATION succeeds")
    void testChangeStatus_DraftToQuotation() {
        sampleSubscription.setStatus(SubscriptionStatus.DRAFT);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));
        when(subscriptionRepository.save(any())).thenReturn(sampleSubscription);

        SubscriptionResponseDto response = subscriptionService.changeStatus(500L, SubscriptionStatus.QUOTATION);

        assertEquals(SubscriptionStatus.QUOTATION, response.getStatus());
    }

    @Test
    @DisplayName("Transition: QUOTATION -> CONFIRMED succeeds")
    void testChangeStatus_QuotationToConfirmed() {
        sampleSubscription.setStatus(SubscriptionStatus.QUOTATION);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));
        when(subscriptionRepository.save(any())).thenReturn(sampleSubscription);

        SubscriptionResponseDto response = subscriptionService.changeStatus(500L, SubscriptionStatus.CONFIRMED);

        assertEquals(SubscriptionStatus.CONFIRMED, response.getStatus());
    }

    @Test
    @DisplayName("Transition: CONFIRMED -> ACTIVE succeeds")
    void testChangeStatus_ConfirmedToActive() {
        sampleSubscription.setStatus(SubscriptionStatus.CONFIRMED);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));
        when(subscriptionRepository.save(any())).thenReturn(sampleSubscription);

        SubscriptionResponseDto response = subscriptionService.changeStatus(500L, SubscriptionStatus.ACTIVE);

        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Transition: ACTIVE -> CLOSED succeeds")
    void testChangeStatus_ActiveToClosed() {
        sampleSubscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));
        when(subscriptionRepository.save(any())).thenReturn(sampleSubscription);

        SubscriptionResponseDto response = subscriptionService.changeStatus(500L, SubscriptionStatus.CLOSED);

        assertEquals(SubscriptionStatus.CLOSED, response.getStatus());
    }

    @Test
    @DisplayName("Invalid Transition: DRAFT -> ACTIVE rejected (400 BusinessRuleException)")
    void testChangeStatus_Invalid_DraftToActive() {
        sampleSubscription.setStatus(SubscriptionStatus.DRAFT);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.changeStatus(500L, SubscriptionStatus.ACTIVE));
        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("Invalid Transition: Backward CONFIRMED -> QUOTATION rejected")
    void testChangeStatus_Invalid_Backward() {
        sampleSubscription.setStatus(SubscriptionStatus.CONFIRMED);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.changeStatus(500L, SubscriptionStatus.QUOTATION));
        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("Invalid Transition: Cannot transition from CLOSED")
    void testChangeStatus_Invalid_FromClosed() {
        sampleSubscription.setStatus(SubscriptionStatus.CLOSED);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.changeStatus(500L, SubscriptionStatus.ACTIVE));
        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("Invalid Transition: Same status transition rejected")
    void testChangeStatus_SameStatus() {
        sampleSubscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.changeStatus(500L, SubscriptionStatus.ACTIVE));
        assertTrue(ex.getMessage().contains("already in ACTIVE status"));
    }

    // =========================================================================
    // RBAC & OWNERSHIP TESTS
    // =========================================================================

    @Test
    @DisplayName("ADMIN can view any customer's subscription")
    void testGetSubscriptionById_AdminAccess() {
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        SubscriptionResponseDto response = subscriptionService.getSubscriptionById(500L, adminUser);
        assertNotNull(response);
        assertEquals("SUB-0001", response.getSubscriptionNumber());
    }

    @Test
    @DisplayName("STAFF can view any customer's subscription")
    void testGetSubscriptionById_StaffAccess() {
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        SubscriptionResponseDto response = subscriptionService.getSubscriptionById(500L, staffUser);
        assertNotNull(response);
        assertEquals("SUB-0001", response.getSubscriptionNumber());
    }

    @Test
    @DisplayName("CUSTOMER can view their own subscription")
    void testGetSubscriptionById_CustomerOwnAccess() {
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        SubscriptionResponseDto response = subscriptionService.getSubscriptionById(500L, customer1);
        assertNotNull(response);
        assertEquals("SUB-0001", response.getSubscriptionNumber());
    }

    @Test
    @DisplayName("CUSTOMER cannot view another customer's subscription (Throws AccessDeniedException)")
    void testGetSubscriptionById_CustomerOtherAccess_Forbidden() {
        when(subscriptionRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(sampleSubscription));

        assertThrows(AccessDeniedException.class, () ->
                subscriptionService.getSubscriptionById(500L, customer2));
    }

    @Test
    @DisplayName("Customer listing is automatically restricted to their own customerId")
    void testGetSubscriptions_CustomerScoping() {
        Pageable pageable = PageRequest.of(0, 10);
        when(subscriptionRepository.findByCustomerIdAndActiveTrue(eq(10L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleSubscription)));

        Page<SubscriptionResponseDto> result = subscriptionService.getSubscriptions(null, null, pageable, customer1);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(subscriptionRepository).findByCustomerIdAndActiveTrue(10L, pageable);
        verify(subscriptionRepository, never()).findByActiveTrue(any());
    }

    @Test
    @DisplayName("Admin listing queries all active subscriptions")
    void testGetSubscriptions_AdminAccessAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(subscriptionRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleSubscription)));

        Page<SubscriptionResponseDto> result = subscriptionService.getSubscriptions(null, null, pageable, adminUser);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(subscriptionRepository).findByActiveTrue(pageable);
    }
}

