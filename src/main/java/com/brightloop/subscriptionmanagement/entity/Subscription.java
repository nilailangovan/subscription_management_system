package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Customer Subscription contract in LoopPay.
 *
 * Lifecycle:
 *   DRAFT → QUOTATION → CONFIRMED → ACTIVE → CLOSED
 *
 * DB Indexes (Per RULES.md & Phase 3 specs):
 *  - customer_id
 *  - status
 *  - plan_id
 *  - subscription_number
 */
@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_subscriptions_customer_id", columnList = "customer_id"),
        @Index(name = "idx_subscriptions_status", columnList = "status"),
        @Index(name = "idx_subscriptions_plan_id", columnList = "plan_id"),
        @Index(name = "idx_subscriptions_number", columnList = "subscription_number"),
        @Index(name = "idx_subscriptions_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique human-readable identifier (e.g. SUB-0001) */
    @Column(name = "subscription_number", nullable = false, unique = true, length = 32)
    private String subscriptionNumber;

    /** Customer who owns this subscription */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** The billing plan associated with this subscription */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    /** Subscription validity start date */
    @Column(nullable = false)
    private LocalDate startDate;

    /** Subscription expiry date */
    @Column(nullable = false)
    private LocalDate expiryDate;

    /** Payment terms agreement (e.g. "Immediate Payment", "Net 15", "Net 30") */
    @Column(nullable = false)
    private String paymentTerms;

    /** Current lifecycle status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.DRAFT;

    /** Sum of all line item basic prices */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Total tax calculated across all lines */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    /** Total discounts applied */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    /** Grand total invoiceable amount */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    /** Associated product lines */
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubscriptionLine> lines = new ArrayList<>();

    /** Soft-delete flag (true = active, false = deleted) */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Timestamp when created */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when last updated */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SubscriptionStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Helper to add a subscription line maintaining bidirectional relationship.
     */
    public void addLine(SubscriptionLine line) {
        if (lines == null) {
            lines = new ArrayList<>();
        }
        lines.add(line);
        line.setSubscription(this);
    }

    /**
     * Helper to clear and update lines.
     */
    public void clearLines() {
        if (lines != null) {
            lines.clear();
        }
    }
}

