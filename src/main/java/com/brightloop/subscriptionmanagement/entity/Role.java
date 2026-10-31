package com.brightloop.subscriptionmanagement.entity;

/**
 * Represents the roles a user can have in the system.
 *
 * - ADMIN  : Full control — manages users, plans, subscriptions, invoices.
 * - STAFF  : Operational access — manages subscriptions and invoices.
 * - CUSTOMER: Limited access — views their own subscriptions and invoices only.
 */
public enum Role {
    ADMIN,
    STAFF,
    CUSTOMER
}

