package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload to advance or change the lifecycle status of a subscription.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSubscriptionStatusRequestDto {

    @NotNull(message = "Target status is required")
    private SubscriptionStatus status;
}

