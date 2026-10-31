package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for the customer self-registration (signup) request.
 *
 * Contains all the data a new customer must provide to create an account.
 * Validations are applied here — if any fail, Spring returns a 400 error
 * before the request even reaches the service layer.
 */
@Getter
@Setter
public class SignupRequestDto {

    /** User's first name — cannot be blank */
    @NotBlank(message = "First name is required")
    private String firstName;

    /** User's last name — cannot be blank */
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * User's email address.
     * Must be a valid email format and will be checked for uniqueness in the service.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /**
     * User's desired password.
     *
     * Rules (per RULES.md):
     *  - More than 8 characters
     *  - At least 1 uppercase letter
     *  - At least 1 lowercase letter
     *  - At least 1 special character from: !@#$%^&*
     *
     * The regex breakdown:
     *  (?=.*[a-z])   → at least one lowercase letter
     *  (?=.*[A-Z])   → at least one uppercase letter
     *  (?=.*[!@#$%^&*]) → at least one special character
     *  .{9,}         → total length more than 8 (i.e., at least 9 characters)
     */
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{9,}$",
        message = "Password must be more than 8 characters and contain at least one uppercase letter, one lowercase letter, and one special character (!@#$%^&*)"
    )
    private String password;
}

