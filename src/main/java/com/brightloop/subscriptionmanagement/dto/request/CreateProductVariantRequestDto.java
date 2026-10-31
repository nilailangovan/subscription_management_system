package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Payload to create a variant for a Product.
 * Example: attribute="Brand", value="Odoo", extraPrice=560.00
 */
@Getter
@Setter
public class CreateProductVariantRequestDto {

    @NotBlank(message = "Variant attribute is required (e.g. Brand, Tier, Storage)")
    private String attribute;

    @NotBlank(message = "Variant value is required (e.g. Odoo, Enterprise, 100GB)")
    private String value;

    @NotNull(message = "Extra price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Extra price cannot be negative")
    private BigDecimal extraPrice;
}

