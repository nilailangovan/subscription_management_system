package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload representing a single product line in a subscription create/update request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionLineRequestDto {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Optional custom unit price.
     * If omitted, falls back to the product's base salesPrice.
     */
    private BigDecimal unitPrice;

    /**
     * Optional tax identifier.
     */
    private Long taxId;
}

