package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineRequest;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;
import com.brightloop.subscriptionmanagement.dto.request.CreateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.PortalLineRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.PortalSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.SubscriptionLineRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.InvoiceResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.PortalPlanResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.*;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.InvoiceRepository;
import com.brightloop.subscriptionmanagement.repository.PlanRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import com.brightloop.subscriptionmanagement.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer self-service: browse subscribable plans, preview prices and subscribe.
 *
 * Rules enforced here:
 *  - The customer always comes from the JWT (passed in by the controller), never from the request.
 *  - Prices are always recalculated on the server: unit price = product sales price + variant extra price.
 *  - Only ACTIVE plans inside their start/end dates can be subscribed to.
 *  - Every line's quantity must be at least the plan's minQuantity.
 *  - A customer can only read their own subscriptions and invoices; anything else is a 404.
 *
 * Subscriptions are created through SubscriptionService, so they show up in the admin/staff list too.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalService {

    private final PlanRepository planRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final PricingService pricingService;

    /** true: a confirmed portal subscription becomes ACTIVE straight away; false: the customer activates it */
    @Value("${app.subscription.auto-activate-on-confirm:true}")
    private boolean autoActivateOnConfirm;

    /** Payment terms customers can choose from (first one is the default) */
    @Value("${app.subscription.payment-terms:Immediate,Net 15,Net 30}")
    private List<String> paymentTerms;

    // =========================================================================
    // PLANS
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<PortalPlanResponseDto> getAvailablePlans(String search, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<Plan> plans = search != null && !search.isBlank()
                ? planRepository.findByNameContainingIgnoreCaseAndStatusAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        search.trim(), PlanStatus.ACTIVE, today, today, pageable)
                : planRepository.findByStatusAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        PlanStatus.ACTIVE, today, today, pageable);
        return plans.map(this::mapPlan);
    }

    @Transactional(readOnly = true)
    public PortalPlanResponseDto getAvailablePlan(Long planId) {
        return mapPlan(loadAvailablePlan(planId));
    }

    public List<String> getPaymentTerms() {
        return paymentTerms;
    }

    // =========================================================================
    // PREVIEW & SUBSCRIBE
    // =========================================================================

    /** Prices the requested lines without saving anything. */
    @Transactional(readOnly = true)
    public PricingResult preview(PortalSubscriptionRequestDto request) {
        Plan plan = loadAvailablePlan(request.getPlanId());
        List<PricingLineRequest> pricingLines = priceLines(plan, request.getLines()).stream()
                .map(line -> PricingLineRequest.builder()
                        .productId(line.getProductId())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .build())
                .toList();
        return pricingService.calculate(pricingLines);
    }

    /**
     * Creates the subscription for the logged-in customer and moves it DRAFT -> QUOTATION -> CONFIRMED,
     * then on to ACTIVE when auto-activation is on.
     */
    @Transactional
    public SubscriptionResponseDto subscribe(PortalSubscriptionRequestDto request, User customer) {
        Plan plan = loadAvailablePlan(request.getPlanId());
        List<SubscriptionLineRequestDto> lines = priceLines(plan, request.getLines());
        LocalDate startDate = validateStartDate(plan, request.getStartDate());

        CreateSubscriptionRequestDto create = CreateSubscriptionRequestDto.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .startDate(startDate)
                // The subscription runs until the plan itself ends
                .expiryDate(plan.getEndDate())
                .paymentTerms(resolvePaymentTerms(request.getPaymentTerms()))
                .lines(lines)
                .build();

        SubscriptionResponseDto created = subscriptionService.createSubscription(create);
        subscriptionService.changeStatus(created.getId(), SubscriptionStatus.QUOTATION);
        SubscriptionResponseDto confirmed = subscriptionService.changeStatus(created.getId(), SubscriptionStatus.CONFIRMED);
        log.info("Customer {} subscribed to plan '{}' as {}", customer.getEmail(), plan.getName(), created.getSubscriptionNumber());

        return autoActivateOnConfirm
                ? subscriptionService.changeStatus(created.getId(), SubscriptionStatus.ACTIVE)
                : confirmed;
    }

    /** Customer activates their own CONFIRMED subscription (only when auto-activation is off). */
    @Transactional
    public SubscriptionResponseDto activate(Long subscriptionId, User customer) {
        if (autoActivateOnConfirm) {
            throw new BusinessRuleException("Subscriptions are activated automatically when confirmed.");
        }
        assertOwned(subscriptionId, customer);
        return subscriptionService.changeStatus(subscriptionId, SubscriptionStatus.ACTIVE);
    }

    // =========================================================================
    // MY SUBSCRIPTIONS & INVOICES
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<SubscriptionResponseDto> getMySubscriptions(Pageable pageable, User customer) {
        // SubscriptionService scopes CUSTOMER users to their own records
        return subscriptionService.getSubscriptions(null, null, pageable, customer);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDto getMySubscription(Long subscriptionId, User customer) {
        assertOwned(subscriptionId, customer);
        return subscriptionService.getSubscriptionById(subscriptionId, customer);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponseDto> getMyInvoices(Long subscriptionId, Pageable pageable, User customer) {
        Page<Invoice> invoices = subscriptionId == null
                ? invoiceRepository.findBySubscriptionCustomerId(customer.getId(), pageable)
                : invoiceRepository.findBySubscriptionCustomerIdAndSubscriptionId(customer.getId(), subscriptionId, pageable);
        return invoices.map(invoiceService::mapToDto);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto getMyInvoice(Long invoiceId, User customer) {
        return invoiceRepository.findByIdAndSubscriptionCustomerId(invoiceId, customer.getId())
                .map(invoiceService::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + invoiceId));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Loads a plan and rejects it with a clear message unless customers can subscribe to it today. */
    Plan loadAvailablePlan(Long planId) {
        Plan plan = planRepository.findByIdAndActiveTrue(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));
        LocalDate today = LocalDate.now();

        if (plan.getStatus() == PlanStatus.PAUSED) {
            throw new BusinessRuleException("Plan '" + plan.getName() + "' is paused and is not accepting new subscriptions.");
        }
        if (plan.getStatus() == PlanStatus.CLOSED) {
            throw new BusinessRuleException("Plan '" + plan.getName() + "' is closed and is not accepting new subscriptions.");
        }
        if (today.isBefore(plan.getStartDate())) {
            throw new BusinessRuleException("Plan '" + plan.getName() + "' has not started yet. It opens on " + plan.getStartDate() + ".");
        }
        if (today.isAfter(plan.getEndDate())) {
            throw new BusinessRuleException("Plan '" + plan.getName() + "' expired on " + plan.getEndDate() + ".");
        }
        return plan;
    }

    /**
     * Validates the requested lines against the plan and returns them priced on the server.
     * Any price the client might send is never read: the portal request has no price field.
     */
    List<SubscriptionLineRequestDto> priceLines(Plan plan, List<PortalLineRequestDto> requestLines) {
        if (requestLines == null || requestLines.isEmpty()) {
            throw new BusinessRuleException("Add at least one product to subscribe.");
        }

        Map<Long, Product> planProducts = planProducts(plan);
        List<SubscriptionLineRequestDto> priced = new ArrayList<>();

        for (PortalLineRequestDto line : requestLines) {
            Product product = planProducts.get(line.getProductId());
            if (product == null) {
                throw new BusinessRuleException("Product " + line.getProductId() + " is not part of plan '" + plan.getName() + "'.");
            }
            if (line.getQuantity() == null || line.getQuantity() < plan.getMinQuantity()) {
                throw new BusinessRuleException(String.format(
                        "Quantity for %s must be at least %d (the plan's minimum quantity).",
                        product.getName(), plan.getMinQuantity()));
            }

            BigDecimal unitPrice = product.getSalesPrice();
            if (line.getVariantId() != null) {
                ProductVariant variant = productVariantRepository.findByIdAndActiveTrue(line.getVariantId())
                        .filter(v -> v.getProduct().getId().equals(product.getId()))
                        .orElseThrow(() -> new BusinessRuleException(
                                "Variant " + line.getVariantId() + " is not available for " + product.getName() + "."));
                unitPrice = unitPrice.add(variant.getExtraPrice());
            }

            priced.add(SubscriptionLineRequestDto.builder()
                    .productId(product.getId())
                    .variantId(line.getVariantId())
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .build());
        }
        return priced;
    }

    /** The plan's active products: the many-to-many list plus the older single product link. */
    private Map<Long, Product> planProducts(Plan plan) {
        Map<Long, Product> products = new LinkedHashMap<>();
        if (plan.getProducts() != null) {
            plan.getProducts().stream().filter(Product::isActive).forEach(p -> products.put(p.getId(), p));
        }
        if (plan.getProduct() != null && plan.getProduct().isActive()) {
            products.putIfAbsent(plan.getProduct().getId(), plan.getProduct());
        }
        return products;
    }

    private LocalDate validateStartDate(Plan plan, LocalDate startDate) {
        if (startDate == null) {
            throw new BusinessRuleException("Start date is required.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Start date cannot be in the past.");
        }
        if (!startDate.isBefore(plan.getEndDate())) {
            throw new BusinessRuleException("Start date must be before the plan ends on " + plan.getEndDate() + ".");
        }
        return startDate;
    }

    private String resolvePaymentTerms(String requested) {
        if (requested == null || requested.isBlank()) {
            return paymentTerms.get(0);
        }
        return paymentTerms.stream()
                .filter(terms -> terms.equalsIgnoreCase(requested.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Unknown payment terms: " + requested + "."));
    }

    /** 404 (not 403) for someone else's subscription, so ids of other customers are not confirmed to exist. */
    private void assertOwned(Long subscriptionId, User customer) {
        if (!subscriptionRepository.existsByIdAndCustomerIdAndActiveTrue(subscriptionId, customer.getId())) {
            throw new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId);
        }
    }

    private PortalPlanResponseDto mapPlan(Plan plan) {
        List<PortalPlanResponseDto.Product> products = planProducts(plan).values().stream()
                .map(product -> PortalPlanResponseDto.Product.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .type(product.getType())
                        .notes(product.getNotes())
                        .salesPrice(product.getSalesPrice())
                        .variants(productVariantRepository.findByProductIdAndActiveTrue(product.getId()).stream()
                                .map(variant -> PortalPlanResponseDto.Variant.builder()
                                        .id(variant.getId())
                                        .attribute(variant.getAttribute())
                                        .value(variant.getValue())
                                        .extraPrice(variant.getExtraPrice())
                                        .finalPrice(product.getSalesPrice().add(variant.getExtraPrice()))
                                        .build())
                                .toList())
                        .build())
                .toList();

        return PortalPlanResponseDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .billingPeriod(plan.getBillingPeriod())
                .minQuantity(plan.getMinQuantity())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .products(products)
                .build();
    }
}
