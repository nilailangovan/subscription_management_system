package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Payload to update a ProductVariant.
 */
@Getter
@Setter
public class UpdateProductVariantRequestDto {

    private String attribute;

    private String value;

    @DecimalMin(value = "0.0", inclusive = true, message = "Extra price cannot be negative")
    private BigDecimal extraPrice;

    private Boolean active;
}

