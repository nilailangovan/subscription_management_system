package com.brightloop.subscriptionmanagement.entity;

/**
 * Represents the possible statuses of a Subscription.
 *
 * Lifecycle:
 *   DRAFT → QUOTATION → CONFIRMED → ACTIVE → CLOSED
 *
 * - DRAFT     : Subscription just created, not yet submitted.
 * - QUOTATION : Sent to the customer for review/approval.
 * - CONFIRMED : Customer has approved/confirmed the subscription.
 * - ACTIVE    : Subscription is live and billing is running.
 * - CLOSED    : Subscription has ended or been cancelled.
 */
public enum SubscriptionStatus {
    DRAFT,
    QUOTATION,
    CONFIRMED,
    ACTIVE,
    CLOSED
}

