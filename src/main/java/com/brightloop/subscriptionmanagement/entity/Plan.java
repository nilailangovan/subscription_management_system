package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a Subscription Plan in LoopPay.
 *
 * Defines pricing, recurring billing schedule, duration, and operational flags:
 *  - autoClose : If true, scheduler automatically closes the plan once endDate passes.
 *  - closable  : If false, manual close action is rejected.
 *  - pausable  : If false, manual pause action is rejected.
 *  - renewable : If false, renew action is rejected.
 *
 * DB Indexes (Per RULES.md & Phase 2 specs):
 *  - status
 *  - product_id
 *  - billing_period
 */
@Entity
@Table(
    name = "plans",
    indexes = {
        @Index(name = "idx_plans_status", columnList = "status"),
        @Index(name = "idx_plans_product_id", columnList = "product_id"),
        @Index(name = "idx_plans_billing_period", columnList = "billing_period"),
        @Index(name = "idx_plans_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plan name (e.g. "Pro Annual Subscription", "Monthly Cloud ERP Starter") */
    @Column(nullable = false)
    private String name;

    /** The optional base product this plan is attached to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Recurring subscription fee for this plan (cannot be negative) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Recurring frequency: DAILY, WEEKLY, MONTHLY, or YEARLY */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    /** Minimum required quantity per order (at least 1) */
    @Column(nullable = false)
    @Builder.Default
    private int minQuantity = 1;

    /** Effective start date of the plan availability */
    @Column(nullable = false)
    private LocalDate startDate;

    /** Expiry / end date of the plan availability (must be after startDate) */
    @Column(nullable = false)
    private LocalDate endDate;

    /** If true, scheduled cron automatically closes the plan when endDate passes */
    @Column(nullable = false)
    @Builder.Default
    private boolean autoClose = false;

    /** If false, admin close action is rejected with 400 Bad Request */
    @Column(nullable = false)
    @Builder.Default
    private boolean closable = true;

    /** If false, admin pause action is rejected with 400 Bad Request */
    @Column(nullable = false)
    @Builder.Default
    private boolean pausable = true;

    /** If false, admin renew action is rejected with 400 Bad Request */
    @Column(nullable = false)
    @Builder.Default
    private boolean renewable = true;

    /** Current lifecycle status: ACTIVE, PAUSED, CLOSED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlanStatus status = PlanStatus.ACTIVE;

    /** Soft-delete flag: true = active, false = soft deleted */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PlanStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

