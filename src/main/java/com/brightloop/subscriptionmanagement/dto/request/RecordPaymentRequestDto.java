package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Records a full payment for a sent (CONFIRMED) invoice. The amount is always the invoice total.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordPaymentRequestDto {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    /** Defaults to today when left empty */
    @PastOrPresent(message = "Paid date cannot be in the future")
    private LocalDate paidDate;

    @Size(max = 100, message = "Reference must be at most 100 characters")
    private String reference;
}
