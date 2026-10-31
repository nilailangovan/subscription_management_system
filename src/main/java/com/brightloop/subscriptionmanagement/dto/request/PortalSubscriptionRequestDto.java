package com.brightloop.subscriptionmanagement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Customer portal request for both price preview and subscribing.
 * startDate and paymentTerms are only used when subscribing.
 * The customer is always taken from the JWT, never from this body.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalSubscriptionRequestDto {

    @NotNull(message = "Plan is required")
    private Long planId;

    @NotEmpty(message = "Add at least one product")
    @Valid
    private List<PortalLineRequestDto> lines;

    private LocalDate startDate;

    private String paymentTerms;
}
