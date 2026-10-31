package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.CreateProductRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.CreateProductVariantRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateProductRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.UpdateProductVariantRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.ProductResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.ProductVariantResponseDto;
import com.brightloop.subscriptionmanagement.service.ProductService;
import com.brightloop.subscriptionmanagement.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing endpoints for Products and Product Variants.
 *
 * RBAC Rules:
 *  - ADMIN: Create, Update, Soft-delete products and variants.
 *  - ADMIN & STAFF: View / List products and variants with pagination and search.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products & Variants", description = "Endpoints for managing products and their variants")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    // ==========================================
    // PRODUCT ENDPOINTS
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product (ADMIN only)")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> createProduct(
            @Valid @RequestBody CreateProductRequestDto request) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Product created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing product (ADMIN only)")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequestDto request) {
        ProductResponseDto response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponseDto.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a product (ADMIN only)")
    public ResponseEntity<ApiResponseDto<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponseDto.success("Product deleted successfully", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get product by ID (ADMIN and STAFF)")
    public ResponseEntity<ApiResponseDto<ProductResponseDto>> getProductById(@PathVariable Long id) {
        ProductResponseDto response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponseDto.success("Product retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List products with pagination and search (ADMIN and STAFF)")
    public ResponseEntity<ApiResponseDto<Page<ProductResponseDto>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        if (sort != null && sort.length >= 2) {
            Sort.Direction direction = sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortObj = Sort.by(direction, sort[0]);
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ProductResponseDto> response = productService.getProducts(search, pageable);
        return ResponseEntity.ok(ApiResponseDto.success("Products retrieved successfully", response));
    }

    // ==========================================
    // PRODUCT VARIANT ENDPOINTS
    // ==========================================

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a variant for a product (ADMIN only)")
    public ResponseEntity<ApiResponseDto<ProductVariantResponseDto>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductVariantRequestDto request) {
        ProductVariantResponseDto response = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Product variant created successfully", response));
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product variant (ADMIN only)")
    public ResponseEntity<ApiResponseDto<ProductVariantResponseDto>> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateProductVariantRequestDto request) {
        ProductVariantResponseDto response = productVariantService.updateVariant(variantId, request);
        return ResponseEntity.ok(ApiResponseDto.success("Product variant updated successfully", response));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a product variant (ADMIN only)")
    public ResponseEntity<ApiResponseDto<Void>> deleteVariant(@PathVariable Long variantId) {
        productVariantService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponseDto.success("Product variant deleted successfully", null));
    }

    @GetMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get product variant by ID (ADMIN and STAFF)")
    public ResponseEntity<ApiResponseDto<ProductVariantResponseDto>> getVariantById(@PathVariable Long variantId) {
        ProductVariantResponseDto response = productVariantService.getVariantById(variantId);
        return ResponseEntity.ok(ApiResponseDto.success("Product variant retrieved successfully", response));
    }

    @GetMapping("/{productId}/variants")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List variants for a product with pagination (ADMIN and STAFF)")
    public ResponseEntity<ApiResponseDto<Page<ProductVariantResponseDto>>> getVariantsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductVariantResponseDto> response = productVariantService.getVariantsByProductId(productId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success("Product variants retrieved successfully", response));
    }
}

