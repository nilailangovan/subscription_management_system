package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sanitized response representation of a subscription line.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionLineResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal amount;
}

