package com.brightloop.subscriptionmanagement.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated pricing result containing per-line calculations and order-level totals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResult {

    private List<PricingLineResult> lineResults;
    private BigDecimal subtotal;
    private BigDecimal totalTax;
    private BigDecimal totalDiscount;
    private BigDecimal grandTotal;
}

