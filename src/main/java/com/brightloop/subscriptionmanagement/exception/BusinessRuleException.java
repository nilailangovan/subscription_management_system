package com.brightloop.subscriptionmanagement.exception;

/**
 * Thrown when a business rule is violated.
 * Results in a 400 Bad Request HTTP response.
 *
 * Example: Trying to activate a subscription that is already CLOSED.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

