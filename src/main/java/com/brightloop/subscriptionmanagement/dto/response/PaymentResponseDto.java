package com.brightloop.subscriptionmanagement.dto.response;

import com.brightloop.subscriptionmanagement.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A payment as returned by the API, with the invoice and customer it belongs to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private String paymentNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private String subscriptionNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDate paidDate;
    private String reference;
    private LocalDateTime createdAt;
}
