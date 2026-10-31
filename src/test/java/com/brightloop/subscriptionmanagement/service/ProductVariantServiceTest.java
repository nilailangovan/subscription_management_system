package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.CreateProductVariantRequestDto;
import com.brightloop.subscriptionmanagement.dto.response.ProductVariantResponseDto;
import com.brightloop.subscriptionmanagement.entity.Product;
import com.brightloop.subscriptionmanagement.entity.ProductType;
import com.brightloop.subscriptionmanagement.entity.ProductVariant;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.exception.ResourceNotFoundException;
import com.brightloop.subscriptionmanagement.repository.ProductRepository;
import com.brightloop.subscriptionmanagement.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductVariant pricing calculations and CRUD rules.
 */
@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductVariantService productVariantService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .name("Odoo ERP Suite")
                .type(ProductType.SOFTWARE)
                .salesPrice(new BigDecimal("1000.00"))
                .costPrice(new BigDecimal("400.00"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should correctly calculate final price as salesPrice + extraPrice (e.g. 1000 + 560 = 1560)")
    void testCalculateFinalPrice_Formula() {
        ProductVariant variant = ProductVariant.builder()
                .id(10L)
                .product(sampleProduct)
                .attribute("Brand")
                .value("Odoo")
                .extraPrice(new BigDecimal("560.00"))
                .active(true)
                .build();

        BigDecimal finalPrice = variant.calculateFinalPrice();
        assertEquals(new BigDecimal("1560.00"), finalPrice);

        BigDecimal helperCalc = productVariantService.calculateFinalPrice(sampleProduct.getSalesPrice(), variant.getExtraPrice());
        assertEquals(new BigDecimal("1560.00"), helperCalc);
    }

    @Test
    @DisplayName("Should successfully create product variant and return calculated final price")
    void testCreateVariant_Success() {
        CreateProductVariantRequestDto request = new CreateProductVariantRequestDto();
        request.setAttribute("Brand");
        request.setValue("Odoo");
        request.setExtraPrice(new BigDecimal("560.00"));

        ProductVariant savedVariant = ProductVariant.builder()
                .id(101L)
                .product(sampleProduct)
                .attribute("Brand")
                .value("Odoo")
                .extraPrice(new BigDecimal("560.00"))
                .active(true)
                .build();

        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);

        ProductVariantResponseDto response = productVariantService.createVariant(1L, request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Brand", response.getAttribute());
        assertEquals("Odoo", response.getValue());
        assertEquals(new BigDecimal("1000.00"), response.getBaseSalesPrice());
        assertEquals(new BigDecimal("560.00"), response.getExtraPrice());
        assertEquals(new BigDecimal("1560.00"), response.getFinalPrice());
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when variant extra price is negative")
    void testCreateVariant_NegativePrice_ThrowsException() {
        CreateProductVariantRequestDto request = new CreateProductVariantRequestDto();
        request.setAttribute("Storage");
        request.setValue("50GB");
        request.setExtraPrice(new BigDecimal("-50.00"));

        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleProduct));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> productVariantService.createVariant(1L, request));

        assertTrue(ex.getMessage().contains("Variant extra price cannot be negative"));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent product does not exist")
    void testCreateVariant_ParentProductNotFound() {
        CreateProductVariantRequestDto request = new CreateProductVariantRequestDto();
        request.setAttribute("Edition");
        request.setValue("Enterprise");
        request.setExtraPrice(BigDecimal.ZERO);

        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productVariantService.createVariant(99L, request));
    }

    @Test
    @DisplayName("Should soft delete product variant by setting active to false")
    void testDeleteVariant_SoftDelete() {
        ProductVariant variant = ProductVariant.builder()
                .id(5L)
                .product(sampleProduct)
                .attribute("Tier")
                .value("Basic")
                .extraPrice(BigDecimal.ZERO)
                .active(true)
                .build();

        when(productVariantRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(variant);

        productVariantService.deleteVariant(5L);

        assertFalse(variant.isActive());
        verify(productVariantRepository, times(1)).save(variant);
    }
}

