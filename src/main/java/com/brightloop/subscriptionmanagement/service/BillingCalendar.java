package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure date helpers for billing: period arithmetic, next billing date and payment-term due dates.
 */
public final class BillingCalendar {

    private static final Pattern DAYS = Pattern.compile("(\\d+)");

    private BillingCalendar() {
    }

    /** Adds one billing period to a date (e.g. MONTHLY: 2026-01-31 -> 2026-02-28). */
    public static LocalDate addPeriod(LocalDate date, BillingPeriod period) {
        return switch (period) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    /**
     * Next date the customer will be billed, or null when there is none.
     *  - Not started yet (DRAFT/QUOTATION/CONFIRMED): the start date, i.e. the first bill.
     *  - ACTIVE: the first period boundary after today.
     *  - CLOSED, or past the expiry date: null.
     */
    public static LocalDate nextBillingDate(LocalDate startDate, LocalDate expiryDate, BillingPeriod period,
                                            SubscriptionStatus status, LocalDate today) {
        if (startDate == null || period == null || status == SubscriptionStatus.CLOSED) {
            return null;
        }
        LocalDate next = startDate;
        if (status == SubscriptionStatus.ACTIVE) {
            while (!next.isAfter(today)) {
                next = addPeriod(next, period);
            }
        }
        return expiryDate != null && next.isAfter(expiryDate) ? null : next;
    }

    /**
     * Due date from free-text payment terms: the first number is the day count
     * ("Net 30" -> +30 days); terms without a number ("Immediate") are due on the issue date.
     */
    public static LocalDate dueDate(LocalDate issueDate, String paymentTerms) {
        if (paymentTerms == null) {
            return issueDate;
        }
        Matcher matcher = DAYS.matcher(paymentTerms);
        return matcher.find() ? issueDate.plusDays(Long.parseLong(matcher.group(1))) : issueDate;
    }
}
