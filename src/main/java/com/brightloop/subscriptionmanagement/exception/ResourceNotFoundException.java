package com.brightloop.subscriptionmanagement.exception;

/**
 * Thrown when a requested resource is not found in the database.
 * Results in a 404 Not Found HTTP response.
 *
 * Example: Fetching a user by ID that doesn't exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

