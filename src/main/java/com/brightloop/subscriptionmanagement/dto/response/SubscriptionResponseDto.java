package com.brightloop.subscriptionmanagement.dto.response;

import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sanitized response representation of a Subscription.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {

    private Long id;
    private String subscriptionNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long planId;
    private String planName;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String paymentTerms;
    private SubscriptionStatus status;
    private BigDecimal subtotal;
    private BigDecimal totalTax;
    private BigDecimal totalDiscount;
    private BigDecimal grandTotal;
    private List<SubscriptionLineResponseDto> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

