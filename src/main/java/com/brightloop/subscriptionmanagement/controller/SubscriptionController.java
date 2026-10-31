package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.ChangeSubscriptionStatusRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.CreateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateSubscriptionRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import com.brightloop.subscriptionmanagement.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controller exposing REST endpoints for Subscriptions.
 *
 * RBAC Rules:
 *  - ADMIN & STAFF: Create, Update (only in DRAFT), change lifecycle status, list/search all subscriptions.
 *  - CUSTOMER: View only their own subscriptions.
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Endpoints for managing customer subscriptions, line items, and lifecycle transitions")
@SecurityRequirement(name = "Bearer Authentication")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    // =========================================================================
    // WRITE ENDPOINTS (ADMIN & STAFF ONLY)
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new subscription in DRAFT status (ADMIN & STAFF only)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Subscription created successfully in DRAFT status", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update an existing subscription (Only allowed in DRAFT status, ADMIN & STAFF only)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.updateSubscription(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Subscription updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Advance subscription lifecycle status strictly (DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE -> CLOSED)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeSubscriptionStatusRequestDto request) {
        SubscriptionResponseDto response = subscriptionService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponseDto.success("Subscription status transitioned successfully", response));
    }

    // =========================================================================
    // READ ENDPOINTS (ADMIN, STAFF, and CUSTOMER with ownership scoping)
    // =========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Get subscription by ID (CUSTOMER can only view their own)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> getSubscriptionById(
            @PathVariable Long id,
            Principal principal) {
        User currentUser = getAuthenticatedUser(principal);
        SubscriptionResponseDto response = subscriptionService.getSubscriptionById(id, currentUser);
        return ResponseEntity.ok(ApiResponseDto.success("Subscription retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "List subscriptions with pagination and status filter (CUSTOMER view restricted to own)")
    public ResponseEntity<ApiResponseDto<Page<SubscriptionResponseDto>>> getSubscriptions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            Principal principal) {

        User currentUser = getAuthenticatedUser(principal);

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<SubscriptionResponseDto> response = subscriptionService.getSubscriptions(search, status, pageable, currentUser);

        return ResponseEntity.ok(ApiResponseDto.success("Subscriptions retrieved successfully", response));
    }

    // =========================================================================
    // QUOTATION TEMPLATE HOOK (TODO for Phase 4)
    // =========================================================================

    /**
     * TODO: Hook for creating a subscription from a Quotation Template.
     */
    @PostMapping("/from-template/{templateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "TODO: Create subscription from quotation template (Hook for Phase 4)")
    public ResponseEntity<ApiResponseDto<SubscriptionResponseDto>> createFromTemplate(
            @PathVariable Long templateId,
            @RequestParam Long customerId) {
        SubscriptionResponseDto response = subscriptionService.createFromQuotationTemplate(templateId, customerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Subscription created from template", response));
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResourceNotFoundException("Unauthenticated user session.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account not found."));
    }
}

