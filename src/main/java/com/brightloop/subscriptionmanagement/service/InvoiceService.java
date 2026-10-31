package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.response.InvoiceResponseDto;
import com.brightloop.subscriptionmanagement.entity.Invoice;
import com.brightloop.subscriptionmanagement.entity.InvoiceStatus;
import com.brightloop.subscriptionmanagement.entity.Subscription;
import com.brightloop.subscriptionmanagement.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Invoice generation and mapping.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Issues the invoice for a subscription's first billing period (the one starting on its start date).
     * Idempotent: if that invoice already exists it is returned instead of creating a duplicate.
     */
    @Transactional
    public Invoice generateFirstInvoice(Subscription subscription) {
        String billingPeriod = subscription.getStartDate().toString();

        Optional<Invoice> existing = invoiceRepository.findBySubscriptionIdAndBillingPeriod(subscription.getId(), billingPeriod);
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDate issueDate = LocalDate.now();
        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .subscription(subscription)
                .billingPeriod(billingPeriod)
                .issueDate(issueDate)
                .dueDate(BillingCalendar.dueDate(issueDate, subscription.getPaymentTerms()))
                .subtotal(subscription.getSubtotal())
                .totalTax(subscription.getTotalTax())
                .totalDiscount(subscription.getTotalDiscount())
                .totalAmount(subscription.getGrandTotal())
                // CONFIRMED = finalized and sent to the customer, awaiting payment
                .status(InvoiceStatus.CONFIRMED)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} issued for subscription {} (period {})",
                saved.getInvoiceNumber(), subscription.getSubscriptionNumber(), billingPeriod);
        return saved;
    }

    public InvoiceResponseDto mapToDto(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subscriptionId(subscription.getId())
                .subscriptionNumber(subscription.getSubscriptionNumber())
                .planName(subscription.getPlan().getName())
                .customerName(subscription.getCustomer().getFirstName() + " " + subscription.getCustomer().getLastName())
                .customerEmail(subscription.getCustomer().getEmail())
                .billingPeriod(invoice.getBillingPeriod())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .paymentTerms(subscription.getPaymentTerms())
                .subtotal(invoice.getSubtotal())
                .totalTax(invoice.getTotalTax())
                .totalDiscount(invoice.getTotalDiscount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .overdue(invoice.getStatus() == InvoiceStatus.CONFIRMED && invoice.getDueDate().isBefore(LocalDate.now()))
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    /** Sequential, zero-padded number (INV-0001), same scheme as subscription numbers. */
    private synchronized String generateInvoiceNumber() {
        long nextIndex = invoiceRepository.findTopByOrderByIdDesc().map(i -> i.getId() + 1).orElse(1L);
        String candidate = String.format("INV-%04d", nextIndex);
        while (invoiceRepository.existsByInvoiceNumber(candidate)) {
            nextIndex++;
            candidate = String.format("INV-%04d", nextIndex);
        }
        return candidate;
    }
}
