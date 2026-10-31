package com.brightloop.subscriptionmanagement.exception;

/**
 * Thrown when a user tries to register with an email that is already in use.
 * Results in a 409 Conflict HTTP response.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

