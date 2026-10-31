package com.brightloop.subscriptionmanagement.dto.response;

import com.brightloop.subscriptionmanagement.entity.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sanitized response for a Product (never exposes JPA entity directly).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private Long id;
    private String name;
    private ProductType type;
    private BigDecimal salesPrice;
    private BigDecimal costPrice;
    private String notes;
    private boolean active;
    private int variantsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

