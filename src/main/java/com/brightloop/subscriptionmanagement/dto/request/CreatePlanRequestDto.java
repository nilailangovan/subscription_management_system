package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload to create a new Subscription Plan.
 *
 * Validations:
 *  - prices cannot be negative
 *  - minQuantity at least 1
 *  - endDate must be after startDate (checked in service)
 */
@Getter
@Setter
public class CreatePlanRequestDto {

    @NotBlank(message = "Plan name is required")
    private String name;

    /** Optional product link */
    private Long productId;

    // Products included in the plan (many-to-many). Optional; replaces the single productId.
    private List<Long> productIds;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Billing period is required (DAILY, WEEKLY, MONTHLY, YEARLY)")
    private BillingPeriod billingPeriod;

    @NotNull(message = "Minimum quantity is required")
    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity = 1;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private boolean autoClose = false;

    private boolean closable = true;

    private boolean pausable = true;

    private boolean renewable = true;
}

