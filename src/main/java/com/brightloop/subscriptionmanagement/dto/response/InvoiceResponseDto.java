package com.brightloop.subscriptionmanagement.dto.response;

import com.brightloop.subscriptionmanagement.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Invoice as returned to clients (never the JPA entity itself).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDto {
    private Long id;
    private String invoiceNumber;
    private Long subscriptionId;
    private String subscriptionNumber;
    private String planName;
    private String customerName;
    private String customerEmail;
    private String billingPeriod;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String paymentTerms;
    private BigDecimal subtotal;
    private BigDecimal totalTax;
    private BigDecimal totalDiscount;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    /** True when the invoice is unpaid (CONFIRMED) and its due date has passed */
    private boolean overdue;
    private LocalDateTime createdAt;
}
