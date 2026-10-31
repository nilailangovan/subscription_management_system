package com.brightloop.subscriptionmanagement.dto.response;

import com.brightloop.subscriptionmanagement.entity.BillingPeriod;
import com.brightloop.subscriptionmanagement.entity.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A plan as shown to customers: only subscribable plans, with their products,
 * variants and final prices (product sales price + variant extra price).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalPlanResponseDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private BillingPeriod billingPeriod;
    private int minQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Product> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private Long id;
        private String name;
        private ProductType type;
        private String notes;
        private BigDecimal salesPrice;
        private List<Variant> variants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variant {
        private Long id;
        private String attribute;
        private String value;
        private BigDecimal extraPrice;
        private BigDecimal finalPrice;
    }
}
