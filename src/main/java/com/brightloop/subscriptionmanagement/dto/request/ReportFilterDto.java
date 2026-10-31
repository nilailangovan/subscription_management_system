package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filters shared by the report lists and their exports (invoices, payments, subscriptions, dashboard).
 * Every field is optional; an empty filter matches everything.
 *
 * What the date range applies to:
 *   invoices      → issue date
 *   payments      → paid date
 *   subscriptions → last updated date
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterDto {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    /** Matches document numbers, customer name or customer email (case-insensitive) */
    private String search;

    private Long customerId;

    private SubscriptionStatus subscriptionStatus;

    private InvoiceState invoiceStatus;

    /**
     * Invoice filter values. DRAFT, CONFIRMED and PAID are the stored InvoiceStatus values;
     * OUTSTANDING and OVERDUE split CONFIRMED (sent, unpaid) invoices by due date as of today.
     */
    public enum InvoiceState {
        DRAFT,
        CONFIRMED,
        PAID,
        OUTSTANDING,
        OVERDUE
    }

    @JsonIgnore
    @AssertTrue(message = "The From date must be on or before the To date")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
