package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One line a customer picks in the portal. There is deliberately no price field:
 * prices are always calculated on the server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalLineRequestDto {

    @NotNull(message = "Product is required")
    private Long productId;

    // Optional variant of the product
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
