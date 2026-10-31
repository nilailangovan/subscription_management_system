package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a variant of a base Product.
 *
 * Each variant specifies an attribute (e.g. "Brand", "Tier", "Edition")
 * and value (e.g. "Odoo", "Enterprise", "Pro").
 *
 * Pricing Formula:
 *   Final Price = Product.salesPrice + Variant.extraPrice
 */
@Entity
@Table(
    name = "product_variants",
    indexes = {
        // Index on product_id for fast queries per RULES.md & Phase 2 specs
        @Index(name = "idx_product_variants_product_id", columnList = "product_id"),
        @Index(name = "idx_product_variants_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The parent product this variant belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** The attribute name (e.g. "Brand", "Storage", "Support Level") */
    @Column(nullable = false)
    private String attribute;

    /** The attribute value (e.g. "Odoo", "100GB", "24/7 Dedicated") */
    @Column(nullable = false)
    private String value;

    /**
     * Additional price added to the parent product's salesPrice.
     * Can be zero or positive. Cannot be negative.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the total final price for this variant:
     *   Final Price = Product.salesPrice + extraPrice
     *
     * @return the total price, or extraPrice if product is null
     */
    public BigDecimal calculateFinalPrice() {
        if (this.product == null || this.product.getSalesPrice() == null) {
            return this.extraPrice != null ? this.extraPrice : BigDecimal.ZERO;
        }
        BigDecimal extra = this.extraPrice != null ? this.extraPrice : BigDecimal.ZERO;
        return this.product.getSalesPrice().add(extra);
    }
}

