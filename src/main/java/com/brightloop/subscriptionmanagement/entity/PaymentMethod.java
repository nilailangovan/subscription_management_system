package com.brightloop.subscriptionmanagement.entity;

/**
 * How a customer paid an invoice. Stored as a string (RULES.md section 10).
 */
public enum PaymentMethod {
    CARD,
    BANK_TRANSFER,
    UPI,
    CASH
}
