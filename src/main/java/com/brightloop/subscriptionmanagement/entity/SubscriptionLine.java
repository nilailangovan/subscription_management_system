package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents an individual product line in a subscription order.
 *
 * Each line specifies a product, quantity, unit price, calculated tax,
 * discount, and total line amount.
 */
@Entity
@Table(
    name = "subscription_lines",
    indexes = {
        @Index(name = "idx_subscription_lines_sub_id", columnList = "subscription_id"),
        @Index(name = "idx_subscription_lines_product_id", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent subscription reference */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /** Product included in this subscription line */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Number of units of this product (must be >= 1) */
    @Column(nullable = false)
    private Integer quantity;

    /** Unit price charged per item */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Tax amount applied to this line */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    /** Discount applied to this line */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    /** Total amount for this line item (unitPrice * quantity + tax - discount) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}

