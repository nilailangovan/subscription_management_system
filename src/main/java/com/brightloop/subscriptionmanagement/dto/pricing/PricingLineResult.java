package com.brightloop.subscriptionmanagement.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Calculated pricing result for a single line item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingLineResult {

    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long taxId;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
}

