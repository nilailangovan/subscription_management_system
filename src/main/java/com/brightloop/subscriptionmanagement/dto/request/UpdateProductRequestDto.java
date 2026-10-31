package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.ProductType;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Payload to update an existing Product.
 */
@Getter
@Setter
public class UpdateProductRequestDto {

    private String name;

    private ProductType type;

    @DecimalMin(value = "0.0", inclusive = true, message = "Sales price cannot be negative")
    private BigDecimal salesPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    private String notes;

    private Boolean active;
}

