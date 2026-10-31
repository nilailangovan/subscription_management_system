package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Product database operations.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search products by name (case-insensitive) across active records.
     */
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * Search products by name across all records (including soft-deleted for admin).
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * List all active products with pagination.
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Find active product by ID.
     */
    Optional<Product> findByIdAndActiveTrue(Long id);
}

