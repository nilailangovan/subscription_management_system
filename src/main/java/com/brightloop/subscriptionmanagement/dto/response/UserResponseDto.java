package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning user details.
 *
 * Used when creating a user or fetching a user profile.
 * Never includes the password — that stays on the server.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
}

