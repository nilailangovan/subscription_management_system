package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.RecordPaymentRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.PaymentResponseDto;
import com.brightloop.subscriptionmanagement.entity.Invoice;
import com.brightloop.subscriptionmanagement.entity.InvoiceStatus;
import com.brightloop.subscriptionmanagement.entity.Payment;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.InvoiceRepository;
import com.brightloop.subscriptionmanagement.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Records payments against invoices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Records a full payment for a sent invoice and marks the invoice PAID.
     *
     * shortcut: one payment always settles the whole invoice; add an amount field if partial payments are needed.
     *
     * @throws ResourceNotFoundException if the invoice does not exist
     * @throws BusinessRuleException     if the invoice is a draft, already paid, or the date is before the issue date
     */
    @Transactional
    public PaymentResponseDto recordPayment(RecordPaymentRequestDto request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID || paymentRepository.existsByInvoiceId(invoice.getId())) {
            throw new BusinessRuleException("Invoice " + invoice.getInvoiceNumber() + " is already paid.");
        }
        if (invoice.getStatus() != InvoiceStatus.CONFIRMED) {
            throw new BusinessRuleException("Invoice " + invoice.getInvoiceNumber()
                    + " is still a draft. Only sent (CONFIRMED) invoices can be paid.");
        }

        LocalDate paidDate = request.getPaidDate() != null ? request.getPaidDate() : LocalDate.now();
        if (paidDate.isBefore(invoice.getIssueDate())) {
            throw new BusinessRuleException("The paid date cannot be before the invoice was issued (" + invoice.getIssueDate() + ").");
        }

        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .invoice(invoice)
                .amount(invoice.getTotalAmount())
                .method(request.getMethod())
                .paidDate(paidDate)
                .reference(request.getReference() == null || request.getReference().isBlank() ? null : request.getReference().trim())
                .build();
        invoice.setStatus(InvoiceStatus.PAID);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} of {} recorded for invoice {}", saved.getPaymentNumber(), saved.getAmount(), invoice.getInvoiceNumber());
        return mapToDto(saved);
    }

    public PaymentResponseDto mapToDto(Payment payment) {
        Invoice invoice = payment.getInvoice();
        User customer = invoice.getSubscription().getCustomer();
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subscriptionNumber(invoice.getSubscription().getSubscriptionNumber())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getEmail())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .paidDate(payment.getPaidDate())
                .reference(payment.getReference())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    /** Sequential, zero-padded number (PAY-0001), same scheme as invoice numbers. */
    private synchronized String generatePaymentNumber() {
        long nextIndex = paymentRepository.findTopByOrderByIdDesc().map(p -> p.getId() + 1).orElse(1L);
        String candidate = String.format("PAY-%04d", nextIndex);
        while (paymentRepository.existsByPaymentNumber(candidate)) {
            nextIndex++;
            candidate = String.format("PAY-%04d", nextIndex);
        }
        return candidate;
    }
}
