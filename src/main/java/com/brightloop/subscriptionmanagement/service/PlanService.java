package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.CreatePlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.RenewPlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdatePlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.PlanProductDto;
import com.brightloop.subscriptionmanagement.dto.response.PlanResponseDto;
import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.Plan;
import com.brightloop.subscriptionmanagement.entity.PlanStatus;
import com.brightloop.subscriptionmanagement.entity.Product;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing Subscription Plan operations, lifecycles, and scheduled auto-closures.
 *
 * Enforces business rules:
 *  - prices cannot be negative
 *  - minQuantity must be at least 1
 *  - endDate must be strictly after startDate
 *  - closable / pausable / renewable operational flags
 *  - auto-closure for expired plans with autoClose=true
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;
    private final ProductRepository productRepository;

    /**
     * Create a new Subscription Plan.
     */
    @Transactional
    public PlanResponseDto createPlan(CreatePlanRequestDto request) {
        log.info("Creating subscription plan: {}", request.getName());

        validatePlanDatesAndPricing(request.getPrice(), request.getMinQuantity(), request.getStartDate(), request.getEndDate());

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findByIdAndActiveTrue(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        }

        Plan plan = Plan.builder()
                .name(request.getName().trim())
                .product(product)
                .products(resolveProducts(request.getProductIds()))
                .price(request.getPrice())
                .billingPeriod(request.getBillingPeriod())
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .autoClose(request.isAutoClose())
                .closable(request.isClosable())
                .pausable(request.isPausable())
                .renewable(request.isRenewable())
                .status(PlanStatus.ACTIVE)
                .active(true)
                .build();

        Plan saved = planRepository.save(plan);
        return mapToDto(saved);
    }

    /**
     * Update an existing Subscription Plan.
     */
    @Transactional
    public PlanResponseDto updatePlan(Long id, UpdatePlanRequestDto request) {
        log.info("Updating plan id: {}", id);

        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        validatePlanDatesAndPricing(request.getPrice(), request.getMinQuantity(), request.getStartDate(), request.getEndDate());

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findByIdAndActiveTrue(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        }

        if (request.getName() != null) {
            plan.setName(request.getName().trim());
        }
        if (request.getProductIds() != null) {
            plan.setProducts(resolveProducts(request.getProductIds()));
        }
        if (request.getProductId() != null) {
            plan.setProduct(product);
        }
        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }
        if (request.getBillingPeriod() != null) {
            plan.setBillingPeriod(request.getBillingPeriod());
        }
        if (request.getMinQuantity() != null) {
            plan.setMinQuantity(request.getMinQuantity());
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            plan.setEndDate(request.getEndDate());
        }
        if (request.getAutoClose() != null) {
            plan.setAutoClose(request.getAutoClose());
        }
        if (request.getClosable() != null) {
            plan.setClosable(request.getClosable());
        }
        if (request.getPausable() != null) {
            plan.setPausable(request.getPausable());
        }
        if (request.getRenewable() != null) {
            plan.setRenewable(request.getRenewable());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        Plan updated = planRepository.save(plan);
        return mapToDto(updated);
    }

    /**
     * Get plan by ID.
     */
    @Transactional(readOnly = true)
    public PlanResponseDto getPlanById(Long id) {
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        return mapToDto(plan);
    }

    /**
     * View/list plans for ADMIN and STAFF with optional search by name.
     */
    @Transactional(readOnly = true)
    public Page<PlanResponseDto> getPlans(String search, Pageable pageable) {
        Page<Plan> page;
        if (search != null && !search.trim().isEmpty()) {
            page = planRepository.findByNameContainingIgnoreCaseAndActiveTrue(search.trim(), pageable);
        } else {
            page = planRepository.findByActiveTrue(pageable);
        }
        return page.map(this::mapToDto);
    }

    /**
     * Customer endpoint: view available plans only (active=true, status=ACTIVE, startDate <= today <= endDate).
     */
    @Transactional(readOnly = true)
    public Page<PlanResponseDto> getAvailablePlansForCustomer(String search, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<Plan> page;
        if (search != null && !search.trim().isEmpty()) {
            page = planRepository.findByNameContainingIgnoreCaseAndStatusAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    search.trim(), PlanStatus.ACTIVE, today, today, pageable);
        } else {
            page = planRepository.findByStatusAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    PlanStatus.ACTIVE, today, today, pageable);
        }
        return page.map(this::mapToDto);
    }

    /**
     * Soft delete plan (ADMIN only).
     */
    @Transactional
    public void deletePlan(Long id) {
        log.info("Soft deleting plan id: {}", id);
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        plan.setActive(false);
        planRepository.save(plan);
    }

    // ==========================================
    // PLAN ACTIONS (ADMIN ONLY)
    // ==========================================

    /**
     * Close a plan:
     * - Reject with 400 if closable == false
     * - Reject if already CLOSED
     */
    @Transactional
    public PlanResponseDto closePlan(Long id) {
        log.info("Admin action: Close plan id: {}", id);
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        if (!plan.isClosable()) {
            throw new BusinessRuleException("Plan cannot be closed because it is configured as non-closable (closable=false)");
        }
        if (plan.getStatus() == PlanStatus.CLOSED) {
            throw new BusinessRuleException("Plan is already CLOSED");
        }

        plan.setStatus(PlanStatus.CLOSED);
        Plan updated = planRepository.save(plan);
        return mapToDto(updated);
    }

    /**
     * Pause a plan:
     * - Reject with 400 if pausable == false
     * - Reject if status is not ACTIVE
     */
    @Transactional
    public PlanResponseDto pausePlan(Long id) {
        log.info("Admin action: Pause plan id: {}", id);
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        if (!plan.isPausable()) {
            throw new BusinessRuleException("Plan cannot be paused because it is configured as non-pausable (pausable=false)");
        }
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE plans can be paused (current status: " + plan.getStatus() + ")");
        }

        plan.setStatus(PlanStatus.PAUSED);
        Plan updated = planRepository.save(plan);
        return mapToDto(updated);
    }

    /**
     * Resume a paused plan:
     * - Reject if status is not PAUSED
     */
    @Transactional
    public PlanResponseDto resumePlan(Long id) {
        log.info("Admin action: Resume plan id: {}", id);
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        if (plan.getStatus() != PlanStatus.PAUSED) {
            throw new BusinessRuleException("Only PAUSED plans can be resumed (current status: " + plan.getStatus() + ")");
        }

        plan.setStatus(PlanStatus.ACTIVE);
        Plan updated = planRepository.save(plan);
        return mapToDto(updated);
    }

    /**
     * Renew a plan:
     * - Reject with 400 if renewable == false
     * - Extend endDate (either to provided newEndDate or by 1 billing cycle)
     * - Reset status to ACTIVE if previously CLOSED or PAUSED
     */
    @Transactional
    public PlanResponseDto renewPlan(Long id, RenewPlanRequestDto request) {
        log.info("Admin action: Renew plan id: {}", id);
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        if (!plan.isRenewable()) {
            throw new BusinessRuleException("Plan cannot be renewed because it is configured as non-renewable (renewable=false)");
        }

        LocalDate newEnd;
        if (request != null && request.getNewEndDate() != null) {
            newEnd = request.getNewEndDate();
            if (!newEnd.isAfter(plan.getEndDate())) {
                throw new BusinessRuleException("New end date must be strictly after current end date (" + plan.getEndDate() + ")");
            }
        } else {
            // Calculate extension by 1 billing period
            LocalDate baseDate = plan.getEndDate().isAfter(LocalDate.now()) ? plan.getEndDate() : LocalDate.now();
            newEnd = extendByBillingPeriod(baseDate, plan.getBillingPeriod());
        }

        plan.setEndDate(newEnd);
        plan.setStatus(PlanStatus.ACTIVE);

        Plan updated = planRepository.save(plan);
        return mapToDto(updated);
    }

    /**
     * Scheduled Job: Auto-closes all plans whose endDate has passed and autoClose=true.
     */
    @Transactional
    public int autoCloseExpiredPlans() {
        LocalDate today = LocalDate.now();
        List<Plan> expiredPlans = planRepository.findByAutoCloseTrueAndStatusNotAndEndDateBefore(PlanStatus.CLOSED, today);

        if (expiredPlans.isEmpty()) {
            log.debug("Auto-close scheduler: No expired plans found.");
            return 0;
        }

        log.info("Auto-close scheduler: Found {} plan(s) to close automatically.", expiredPlans.size());
        for (Plan plan : expiredPlans) {
            plan.setStatus(PlanStatus.CLOSED);
            log.info("Auto-closed expired plan: id={}, name={}, endDate={}", plan.getId(), plan.getName(), plan.getEndDate());
        }

        planRepository.saveAll(expiredPlans);
        return expiredPlans.size();
    }

    /**
     * Helper to extend a date by a billing period unit.
     */
    public LocalDate extendByBillingPeriod(LocalDate date, BillingPeriod billingPeriod) {
        if (billingPeriod == null) {
            return date.plusMonths(1);
        }
        return switch (billingPeriod) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    /**
     * Common validation for plan dates and pricing.
     */
    private void validatePlanDatesAndPricing(BigDecimal price, Integer minQuantity, LocalDate startDate, LocalDate endDate) {
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Plan price cannot be negative");
        }
        if (minQuantity != null && minQuantity < 1) {
            throw new BusinessRuleException("Minimum quantity must be at least 1");
        }
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new BusinessRuleException("End date must be strictly after start date");
        }
    }

    /**
     * Loads the active products for the given ids, failing with 404 if any id is unknown or deleted.
     */
    private Set<Product> resolveProducts(List<Long> productIds) {
        Set<Product> products = new LinkedHashSet<>();
        if (productIds == null) {
            return products;
        }
        for (Long productId : productIds) {
            products.add(productRepository.findByIdAndActiveTrue(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId)));
        }
        return products;
    }

    /**
     * Helper to map Plan entity to PlanResponseDto.
     */
    public PlanResponseDto mapToDto(Plan plan) {
        Product product = plan.getProduct();
        return PlanResponseDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .products(plan.getProducts() == null ? List.of() : plan.getProducts().stream()
                        .map(p -> PlanProductDto.builder().id(p.getId()).name(p.getName()).build())
                        .toList())
                .price(plan.getPrice())
                .billingPeriod(plan.getBillingPeriod())
                .minQuantity(plan.getMinQuantity())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .autoClose(plan.isAutoClose())
                .closable(plan.isClosable())
                .pausable(plan.isPausable())
                .renewable(plan.isRenewable())
                .status(plan.getStatus())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
