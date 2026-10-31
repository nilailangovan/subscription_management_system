package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.config.JwtUtil;
import com.brightloop.subscriptionmanagement.dto.request.*;
import com.brightloop.subscriptionmanagement.dto.response.AuthResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.UserResponseDto;
import com.brightloop.subscriptionmanagement.entity.PasswordResetToken;
import com.brightloop.subscriptionmanagement.entity.Role;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.exception.EmailAlreadyExistsException;
import com.brightloop.subscriptionmanagement.exception.InvalidTokenException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.PasswordResetTokenRepository;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service class that handles all authentication-related business logic:
 *  - Customer signup
 *  - Login with JWT generation
 *  - Forgot password (generates and sends/logs a reset token)
 *  - Reset password (validates token and updates password)
 *  - Admin: Create STAFF users
 *
 * The controller calls this service. This service calls the repository.
 * Business rules and validations live here, not in the controller.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.jwt.reset-token-expiry-ms}")
    private long resetTokenExpiryMs;

    // ─────────────────────────────────────────────────────────────
    // 1. Customer Signup
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a new customer account.
     *
     * Steps:
     *  1. Check if the email is already taken.
     *  2. Hash the password.
     *  3. Save the new user with CUSTOMER role.
     *  4. Return user details.
     *
     * @param request signup details (name, email, password)
     * @return the newly created user's details (no password)
     */
    @Transactional
    public UserResponseDto signup(SignupRequestDto request) {
        // Step 1: Ensure email is unique
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                "An account with email '" + request.getEmail() + "' already exists."
            );
        }

        // Step 2: Build and save the user
        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                // Hash the password with BCrypt before saving
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER) // Signup always creates CUSTOMER accounts
                .active(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New customer registered: {}", savedUser.getEmail());

        // Step 3: Return the user info (no password in the response)
        return mapToUserResponse(savedUser);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Login
    // ─────────────────────────────────────────────────────────────

    /**
     * Authenticates a user and returns a JWT token.
     *
     * Steps:
     *  1. Find user by email.
     *  2. Check if account is active.
     *  3. Verify the password against the stored BCrypt hash.
     *  4. Generate and return a JWT token.
     *
     * @param request login credentials (email + password)
     * @return JWT token and basic user info
     */
    public AuthResponseDto login(LoginRequestDto request) {
        // Step 1: Find the user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        // Step 2: Check if the account is active
        if (!user.isActive()) {
            throw new DisabledException("Your account has been deactivated.");
        }

        // Step 3: Verify the password
        // passwordEncoder.matches() compares plain text with the stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        // Step 4: Generate a JWT token for the authenticated user
        UserDetails userDetails = buildUserDetails(user);
        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().name());

        log.info("User logged in: {}", user.getEmail());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Forgot Password
    // ─────────────────────────────────────────────────────────────

    /**
     * Initiates the password reset flow.
     *
     * Steps:
     *  1. Find the user by email (return generic message even if not found — security best practice).
     *  2. Delete any old reset tokens for this user.
     *  3. Generate a new unique token with expiry.
     *  4. Send (or log) the reset link.
     *
     * Note: We always return a success message even if the email isn't found.
     * This prevents "email enumeration" attacks where attackers probe which emails are registered.
     *
     * @param request the email to send the reset link to
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        // Try to find the user — silently do nothing if not found (security best practice)
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Step 1: Delete old tokens for this user to avoid stale token accumulation
            passwordResetTokenRepository.deleteAllByUserId(user.getId());

            // Step 2: Generate a unique, random reset token (UUID is cryptographically random)
            String tokenValue = UUID.randomUUID().toString();

            // Step 3: Set expiry time
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusNanos(resetTokenExpiryMs * 1_000_000);

            // Step 4: Save the token to the database
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiresAt(expiresAt)
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Step 5: Send or log the reset link
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), tokenValue);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Reset Password
    // ─────────────────────────────────────────────────────────────

    /**
     * Completes the password reset process.
     *
     * Steps:
     *  1. Find the reset token in the database.
     *  2. Validate: must not be expired and not already used.
     *  3. Update the user's password.
     *  4. Mark the token as used.
     *
     * @param request the reset token and new password
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        // Step 1: Find the token record
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));

        // Step 2: Check if the token has already been used
        if (resetToken.isUsed()) {
            throw new InvalidTokenException("This reset link has already been used. Please request a new one.");
        }

        // Step 3: Check if the token has expired
        if (LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            throw new InvalidTokenException("This reset link has expired. Please request a new one.");
        }

        // Step 4: Update the user's password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Step 5: Mark the token as used so it cannot be reused
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Admin: Create Staff User
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new STAFF user. Only accessible by ADMIN.
     * (The @PreAuthorize check is on the controller method.)
     *
     * @param request the new staff member's details
     * @return the created staff user's details
     */
    @Transactional
    public UserResponseDto createStaffUser(CreateStaffRequestDto request) {
        // Check if email is already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                "An account with email '" + request.getEmail() + "' already exists."
            );
        }

        User staffUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF)  // Always STAFF — admin cannot set arbitrary roles
                .active(true)
                .build();

        User savedUser = userRepository.save(staffUser);
        log.info("STAFF user created by admin: {}", savedUser.getEmail());

        return mapToUserResponse(savedUser);
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helper Methods
    // ─────────────────────────────────────────────────────────────

    /**
     * Converts a User entity to a UserResponseDto (strips out the password).
     */
    private UserResponseDto mapToUserResponse(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Builds a Spring Security UserDetails object from a User entity.
     * Used when generating JWT tokens.
     */
    private UserDetails buildUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}

