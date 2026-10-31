package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.CreatePlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.RenewPlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdatePlanRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.PlanResponseDto;
import com.brightloop.subscriptionmanagement.service.PlanService;
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

/**
 * Controller exposing endpoints for Subscription Plans and Lifecycle Actions.
 *
 * RBAC Rules:
 *  - ADMIN: Create, Update, Soft-delete, and execute lifecycle actions (close, pause, resume, renew).
 *  - ADMIN & STAFF: View / List all plans with pagination and search.
 *  - CUSTOMER: View active available plans within current date range.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plans", description = "Endpoints for managing subscription plans and actions")
@SecurityRequirement(name = "Bearer Authentication")
public class PlanController {

    private final PlanService planService;

    // ==========================================
    // CRUD ENDPOINTS
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new plan (ADMIN only)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> createPlan(
            @Valid @RequestBody CreatePlanRequestDto request) {
        PlanResponseDto response = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Plan created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing plan (ADMIN only)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlanRequestDto request) {
        PlanResponseDto response = planService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Plan updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a plan (ADMIN only)")
    public ResponseEntity<ApiResponseDto<Void>> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan deleted successfully", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get plan by ID (ADMIN and STAFF)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> getPlanById(@PathVariable Long id) {
        PlanResponseDto response = planService.getPlanById(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List all plans with pagination and search (ADMIN and STAFF)")
    public ResponseEntity<ApiResponseDto<Page<PlanResponseDto>>> getPlans(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        if (sort != null && sort.length >= 2) {
            Sort.Direction direction = sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortObj = Sort.by(direction, sort[0]);
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<PlanResponseDto> response = planService.getPlans(search, pageable);
        return ResponseEntity.ok(ApiResponseDto.success("Plans retrieved successfully", response));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF')")
    @Operation(summary = "View available plans (CUSTOMER, ADMIN, STAFF) - active status and within date range")
    public ResponseEntity<ApiResponseDto<Page<PlanResponseDto>>> getAvailablePlans(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "price"));
        Page<PlanResponseDto> response = planService.getAvailablePlansForCustomer(search, pageable);
        return ResponseEntity.ok(ApiResponseDto.success("Available plans retrieved successfully", response));
    }

    // ==========================================
    // PLAN ACTIONS (ADMIN ONLY)
    // ==========================================

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close a plan (ADMIN only - requires closable=true)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> closePlan(@PathVariable Long id) {
        PlanResponseDto response = planService.closePlan(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan closed successfully", response));
    }

    @PatchMapping("/{id}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Pause an active plan (ADMIN only - requires pausable=true)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> pausePlan(@PathVariable Long id) {
        PlanResponseDto response = planService.pausePlan(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan paused successfully", response));
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resume a paused plan (ADMIN only)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> resumePlan(@PathVariable Long id) {
        PlanResponseDto response = planService.resumePlan(id);
        return ResponseEntity.ok(ApiResponseDto.success("Plan resumed successfully", response));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Renew a plan (ADMIN only - requires renewable=true)")
    public ResponseEntity<ApiResponseDto<PlanResponseDto>> renewPlan(
            @PathVariable Long id,
            @RequestBody(required = false) RenewPlanRequestDto request) {
        PlanResponseDto response = planService.renewPlan(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Plan renewed successfully", response));
    }
}

