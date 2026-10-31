package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.*;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.AuthResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.UserResponseDto;
import com.brightloop.subscriptionmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication-related endpoints.
 *
 * Base URL: /api/auth
 *
 * Public endpoints (no JWT required):
 *   POST /api/auth/signup          — Register a new customer
 *   POST /api/auth/login           — Login and get a JWT token
 *   POST /api/auth/forgot-password — Request a password reset link
 *   POST /api/auth/reset-password  — Submit new password using the reset token
 *
 * Protected endpoints (JWT required):
 *   POST /api/admin/users/staff    — ADMIN only: create a STAFF user
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────
    // 1. Signup — Register a new customer
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a new CUSTOMER account.
     *
     * @param request signup details (firstName, lastName, email, password)
     * @return 201 Created with the new user's details
     *
     * @Valid triggers Jakarta Validation on the request body.
     * If any field fails validation, Spring returns 400 before reaching this method.
     */
    @PostMapping("/api/auth/signup")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request) {

        UserResponseDto newUser = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Account created successfully. Welcome to LoopPay!", newUser));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Login — Authenticate and receive a JWT token
    // ─────────────────────────────────────────────────────────────

    /**
     * Logs in a user and returns a JWT token.
     *
     * @param request login credentials (email, password)
     * @return 200 OK with JWT token and user info
     */
    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {

        AuthResponseDto authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponseDto.success("Login successful.", authResponse)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Forgot Password — Request a reset link
    // ─────────────────────────────────────────────────────────────

    /**
     * Initiates the password reset flow.
     *
     * Always returns 200 OK with the same message, even if the email is not found.
     * This is intentional — we don't want to reveal which emails are registered.
     *
     * @param request the email address to send the reset link to
     * @return 200 OK with a generic success message
     */
    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<ApiResponseDto<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {

        authService.forgotPassword(request);

        // Always return the same message regardless of whether the email exists
        return ResponseEntity.ok(
                ApiResponseDto.success(
                    "If an account with that email exists, a password reset link has been sent."
                )
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Reset Password — Submit new password with reset token
    // ─────────────────────────────────────────────────────────────

    /**
     * Resets the user's password using the token from the reset email.
     *
     * @param request the reset token and new password
     * @return 200 OK on success
     */
    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<ApiResponseDto<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponseDto.success("Password reset successfully. You can now log in with your new password.")
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Admin: Create Staff User
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new STAFF user account.
     *
     * @PreAuthorize("hasRole('ADMIN')") ensures only ADMIN users can call this.
     * If a non-admin user tries, Spring Security returns 403 Forbidden.
     *
     * @param request the new staff member's details
     * @return 201 Created with the staff user's details
     */
    @PostMapping("/api/admin/users/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> createStaffUser(
            @Valid @RequestBody CreateStaffRequestDto request) {

        UserResponseDto staffUser = authService.createStaffUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Staff user created successfully.", staffUser));
    }
}

