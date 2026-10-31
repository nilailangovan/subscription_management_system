package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.PlanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to update an existing Subscription Plan.
 */
@Getter
@Setter
public class UpdatePlanRequestDto {

    private String name;

    private Long productId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    private BillingPeriod billingPeriod;

    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean autoClose;

    private Boolean closable;

    private Boolean pausable;

    private Boolean renewable;

    private PlanStatus status;

    private Boolean active;
}
