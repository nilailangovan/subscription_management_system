package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the login request.
 *
 * Contains the user's email and password.
 * On success, the server returns a JWT token.
 */
@Getter
@Setter
public class LoginRequestDto {

    /** The user's registered email address */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /** The user's password (plain text — will be matched against the BCrypt hash) */
    @NotBlank(message = "Password is required")
    private String password;
}

