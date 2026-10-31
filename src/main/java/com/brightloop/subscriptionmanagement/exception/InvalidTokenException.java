package com.brightloop.subscriptionmanagement.exception;

/**
 * Thrown when a password reset token is invalid (not found, expired, or already used).
 * Results in a 400 Bad Request HTTP response.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}

