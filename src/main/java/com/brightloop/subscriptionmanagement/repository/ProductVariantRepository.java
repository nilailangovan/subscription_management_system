package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for ProductVariant operations.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Find active variants belonging to a specific product (paginated).
     */
    Page<ProductVariant> findByProductIdAndActiveTrue(Long productId, Pageable pageable);

    /**
     * Find all active variants for a specific product as a list.
     */
    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    /**
     * Find an active variant by its ID.
     */
    Optional<ProductVariant> findByIdAndActiveTrue(Long id);
}

