package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.CreateProductRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateProductRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ProductResponseDto;
import com.brightloop.subscriptionmanagement.entity.Product;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service handling base Product operations.
 * Enforces business rules:
 * - Prices cannot be negative
 * - Soft deletion
 * - DTO sanitization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Create a new product.
     */
    @Transactional
    public ProductResponseDto createProduct(CreateProductRequestDto request) {
        log.info("Creating product: {}", request.getName());

        validatePrices(request.getSalesPrice(), request.getCostPrice());

        Product product = Product.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .salesPrice(request.getSalesPrice())
                .costPrice(request.getCostPrice())
                .notes(request.getNotes())
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        return mapToDto(saved);
    }

    /**
     * Update an existing product.
     */
    @Transactional
    public ProductResponseDto updateProduct(Long id, UpdateProductRequestDto request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        validatePrices(request.getSalesPrice(), request.getCostPrice());

        if (request.getName() != null) {
            product.setName(request.getName().trim());
        }
        if (request.getType() != null) {
            product.setType(request.getType());
        }
        if (request.getSalesPrice() != null) {
            product.setSalesPrice(request.getSalesPrice());
        }
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        if (request.getNotes() != null) {
            product.setNotes(request.getNotes());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.save(product);
        return mapToDto(updated);
    }

    /**
     * Get product by ID (active records only).
     */
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToDto(product);
    }

    /**
     * Get paginated products with optional search by name.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(String search, Pageable pageable) {
        Page<Product> page;
        if (search != null && !search.trim().isEmpty()) {
            page = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(search.trim(), pageable);
        } else {
            page = productRepository.findByActiveTrue(pageable);
        }
        return page.map(this::mapToDto);
    }

    /**
     * Soft delete a product by setting active = false.
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Soft deleting product id: {}", id);
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Helper to validate that prices are non-negative.
     */
    private void validatePrices(BigDecimal salesPrice, BigDecimal costPrice) {
        if (salesPrice != null && salesPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Sales price cannot be negative");
        }
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Cost price cannot be negative");
        }
    }

    /**
     * Helper to map Product entity to ProductResponseDto.
     */
    public ProductResponseDto mapToDto(Product product) {
        int variantsCount = product.getVariants() != null ? product.getVariants().size() : 0;
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .type(product.getType())
                .salesPrice(product.getSalesPrice())
                .costPrice(product.getCostPrice())
                .notes(product.getNotes())
                .active(product.isActive())
                .variantsCount(variantsCount)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
