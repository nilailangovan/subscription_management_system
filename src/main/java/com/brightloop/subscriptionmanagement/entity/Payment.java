package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A payment received against an invoice. Recording one marks the invoice PAID.
 *
 * One payment settles one invoice in full; the unique invoice_id column stops an invoice
 * from being paid twice.
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_paid_date", columnList = "paid_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable number, e.g. PAY-0001 */
    @Column(nullable = false, unique = true)
    private String paymentNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true)
    private Invoice invoice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    /** The day the money was received */
    @Column(name = "paid_date", nullable = false)
    private LocalDate paidDate;

    /** Optional bank or card reference */
    private String reference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
