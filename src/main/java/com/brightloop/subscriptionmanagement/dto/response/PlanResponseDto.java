package com.brightloop.subscriptionmanagement.dto.response;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sanitized response for a Subscription Plan.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponseDto {

    private Long id;
    private String name;
    private Long productId;
    private String productName;
    private List<PlanProductDto> products;
    private BigDecimal price;
    private BillingPeriod billingPeriod;
    private int minQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean autoClose;
    private boolean closable;
    private boolean pausable;
    private boolean renewable;
    private PlanStatus status;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

