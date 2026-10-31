package com.brightloop.subscriptionmanagement.entity;

/**
 * Defines the recurring billing frequency for a Plan.
 *
 * - DAILY   : Billed every day.
 * - WEEKLY  : Billed every week.
 * - MONTHLY : Billed every month (most common).
 * - YEARLY  : Billed once a year (annual subscription).
 */
public enum BillingPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

