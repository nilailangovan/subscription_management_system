package com.brightloop.subscriptionmanagement.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Line item input for pricing calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingLineRequest {

    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long taxId;
}

