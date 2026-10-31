package com.brightloop.subscriptionmanagement.entity;

/**
 * Represents the lifecycle state of a Plan.
 *
 * - ACTIVE : Plan is available for subscription and ongoing billing.
 * - PAUSED : Plan is temporarily paused (no new signups, billing suspended).
 * - CLOSED : Plan is permanently closed or retired.
 */
public enum PlanStatus {
    ACTIVE,
    PAUSED,
    CLOSED
}

