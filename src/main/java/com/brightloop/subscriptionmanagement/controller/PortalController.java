package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;
import com.brightloop.subscriptionmanagement.dto.request.PortalSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.InvoiceResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.PortalPlanResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import com.brightloop.subscriptionmanagement.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Customer self-service portal (CUSTOMER role only).
 * The customer is always resolved from the JWT principal, never from the request body.
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Portal", description = "Self-service plans, subscriptions and invoices for customers")
@SecurityRequirement(name = "Bearer Authentication")
public class PortalController {

    private final PortalService portalService;
    private final UserRepository userRepository;

    @GetMapping("/plans")
    @Operation(summary = "Plans available to subscribe to (ACTIVE and within their dates), with products, variants and final prices")
    public ResponseEntity<ApiResponseDto<Page<PortalPlanResponseDto>>> getPlans(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PortalPlanResponseDto> plans = portalService.getAvailablePlans(
                search, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "price")));
        return ResponseEntity.ok(ApiResponseDto.success("Available plans retrieved successfully", plans));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "One subscribable plan (400 if paused, closed, expired or not started)")
    public ResponseEntity<ApiResponseDto<PortalPlanResponseDto>> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success("Plan retrieved successfully", portalService.getAvailablePlan(id)));
    }

    @GetMapping("/payment-terms")
    @Operation(summary = "Payment terms a customer can choose (configured in app.subscription.payment-terms)")
    public ResponseEntity<ApiResponseDto<List<String>>> getPaymentTerms() {
        return ResponseEntity.ok(ApiResponseDto.success("Payment terms retrieved successfully", portalService.getPaymentTerms()));
    }

    @PostMapping("/subscriptions/preview")
    @Operation(summary = "Price a subscription (unit price, tax, discount, totals) without saving it")
    public ResponseEntity<ApiResponseDto<PricingResult>> preview(@Valid @RequestBody PortalSubscriptionRequestDto request) {
        return ResponseEntity.ok(ApiResponseDto.success("Price preview calculated", portalService.preview(request)));
    }

    @PostMapping("/subscriptions")
    @Operation(summary = "Subscribe the logged-in customer (price recalculated on the server, DRAFT -> QUOTATION -> CONFIRMED)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> subscribe(
            @Valid @RequestBody PortalSubscriptionRequestDto request, Principal principal) {
        SubscriptionResponseDto subscription = portalService.subscribe(request, currentCustomer(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Subscription " + subscription.getSubscriptionNumber() + " created", subscription));
    }

    @PostMapping("/subscriptions/{id}/activate")
    @Operation(summary = "Activate your own CONFIRMED subscription (only when app.subscription.auto-activate-on-confirm=false)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> activate(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponseDto.success("Subscription activated",
                portalService.activate(id, currentCustomer(principal))));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Your subscriptions, newest first")
    public ResponseEntity<ApiResponseDto<Page<SubscriptionResponseDto>>> getMySubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Page<SubscriptionResponseDto> subscriptions = portalService.getMySubscriptions(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")), currentCustomer(principal));
        return ResponseEntity.ok(ApiResponseDto.success("Subscriptions retrieved successfully", subscriptions));
    }

    @GetMapping("/subscriptions/{id}")
    @Operation(summary = "One of your subscriptions (404 if it belongs to someone else)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> getMySubscription(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponseDto.success("Subscription retrieved successfully",
                portalService.getMySubscription(id, currentCustomer(principal))));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Your invoices, newest first (optionally for one subscription)")
    public ResponseEntity<ApiResponseDto<Page<InvoiceResponseDto>>> getMyInvoices(
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Page<InvoiceResponseDto> invoices = portalService.getMyInvoices(
                subscriptionId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")), currentCustomer(principal));
        return ResponseEntity.ok(ApiResponseDto.success("Invoices retrieved successfully", invoices));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "One of your invoices (404 if it belongs to someone else)")
    public ResponseEntity<ApiResponseDto<InvoiceResponseDto>> getMyInvoice(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponseDto.success("Invoice retrieved successfully",
                portalService.getMyInvoice(id, currentCustomer(principal))));
    }

    /** The logged-in customer, looked up from the JWT subject (email). */
    private User currentCustomer(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
