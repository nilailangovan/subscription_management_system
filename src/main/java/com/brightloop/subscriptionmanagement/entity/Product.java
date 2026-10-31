package com.brightloop.subscriptionmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a base Product in LoopPay.
 *
 * Products can be of type SERVICE, GOODS, or SOFTWARE.
 * A product defines standard salesPrice and costPrice, and can have
 * multiple ProductVariants with extraPrice modifiers.
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_type", columnList = "type"),
        @Index(name = "idx_products_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the product (e.g. "Cloud ERP Software", "IT Consultation") */
    @Column(nullable = false)
    private String name;

    /** Category type: SERVICE, GOODS, or SOFTWARE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    /** Base selling price for customers (cannot be negative) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salesPrice;

    /** Internal cost price to the company (cannot be negative) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    /** Optional notes, specifications, or description */
    @Column(length = 1000)
    private String notes;

    /** Soft-delete flag: true = active/visible, false = soft deleted */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Variants of this product (e.g. Brand = "Odoo", Storage = "500GB") */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

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
}

