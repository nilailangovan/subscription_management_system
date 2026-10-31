package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineRequest;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineResult;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;
import com.brightloop.subscriptionmanagement.dto.request.CreateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.SubscriptionLineRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionLineResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.*;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import com.brightloop.subscriptionmanagement.repository.SubscriptionRepository;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing subscription creation, lifecycle transitions, updates, and access controls.
 *
 * Rules strictly enforced:
 *  1. Auto-generated human-readable subscription number (SUB-0001, SUB-0002...).
 *  2. Lifecycle transitions: DRAFT → QUOTATION → CONFIRMED → ACTIVE → CLOSED.
 *  3. Updates permitted only in DRAFT status.
 *  4. Total subscription quantity must be >= Plan's minQuantity.
 *  5. Pricing calculation delegated to PricingService.
 *  6. CUSTOMER role can only view their own subscriptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final ProductVariantRepository productVariantRepository;
    private final InvoiceService invoiceService;

    // =========================================================================
    // SUBSCRIPTION CREATION
    // =========================================================================

    /**
     * Creates a new subscription in DRAFT status.
     * Accessible by ADMIN and STAFF.
     */
    @Transactional
    public SubscriptionResponseDto createSubscription(CreateSubscriptionRequestDto request) {
        log.info("Creating subscription for customer id: {}, plan id: {}", request.getCustomerId(), request.getPlanId());

        // 1. Validate dates
        validateDates(request.getStartDate(), request.getExpiryDate());

        // 2. Fetch and validate Customer
        User customer = userRepository.findById(request.getCustomerId())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        // 3. Fetch and validate Plan
        Plan plan = planRepository.findByIdAndActiveTrue(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        // 4. Validate quantity against plan's minQuantity
        validatePlanQuantity(plan, request.getLines());

        // 5. Generate unique subscription number (SUB-0001 format)
        String subscriptionNumber = generateSubscriptionNumber();

        // 6. Build lines and execute pricing calculation
        Subscription subscription = Subscription.builder()
                .subscriptionNumber(subscriptionNumber)
                .customer(customer)
                .plan(plan)
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .paymentTerms(request.getPaymentTerms())
                .status(SubscriptionStatus.DRAFT)
                .active(true)
                .lines(new ArrayList<>())
                .build();

        populateAndPriceLines(subscription, request.getLines());

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created successfully: {}", saved.getSubscriptionNumber());

        return mapToResponseDto(saved);
    }

    // =========================================================================
    // SUBSCRIPTION UPDATE (Only in DRAFT)
    // =========================================================================

    /**
     * Updates an existing subscription.
     * Allowed only when the subscription is in DRAFT status.
     */
    @Transactional
    public SubscriptionResponseDto updateSubscription(Long id, UpdateSubscriptionRequestDto request) {
        log.info("Updating subscription id: {}", id);

        Subscription subscription = findSubscriptionById(id);

        // Strict business rule: Only DRAFT subscriptions can be modified
        if (subscription.getStatus() != SubscriptionStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Subscription can only be updated while in DRAFT status. Current status: " + subscription.getStatus()
            );
        }

        // Validate dates
        validateDates(request.getStartDate(), request.getExpiryDate());

        // Fetch plan
        Plan plan = planRepository.findByIdAndActiveTrue(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        // Validate minQuantity
        validatePlanQuantity(plan, request.getLines());

        subscription.setPlan(plan);
        subscription.setStartDate(request.getStartDate());
        subscription.setExpiryDate(request.getExpiryDate());
        subscription.setPaymentTerms(request.getPaymentTerms());

        // Clear existing lines and re-price new ones
        subscription.getLines().clear();
        populateAndPriceLines(subscription, request.getLines());

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} updated successfully", updated.getSubscriptionNumber());

        return mapToResponseDto(updated);
    }

    // =========================================================================
    // LIFECYCLE STATUS TRANSITIONS
    // =========================================================================

    /**
     * Changes the lifecycle status of a subscription.
     * Strictly enforces valid forward state transitions:
     *   DRAFT → QUOTATION → CONFIRMED → ACTIVE → CLOSED
     */
    @Transactional
    public SubscriptionResponseDto changeStatus(Long id, SubscriptionStatus targetStatus) {
        log.info("Transitioning subscription id: {} to status: {}", id, targetStatus);

        Subscription subscription = findSubscriptionById(id);
        SubscriptionStatus currentStatus = subscription.getStatus();

        if (currentStatus == targetStatus) {
            throw new BusinessRuleException("Subscription is already in " + currentStatus + " status.");
        }

        boolean validTransition = switch (currentStatus) {
            case DRAFT -> targetStatus == SubscriptionStatus.QUOTATION;
            case QUOTATION -> targetStatus == SubscriptionStatus.CONFIRMED;
            case CONFIRMED -> targetStatus == SubscriptionStatus.ACTIVE;
            case ACTIVE -> targetStatus == SubscriptionStatus.CLOSED;
            case CLOSED -> false;
        };

        if (!validTransition) {
            throw new BusinessRuleException(String.format(
                    "Invalid status transition from %s to %s. Allowed sequence: DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE -> CLOSED.",
                    currentStatus, targetStatus
            ));
        }

        subscription.setStatus(targetStatus);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} transitioned from {} to {}", updated.getSubscriptionNumber(), currentStatus, targetStatus);

        // Activation bills the first period (idempotent, so re-activation never duplicates it)
        if (targetStatus == SubscriptionStatus.ACTIVE) {
            invoiceService.generateFirstInvoice(updated);
        }

        return mapToResponseDto(updated);
    }

    // =========================================================================
    // READ / QUERY WITH RBAC & OWNERSHIP RULES
    // =========================================================================

    /**
     * Retrieves a single subscription by ID with strict ownership validation:
     *  - ADMIN / STAFF can view any subscription.
     *  - CUSTOMER can view only their own subscription.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getSubscriptionById(Long id, User currentUser) {
        Subscription subscription = findSubscriptionById(id);

        if (currentUser.getRole() == Role.CUSTOMER) {
            if (!subscription.getCustomer().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this subscription.");
            }
        }

        return mapToResponseDto(subscription);
    }

    /**
     * Lists subscriptions with pagination, optional status filter, and search.
     *  - ADMIN / STAFF: Can query all subscriptions across all customers.
     *  - CUSTOMER: Restricted strictly to their own subscriptions.
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponseDto> getSubscriptions(
            String search, SubscriptionStatus status, Pageable pageable, User currentUser) {

        Page<Subscription> page;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (currentUser.getRole() == Role.CUSTOMER) {
            // Customer is strictly scoped to their own account
            Long customerId = currentUser.getId();
            if (status != null) {
                page = hasSearch
                        ? subscriptionRepository.findByCustomerIdAndSubscriptionNumberContainingIgnoreCaseAndStatusAndActiveTrue(customerId, search.trim(), status, pageable)
                        : subscriptionRepository.findByCustomerIdAndStatusAndActiveTrue(customerId, status, pageable);
            } else {
                page = hasSearch
                        ? subscriptionRepository.findByCustomerIdAndSubscriptionNumberContainingIgnoreCaseAndActiveTrue(customerId, search.trim(), pageable)
                        : subscriptionRepository.findByCustomerIdAndActiveTrue(customerId, pageable);
            }
        } else {
            // ADMIN / STAFF can query across the entire system
            if (status != null) {
                page = hasSearch
                        ? subscriptionRepository.findBySubscriptionNumberContainingIgnoreCaseAndStatusAndActiveTrue(search.trim(), status, pageable)
                        : subscriptionRepository.findByStatusAndActiveTrue(status, pageable);
            } else {
                page = hasSearch
                        ? subscriptionRepository.findBySubscriptionNumberContainingIgnoreCaseAndActiveTrue(search.trim(), pageable)
                        : subscriptionRepository.findByActiveTrue(pageable);
            }
        }

        return page.map(this::mapToResponseDto);
    }

    // =========================================================================
    // QUOTATION TEMPLATE HOOK (TODO for Phase 4)
    // =========================================================================

    /**
     * TODO: Hook for creating a subscription from a Quotation Template.
     * When the Quotation Template module is built, this method will populate lines and terms
     * from the specified template ID before creating the subscription in DRAFT status.
     *
     * @param templateId ID of the quotation template to clone
     * @param customerId ID of the target customer
     * @return created SubscriptionResponseDto
     */
    public SubscriptionResponseDto createFromQuotationTemplate(Long templateId, Long customerId) {
        // TODO: Implement template-to-subscription mapping once Template entity and repository are merged.
        throw new UnsupportedOperationException("Creating subscriptions from quotation templates will be supported in a future release.");
    }

    // =========================================================================
    // HELPER & VALIDATION METHODS
    // =========================================================================

    private Subscription findSubscriptionById(Long id) {
        return subscriptionRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate expiryDate) {
        if (startDate == null || expiryDate == null) {
            throw new BusinessRuleException("Start date and expiry date are required.");
        }
        if (!expiryDate.isAfter(startDate)) {
            throw new BusinessRuleException("Expiry date must be strictly after the start date.");
        }
    }

    private void validatePlanQuantity(Plan plan, List<SubscriptionLineRequestDto> lineRequests) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new BusinessRuleException("Subscription must contain at least one line item.");
        }

        int totalQuantity = lineRequests.stream()
                .mapToInt(line -> line.getQuantity() != null ? line.getQuantity() : 0)
                .sum();

        if (totalQuantity < plan.getMinQuantity()) {
            throw new BusinessRuleException(String.format(
                    "Total subscription quantity (%d) cannot be less than the plan's minimum required quantity (%d).",
                    totalQuantity, plan.getMinQuantity()
            ));
        }
    }

    /**
     * Populates line items, validates products, and delegates calculations to PricingService.
     */
    private void populateAndPriceLines(Subscription subscription, List<SubscriptionLineRequestDto> lineRequests) {
        List<PricingLineRequest> pricingRequests = new ArrayList<>();
        List<Product> resolvedProducts = new ArrayList<>();

        for (SubscriptionLineRequestDto lineDto : lineRequests) {
            Product product = productRepository.findByIdAndActiveTrue(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + lineDto.getProductId()));

            resolvedProducts.add(product);

            BigDecimal unitPrice = lineDto.getUnitPrice() != null ? lineDto.getUnitPrice() : product.getSalesPrice();

            pricingRequests.add(PricingLineRequest.builder()
                    .productId(product.getId())
                    .quantity(lineDto.getQuantity())
                    .unitPrice(unitPrice)
                    .taxId(lineDto.getTaxId())
                    .build());
        }

        // Delegate to PricingService interface
        PricingResult pricingResult = pricingService.calculate(pricingRequests);

        // Populate subscription order totals
        subscription.setSubtotal(pricingResult.getSubtotal());
        subscription.setTotalTax(pricingResult.getTotalTax());
        subscription.setTotalDiscount(pricingResult.getTotalDiscount());
        subscription.setGrandTotal(pricingResult.getGrandTotal());

        // Build individual SubscriptionLine entities
        for (int i = 0; i < lineRequests.size(); i++) {
            SubscriptionLineRequestDto lineDto = lineRequests.get(i);
            Product product = resolvedProducts.get(i);
            PricingLineResult linePricing = pricingResult.getLineResults().get(i);

            SubscriptionLine lineEntity = SubscriptionLine.builder()
                    .product(product)
                    .variant(resolveVariant(product, lineDto.getVariantId()))
                    .quantity(lineDto.getQuantity())
                    .unitPrice(linePricing.getUnitPrice())
                    .tax(linePricing.getTaxAmount())
                    .discount(linePricing.getDiscountAmount())
                    .amount(linePricing.getLineTotal())
                    .build();

            subscription.addLine(lineEntity);
        }
    }

    /** Loads the optional line variant and checks it belongs to the line's product. */
    private ProductVariant resolveVariant(Product product, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant variant = productVariantRepository.findByIdAndActiveTrue(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with ID: " + variantId));
        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new BusinessRuleException("Variant " + variantId + " does not belong to product " + product.getName() + ".");
        }
        return variant;
    }

    /**
     * Generates a sequential, zero-padded subscription number (e.g. SUB-0001).
     */
    private synchronized String generateSubscriptionNumber() {
        long nextIndex = subscriptionRepository.findTopByOrderByIdDesc()
                .map(s -> s.getId() + 1)
                .orElse(1L);

        String candidate = String.format("SUB-%04d", nextIndex);
        while (subscriptionRepository.existsBySubscriptionNumber(candidate)) {
            nextIndex++;
            candidate = String.format("SUB-%04d", nextIndex);
        }
        return candidate;
    }

    private SubscriptionResponseDto mapToResponseDto(Subscription subscription) {
        List<SubscriptionLineResponseDto> lineDtos = subscription.getLines().stream()
                .map(line -> SubscriptionLineResponseDto.builder()
                        .id(line.getId())
                        .productId(line.getProduct().getId())
                        .productName(line.getProduct().getName())
                        .variantId(line.getVariant() != null ? line.getVariant().getId() : null)
                        .variantName(line.getVariant() != null
                                ? line.getVariant().getAttribute() + ": " + line.getVariant().getValue() : null)
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .tax(line.getTax())
                        .discount(line.getDiscount())
                        .amount(line.getAmount())
                        .build())
                .toList();

        return SubscriptionResponseDto.builder()
                .id(subscription.getId())
                .subscriptionNumber(subscription.getSubscriptionNumber())
                .customerId(subscription.getCustomer().getId())
                .customerName(subscription.getCustomer().getFirstName() + " " + subscription.getCustomer().getLastName())
                .customerEmail(subscription.getCustomer().getEmail())
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .billingPeriod(subscription.getPlan().getBillingPeriod())
                .startDate(subscription.getStartDate())
                .expiryDate(subscription.getExpiryDate())
                .nextBillingDate(BillingCalendar.nextBillingDate(subscription.getStartDate(), subscription.getExpiryDate(),
                        subscription.getPlan().getBillingPeriod(), subscription.getStatus(), LocalDate.now()))
                .paymentTerms(subscription.getPaymentTerms())
                .status(subscription.getStatus())
                .subtotal(subscription.getSubtotal())
                .totalTax(subscription.getTotalTax())
                .totalDiscount(subscription.getTotalDiscount())
                .grandTotal(subscription.getGrandTotal())
                .lines(lineDtos)
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
