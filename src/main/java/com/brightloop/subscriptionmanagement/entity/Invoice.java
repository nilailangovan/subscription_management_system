package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Invoice issued for one billing period of a subscription.
 *
 * Idempotency (RULES.md section 8): the unique constraint on (subscription_id, billing_period)
 * guarantees that generating an invoice twice for the same period never creates a duplicate.
 */
@Entity
@Table(
    name = "invoices",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoices_subscription_period", columnNames = {"subscription_id", "billing_period"})
    },
    indexes = {
        @Index(name = "idx_invoices_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_invoices_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable number, e.g. INV-0001 */
    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /** Start date (yyyy-MM-dd) of the billing period this invoice covers */
    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTax;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDiscount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
