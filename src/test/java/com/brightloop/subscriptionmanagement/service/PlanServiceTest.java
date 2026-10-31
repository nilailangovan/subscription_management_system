package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.CreatePlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.RenewPlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.PlanResponseDto;
import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.Plan;
import com.brightloop.subscriptionmanagement.entity.PlanStatus;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlanService:
 * - Date and pricing validation
 * - Plan lifecycle actions: close, pause, resume, renew
 * - Flag enforcement: closable, pausable, renewable
 * - Auto-close scheduler execution
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PlanService planService;

    private Plan standardPlan;

    @BeforeEach
    void setUp() {
        standardPlan = Plan.builder()
                .id(1L)
                .name("Monthly Gold Plan")
                .price(new BigDecimal("199.99"))
                .billingPeriod(BillingPeriod.MONTHLY)
                .minQuantity(1)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusMonths(1))
                .closable(true)
                .pausable(true)
                .renewable(true)
                .autoClose(true)
                .status(PlanStatus.ACTIVE)
                .active(true)
                .build();
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @Test
    @DisplayName("Should throw BusinessRuleException when plan price is negative")
    void testCreatePlan_NegativePrice_ThrowsException() {
        CreatePlanRequestDto dto = new CreatePlanRequestDto();
        dto.setName("Invalid Plan");
        dto.setPrice(new BigDecimal("-10.00"));
        dto.setBillingPeriod(BillingPeriod.MONTHLY);
        dto.setMinQuantity(1);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.createPlan(dto));
        assertTrue(ex.getMessage().contains("Plan price cannot be negative"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when minQuantity is less than 1")
    void testCreatePlan_MinQuantityZero_ThrowsException() {
        CreatePlanRequestDto dto = new CreatePlanRequestDto();
        dto.setName("Invalid Plan");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setBillingPeriod(BillingPeriod.MONTHLY);
        dto.setMinQuantity(0);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.createPlan(dto));
        assertTrue(ex.getMessage().contains("Minimum quantity must be at least 1"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when endDate is on or before startDate")
    void testCreatePlan_EndDateBeforeStartDate_ThrowsException() {
        CreatePlanRequestDto dto = new CreatePlanRequestDto();
        dto.setName("Invalid Date Plan");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setBillingPeriod(BillingPeriod.MONTHLY);
        dto.setMinQuantity(1);
        dto.setStartDate(LocalDate.now().plusDays(10));
        dto.setEndDate(LocalDate.now().plusDays(5)); // Before start date!

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.createPlan(dto));
        assertTrue(ex.getMessage().contains("End date must be strictly after start date"));
    }

    // ==========================================
    // CLOSE ACTION TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully close plan when closable=true")
    void testClosePlan_Success() {
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponseDto response = planService.closePlan(1L);

        assertEquals(PlanStatus.CLOSED, response.getStatus());
        verify(planRepository, times(1)).save(standardPlan);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when closing plan configured as closable=false")
    void testClosePlan_NonClosable_ThrowsException() {
        standardPlan.setClosable(false);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.closePlan(1L));

        assertTrue(ex.getMessage().contains("closable=false"));
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when closing plan that is already CLOSED")
    void testClosePlan_AlreadyClosed_ThrowsException() {
        standardPlan.setStatus(PlanStatus.CLOSED);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.closePlan(1L));

        assertTrue(ex.getMessage().contains("Plan is already CLOSED"));
        verify(planRepository, never()).save(any());
    }

    // ==========================================
    // PAUSE & RESUME ACTION TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully pause active plan when pausable=true")
    void testPausePlan_Success() {
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponseDto response = planService.pausePlan(1L);

        assertEquals(PlanStatus.PAUSED, response.getStatus());
        verify(planRepository, times(1)).save(standardPlan);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when pausing plan configured as pausable=false")
    void testPausePlan_NonPausable_ThrowsException() {
        standardPlan.setPausable(false);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.pausePlan(1L));

        assertTrue(ex.getMessage().contains("pausable=false"));
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when pausing a plan that is not ACTIVE")
    void testPausePlan_NotActive_ThrowsException() {
        standardPlan.setStatus(PlanStatus.CLOSED);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.pausePlan(1L));

        assertTrue(ex.getMessage().contains("Only ACTIVE plans can be paused"));
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully resume a PAUSED plan")
    void testResumePlan_Success() {
        standardPlan.setStatus(PlanStatus.PAUSED);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponseDto response = planService.resumePlan(1L);

        assertEquals(PlanStatus.ACTIVE, response.getStatus());
        verify(planRepository, times(1)).save(standardPlan);
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when resuming a plan that is not PAUSED")
    void testResumePlan_NotPaused_ThrowsException() {
        standardPlan.setStatus(PlanStatus.ACTIVE);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.resumePlan(1L));

        assertTrue(ex.getMessage().contains("Only PAUSED plans can be resumed"));
        verify(planRepository, never()).save(any());
    }

    // ==========================================
    // RENEW ACTION TESTS
    // ==========================================

    @Test
    @DisplayName("Should successfully renew plan with explicit newEndDate")
    void testRenewPlan_WithExplicitNewDate() {
        LocalDate newDate = standardPlan.getEndDate().plusMonths(2);
        RenewPlanRequestDto renewDto = new RenewPlanRequestDto();
        renewDto.setNewEndDate(newDate);

        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponseDto response = planService.renewPlan(1L, renewDto);

        assertEquals(PlanStatus.ACTIVE, response.getStatus());
        assertEquals(newDate, response.getEndDate());
    }

    @Test
    @DisplayName("Should successfully renew plan by extending 1 billing cycle when no date is supplied")
    void testRenewPlan_DefaultBillingPeriodExtension() {
        LocalDate initialEnd = standardPlan.getEndDate();
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponseDto response = planService.renewPlan(1L, null);

        assertEquals(PlanStatus.ACTIVE, response.getStatus());
        assertEquals(initialEnd.plusMonths(1), response.getEndDate());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when renewing plan configured as renewable=false")
    void testRenewPlan_NonRenewable_ThrowsException() {
        standardPlan.setRenewable(false);
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(standardPlan));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> planService.renewPlan(1L, null));

        assertTrue(ex.getMessage().contains("renewable=false"));
        verify(planRepository, never()).save(any());
    }

    // ==========================================
    // AUTO-CLOSE SCHEDULER TESTS
    // ==========================================

    @Test
    @DisplayName("Should auto-close expired plans whose autoClose=true")
    void testAutoCloseExpiredPlans() {
        Plan expiredPlan1 = Plan.builder()
                .id(20L)
                .name("Expired Plan 1")
                .autoClose(true)
                .status(PlanStatus.ACTIVE)
                .endDate(LocalDate.now().minusDays(1))
                .build();

        Plan expiredPlan2 = Plan.builder()
                .id(21L)
                .name("Expired Plan 2")
                .autoClose(true)
                .status(PlanStatus.PAUSED)
                .endDate(LocalDate.now().minusDays(3))
                .build();

        when(planRepository.findByAutoCloseTrueAndStatusNotAndEndDateBefore(eq(PlanStatus.CLOSED), any(LocalDate.class)))
                .thenReturn(List.of(expiredPlan1, expiredPlan2));

        int closedCount = planService.autoCloseExpiredPlans();

        assertEquals(2, closedCount);
        assertEquals(PlanStatus.CLOSED, expiredPlan1.getStatus());
        assertEquals(PlanStatus.CLOSED, expiredPlan2.getStatus());
        verify(planRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Should return 0 when no expired autoClose plans are found")
    void testAutoCloseExpiredPlans_NoneFound() {
        when(planRepository.findByAutoCloseTrueAndStatusNotAndEndDateBefore(eq(PlanStatus.CLOSED), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        int closedCount = planService.autoCloseExpiredPlans();

        assertEquals(0, closedCount);
        verify(planRepository, never()).saveAll(any());
    }
}

