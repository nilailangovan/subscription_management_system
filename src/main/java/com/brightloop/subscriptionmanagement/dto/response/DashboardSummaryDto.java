package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Finance KPIs for the admin dashboard.
 *
 * Collected revenue follows the date range; receivables and subscription counts are as of today.
 * The search and customer filters apply to every figure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private BigDecimal collectedAmount;
    private long paymentCount;
    /** Sent, unpaid invoices (overdue ones included) */
    private BigDecimal outstandingAmount;
    private long outstandingCount;
    private BigDecimal overdueAmount;
    private long overdueCount;
    private long activeSubscriptions;
    private long totalSubscriptions;
}
