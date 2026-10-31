package com.brightloop.subscriptionmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sanitized response for a ProductVariant.
 * Includes calculated finalPrice = product.salesPrice + variant.extraPrice.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponseDto {

    private Long id;
    private Long productId;
    private String productName;
    private String attribute;
    private String value;
    private BigDecimal baseSalesPrice;
    private BigDecimal extraPrice;
    private BigDecimal finalPrice;
    private boolean active;
    private LocalDateTime createdAt;
}

