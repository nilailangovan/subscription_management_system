package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the "forgot password" request.
 *
 * The user provides their email, and the system sends a reset link to that address.
 */
@Getter
@Setter
public class ForgotPasswordRequestDto {

    /** The email address of the account to reset */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}

