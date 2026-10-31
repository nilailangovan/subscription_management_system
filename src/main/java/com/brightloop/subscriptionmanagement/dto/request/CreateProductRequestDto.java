package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Payload to create a new Product.
 */
@Getter
@Setter
public class CreateProductRequestDto {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Product type is required (SERVICE, GOODS, SOFTWARE)")
    private ProductType type;

    @NotNull(message = "Sales price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Sales price cannot be negative")
    private BigDecimal salesPrice;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    private String notes;
}

