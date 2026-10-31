package com.brightloop.subscriptionmanagement.entity;

/**
 * Represents the possible statuses of an Invoice.
 *
 * Lifecycle:
 *   DRAFT → CONFIRMED → PAID
 *
 * - DRAFT     : Invoice created but not yet finalized.
 * - CONFIRMED : Invoice finalized and sent to the customer.
 * - PAID      : Payment has been received.
 */
public enum InvoiceStatus {
    DRAFT,
    CONFIRMED,
    PAID
}

