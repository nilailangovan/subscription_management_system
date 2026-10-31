package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.brightloop.subscriptionmanagement.entity.Role;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating a new STAFF user (Admin-only endpoint).
 *
 * An ADMIN provides the new staff member's details.
 * The role is hardcoded to STAFF in the service layer, but we accept
 * it here so the endpoint can potentially support creating other roles
 * in future (with validation).
 */
@Getter
@Setter
public class CreateStaffRequestDto {

    /** Staff member's first name */
    @NotBlank(message = "First name is required")
    private String firstName;

    /** Staff member's last name */
    @NotBlank(message = "Last name is required")
    private String lastName;

    /** Staff member's email — must be unique */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /**
     * Staff member's initial password.
     * Must follow password rules from RULES.md.
     */
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{9,}$",
        message = "Password must be more than 8 characters and contain at least one uppercase letter, one lowercase letter, and one special character (!@#$%^&*)"
    )
    private String password;
}

