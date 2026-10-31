package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.CreateProductVariantRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateProductVariantRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ProductVariantResponseDto;
import com.brightloop.subscriptionmanagement.entity.Product;
import com.brightloop.subscriptionmanagement.entity.ProductVariant;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service managing ProductVariant entity operations.
 *
 * Implements the pricing calculation formula:
 *   Final Price = Product.salesPrice + Variant.extraPrice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    /**
     * Create a new variant for a product.
     */
    @Transactional
    public ProductVariantResponseDto createVariant(Long productId, CreateProductVariantRequestDto request) {
        log.info("Creating variant for product id {}: attribute={}, value={}",
                productId, request.getAttribute(), request.getValue());

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (request.getExtraPrice() != null && request.getExtraPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Variant extra price cannot be negative");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .attribute(request.getAttribute().trim())
                .value(request.getValue().trim())
                .extraPrice(request.getExtraPrice() != null ? request.getExtraPrice() : BigDecimal.ZERO)
                .active(true)
                .build();

        ProductVariant saved = productVariantRepository.save(variant);
        return mapToDto(saved);
    }

    /**
     * Update an existing product variant.
     */
    @Transactional
    public ProductVariantResponseDto updateVariant(Long variantId, UpdateProductVariantRequestDto request) {
        log.info("Updating variant id: {}", variantId);

        ProductVariant variant = productVariantRepository.findByIdAndActiveTrue(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));

        if (request.getExtraPrice() != null && request.getExtraPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Variant extra price cannot be negative");
        }

        if (request.getAttribute() != null) {
            variant.setAttribute(request.getAttribute().trim());
        }
        if (request.getValue() != null) {
            variant.setValue(request.getValue().trim());
        }
        if (request.getExtraPrice() != null) {
            variant.setExtraPrice(request.getExtraPrice());
        }
        if (request.getActive() != null) {
            variant.setActive(request.getActive());
        }

        ProductVariant updated = productVariantRepository.save(variant);
        return mapToDto(updated);
    }

    /**
     * Get a single variant by ID.
     */
    @Transactional(readOnly = true)
    public ProductVariantResponseDto getVariantById(Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdAndActiveTrue(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));
        return mapToDto(variant);
    }

    /**
     * List active variants for a parent product.
     */
    @Transactional(readOnly = true)
    public Page<ProductVariantResponseDto> getVariantsByProductId(Long productId, Pageable pageable) {
        // Ensure parent product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        Page<ProductVariant> page = productVariantRepository.findByProductIdAndActiveTrue(productId, pageable);
        return page.map(this::mapToDto);
    }

    /**
     * Soft delete a variant.
     */
    @Transactional
    public void deleteVariant(Long variantId) {
        log.info("Soft deleting variant id: {}", variantId);
        ProductVariant variant = productVariantRepository.findByIdAndActiveTrue(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + variantId));
        variant.setActive(false);
        productVariantRepository.save(variant);
    }

    /**
     * Helper to compute final price: salesPrice + extraPrice.
     */
    public BigDecimal calculateFinalPrice(BigDecimal salesPrice, BigDecimal extraPrice) {
        BigDecimal base = salesPrice != null ? salesPrice : BigDecimal.ZERO;
        BigDecimal extra = extraPrice != null ? extraPrice : BigDecimal.ZERO;
        return base.add(extra);
    }

    /**
     * Helper to map ProductVariant to ProductVariantResponseDto.
     */
    public ProductVariantResponseDto mapToDto(ProductVariant variant) {
        Product prod = variant.getProduct();
        BigDecimal basePrice = prod != null && prod.getSalesPrice() != null ? prod.getSalesPrice() : BigDecimal.ZERO;
        BigDecimal finalPrice = variant.calculateFinalPrice();

        return ProductVariantResponseDto.builder()
                .id(variant.getId())
                .productId(prod != null ? prod.getId() : null)
                .productName(prod != null ? prod.getName() : null)
                .attribute(variant.getAttribute())
                .value(variant.getValue())
                .baseSalesPrice(basePrice)
                .extraPrice(variant.getExtraPrice())
                .finalPrice(finalPrice)
                .active(variant.isActive())
                .createdAt(variant.getCreatedAt())
                .build();
    }
}
