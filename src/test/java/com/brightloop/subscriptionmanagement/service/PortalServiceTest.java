package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineRequest;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;
import com.brightloop.subscriptionmanagement.dto.request.CreateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.PortalLineRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.PortalSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.*;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.InvoiceRepository;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import com.brightloop.subscriptionmanagement.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the customer portal rules: ownership, server-side pricing,
 * plan availability and the plan's minimum quantity.
 */
@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock private PlanRepository planRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private InvoiceService invoiceService;
    @Mock private PricingService pricingService;

    @InjectMocks
    private PortalService portalService;

    private final LocalDate today = LocalDate.now();
    private User customer;
    private Product product;
    private ProductVariant odooVariant;
    private Plan plan;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(portalService, "autoActivateOnConfirm", true);
        ReflectionTestUtils.setField(portalService, "paymentTerms", List.of("Immediate", "Net 30"));

        customer = User.builder().id(7L).email("asha@example.com").firstName("Asha").lastName("K").role(Role.CUSTOMER).build();
        product = Product.builder().id(1L).name("ERP Suite").salesPrice(new BigDecimal("1000.00")).active(true).build();
        odooVariant = ProductVariant.builder().id(10L).product(product).attribute("Edition").value("Odoo")
                .extraPrice(new BigDecimal("560.00")).active(true).build();
        plan = Plan.builder().id(5L).name("ERP Monthly").billingPeriod(BillingPeriod.MONTHLY).minQuantity(2)
                .price(new BigDecimal("1000.00")).status(PlanStatus.ACTIVE).active(true)
                .startDate(today.minusDays(1)).endDate(today.plusDays(60))
                .products(new HashSet<>(Set.of(product))).build();
    }

    private PortalSubscriptionRequestDto request(Long variantId, int quantity) {
        return PortalSubscriptionRequestDto.builder()
                .planId(5L)
                .startDate(today)
                .paymentTerms("Net 30")
                .lines(List.of(PortalLineRequestDto.builder().productId(1L).variantId(variantId).quantity(quantity).build()))
                .build();
    }

    // ── Ownership ────────────────────────────────────────────────

    @Test
    void getMySubscription_ofAnotherCustomer_isNotFound() {
        when(subscriptionRepository.existsByIdAndCustomerIdAndActiveTrue(99L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> portalService.getMySubscription(99L, customer))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getMyInvoice_ofAnotherCustomer_isNotFound() {
        when(invoiceRepository.findByIdAndSubscriptionCustomerId(42L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalService.getMyInvoice(42L, customer))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Server-side price recalculation ──────────────────────────

    @Test
    void subscribe_pricesOnServer_andUsesJwtCustomer() {
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        when(productVariantRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(odooVariant));
        when(subscriptionService.createSubscription(any())).thenReturn(SubscriptionResponseDto.builder().id(100L).build());
        when(subscriptionService.changeStatus(anyLong(), any())).thenReturn(SubscriptionResponseDto.builder().id(100L).build());

        portalService.subscribe(request(10L, 3), customer);

        ArgumentCaptor<CreateSubscriptionRequestDto> created = ArgumentCaptor.forClass(CreateSubscriptionRequestDto.class);
        verify(subscriptionService).createSubscription(created.capture());
        assertThat(created.getValue().getCustomerId()).isEqualTo(7L);
        // 1000 sales price + 560 variant extra, regardless of anything the client sent
        assertThat(created.getValue().getLines().get(0).getUnitPrice()).isEqualByComparingTo("1560.00");
        assertThat(created.getValue().getLines().get(0).getVariantId()).isEqualTo(10L);
        assertThat(created.getValue().getPaymentTerms()).isEqualTo("Net 30");
        assertThat(created.getValue().getExpiryDate()).isEqualTo(plan.getEndDate());

        InOrder lifecycle = inOrder(subscriptionService);
        lifecycle.verify(subscriptionService).changeStatus(100L, SubscriptionStatus.QUOTATION);
        lifecycle.verify(subscriptionService).changeStatus(100L, SubscriptionStatus.CONFIRMED);
        lifecycle.verify(subscriptionService).changeStatus(100L, SubscriptionStatus.ACTIVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void preview_pricesOnServer_withoutSaving() {
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        when(productVariantRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(odooVariant));
        when(pricingService.calculate(anyList())).thenReturn(PricingResult.builder().build());

        portalService.preview(request(10L, 2));

        ArgumentCaptor<List<PricingLineRequest>> lines = ArgumentCaptor.forClass(List.class);
        verify(pricingService).calculate(lines.capture());
        assertThat(lines.getValue().get(0).getUnitPrice()).isEqualByComparingTo("1560.00");
        assertThat(lines.getValue().get(0).getQuantity()).isEqualTo(2);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void subscribe_withoutVariant_usesProductSalesPrice() {
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        when(subscriptionService.createSubscription(any())).thenReturn(SubscriptionResponseDto.builder().id(100L).build());
        when(subscriptionService.changeStatus(anyLong(), any())).thenReturn(SubscriptionResponseDto.builder().id(100L).build());

        portalService.subscribe(request(null, 2), customer);

        ArgumentCaptor<CreateSubscriptionRequestDto> created = ArgumentCaptor.forClass(CreateSubscriptionRequestDto.class);
        verify(subscriptionService).createSubscription(created.capture());
        assertThat(created.getValue().getLines().get(0).getUnitPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void subscribe_withVariantOfAnotherProduct_isRejected() {
        Product other = Product.builder().id(2L).name("Other").salesPrice(BigDecimal.ONE).active(true).build();
        ProductVariant foreignVariant = ProductVariant.builder().id(11L).product(other).extraPrice(BigDecimal.TEN).active(true).build();
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        when(productVariantRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(foreignVariant));

        assertThatThrownBy(() -> portalService.subscribe(request(11L, 2), customer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available");
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void subscribe_withProductNotInPlan_isRejected() {
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        PortalSubscriptionRequestDto req = request(null, 2);
        req.getLines().get(0).setProductId(2L);

        assertThatThrownBy(() -> portalService.subscribe(req, customer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not part of plan");
    }

    // ── Plan availability ────────────────────────────────────────

    @Test
    void pausedPlan_isRejected() {
        plan.setStatus(PlanStatus.PAUSED);
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> portalService.preview(request(null, 2)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("paused");
    }

    @Test
    void closedPlan_isRejected() {
        plan.setStatus(PlanStatus.CLOSED);
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> portalService.subscribe(request(null, 2), customer))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("closed");
    }

    @Test
    void expiredPlan_isRejected() {
        plan.setStartDate(today.minusDays(30));
        plan.setEndDate(today.minusDays(1));
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> portalService.getAvailablePlan(5L))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("expired");
    }

    @Test
    void notStartedPlan_isRejected() {
        plan.setStartDate(today.plusDays(1));
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> portalService.getAvailablePlan(5L))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("has not started");
    }

    // ── Minimum quantity ─────────────────────────────────────────

    @Test
    void quantityBelowPlanMinimum_isRejected() {
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> portalService.subscribe(request(null, 1), customer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 2");
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void quantityEqualToPlanMinimum_isAccepted() {
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        when(pricingService.calculate(anyList())).thenReturn(PricingResult.builder().build());

        portalService.preview(request(null, 2));

        verify(pricingService).calculate(anyList());
    }

    // ── Activation flag ──────────────────────────────────────────

    @Test
    void autoActivateOff_leavesSubscriptionConfirmed() {
        ReflectionTestUtils.setField(portalService, "autoActivateOnConfirm", false);
        when(planRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(plan));
        when(subscriptionService.createSubscription(any())).thenReturn(SubscriptionResponseDto.builder().id(100L).build());
        when(subscriptionService.changeStatus(anyLong(), any()))
                .thenReturn(SubscriptionResponseDto.builder().id(100L).status(SubscriptionStatus.CONFIRMED).build());

        SubscriptionResponseDto result = portalService.subscribe(request(null, 2), customer);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CONFIRMED);
        verify(subscriptionService, never()).changeStatus(100L, SubscriptionStatus.ACTIVE);
    }

    @Test
    void manualActivate_ofAnotherCustomersSubscription_isNotFound() {
        ReflectionTestUtils.setField(portalService, "autoActivateOnConfirm", false);
        when(subscriptionRepository.existsByIdAndCustomerIdAndActiveTrue(100L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> portalService.activate(100L, customer))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(subscriptionService);
    }
}
