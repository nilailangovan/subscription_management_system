package com.brightloop.subscriptionmanagement.exception;

import com.brightloop.subscriptionmanagement.dto.response.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 *
 * @RestControllerAdvice means this class intercepts exceptions thrown
 * from ANY controller and converts them into clean, structured JSON responses.
 *
 * Without this, Spring would return ugly HTML error pages or raw stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors — when a DTO field fails @NotBlank, @Email, @Pattern, etc.
     * Returns 400 Bad Request with a map of { fieldName: errorMessage }.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect all field errors into a map
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponseDto response = ErrorResponseDto.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed. Please check the errors and try again.")
                .errors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles 404 errors — when a resource (user, subscription, etc.) is not found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles 409 errors — when an email is already registered.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles invalid/expired password reset tokens.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidToken(InvalidTokenException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles business rule violations (e.g., invalid status transitions).
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessRule(BusinessRuleException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles wrong email/password during login.
     * Spring Security throws this when authentication fails.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    /**
     * Handles login attempts by disabled/inactive accounts.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDto> handleDisabledAccount(DisabledException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Your account has been deactivated. Please contact support.");
    }

    /**
     * Handles 403 Forbidden — when a user tries to access a resource they don't have permission for.
     * Spring Security throws this when @PreAuthorize fails.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
    }

    /**
     * Catch-all handler for any unexpected exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        // Log the real error on the server (don't send stack trace to the client)
        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    /**
     * Helper method to build a standard error response.
     *
     * @param status  the HTTP status code
     * @param message the error message
     * @return a ResponseEntity with the ErrorResponseDto body
     */
    private ResponseEntity<ErrorResponseDto> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponseDto response = ErrorResponseDto.builder()
                .statusCode(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}

