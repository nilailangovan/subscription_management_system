package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A generic API response wrapper.
 *
 * All API responses (success or error) are wrapped in this object
 * so the frontend always gets a consistent structure.
 *
 * Example success response:
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "data": { ... },
 *   "timestamp": "2024-01-01T10:00:00"
 * }
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ApiResponseDto<T> {

    /** Whether the request was successful */
    private boolean success;

    /** A human-readable message about the result */
    private String message;

    /** The actual data payload (null for error responses) */
    private T data;

    /** When this response was generated */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a quick success response with data.
     *
     * @param message descriptive message
     * @param data    the payload to return
     * @return a success ApiResponseDto
     */
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a quick success response without a data payload.
     *
     * @param message descriptive message
     * @return a success ApiResponseDto with null data
     */
    public static <T> ApiResponseDto<T> success(String message) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a quick error response.
     *
     * @param message the error message
     * @return an error ApiResponseDto
     */
    public static <T> ApiResponseDto<T> error(String message) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

