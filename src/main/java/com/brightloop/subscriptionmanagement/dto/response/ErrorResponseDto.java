package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO returned when a validation error or API error occurs.
 *
 * Example error response:
 * {
 *   "statusCode": 400,
 *   "message": "Validation failed",
 *   "errors": { "email": "must be a valid email" },
 *   "timestamp": "2024-01-01T10:00:00"
 * }
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ErrorResponseDto {

    /** HTTP status code (e.g., 400, 401, 403, 404, 500) */
    private int statusCode;

    /** A short description of the error */
    private String message;

    /**
     * Field-level validation errors (only present for 400 errors).
     * Key = field name, Value = what's wrong with it.
     * e.g., { "email": "must not be blank", "password": "must match pattern" }
     */
    private Map<String, String> errors;

    /** When the error occurred */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

