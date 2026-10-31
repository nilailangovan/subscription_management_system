package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Payload to renew an existing Plan.
 * If newEndDate is not provided, the service extends the plan by one billingPeriod.
 */
@Getter
@Setter
public class RenewPlanRequestDto {

    /**
     * Optional new end date.
     * If omitted, the plan's endDate is automatically extended based on its billingPeriod.
     */
    @Future(message = "New end date must be in the future")
    private LocalDate newEndDate;
}

