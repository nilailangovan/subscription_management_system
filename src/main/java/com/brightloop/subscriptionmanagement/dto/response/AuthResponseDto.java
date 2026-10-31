package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned after a successful login.
 *
 * Contains the JWT token and basic user info so the frontend
 * doesn't need to make a separate call to get user details.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthResponseDto {

    /** The JWT token — the frontend must include this in all future requests */
    private String token;

    /** The type of token — always "Bearer" */
    private String tokenType;

    /** The logged-in user's ID */
    private Long userId;

    /** The logged-in user's email */
    private String email;

    /** The logged-in user's full name */
    private String fullName;

    /** The logged-in user's role (ADMIN, STAFF, or CUSTOMER) */
    private String role;
}

