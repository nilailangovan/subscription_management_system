package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the "reset password" request.
 *
 * The user submits:
 *  - The reset token from the email link.
 *  - Their new password (must meet the password rules from RULES.md).
 */
@Getter
@Setter
public class ResetPasswordRequestDto {

    /** The unique reset token from the password reset email link */
    @NotBlank(message = "Reset token is required")
    private String token;

    /**
     * The new password.
     * Must follow the same rules as signup:
     *  - More than 8 characters
     *  - At least 1 uppercase, 1 lowercase, 1 special character
     */
    @NotBlank(message = "New password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{9,}$",
        message = "Password must be more than 8 characters and contain at least one uppercase letter, one lowercase letter, and one special character (!@#$%^&*)"
    )
    private String newPassword;
}

