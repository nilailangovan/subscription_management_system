package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.Subscription;
import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Subscription entity operations.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find active subscription by ID.
     */
    Optional<Subscription> findByIdAndActiveTrue(Long id);

    // Ownership check for the customer portal
    boolean existsByIdAndCustomerIdAndActiveTrue(Long id, Long customerId);

    /**
     * Check if a subscription number already exists.
     */
    boolean existsBySubscriptionNumber(String subscriptionNumber);

    /**
     * Find the highest ID subscription for auto-number generation.
     */
    Optional<Subscription> findTopByOrderByIdDesc();

    // =========================================================================
    // Admin & Staff Queries
    // =========================================================================

    Page<Subscription> findByActiveTrue(Pageable pageable);

    Page<Subscription> findBySubscriptionNumberContainingIgnoreCaseAndActiveTrue(String search, Pageable pageable);

    Page<Subscription> findByStatusAndActiveTrue(SubscriptionStatus status, Pageable pageable);

    Page<Subscription> findBySubscriptionNumberContainingIgnoreCaseAndStatusAndActiveTrue(
            String search, SubscriptionStatus status, Pageable pageable);

    // =========================================================================
    // Customer Queries (Restricted to their own subscriptions)
    // =========================================================================

    Page<Subscription> findByCustomerIdAndActiveTrue(Long customerId, Pageable pageable);

    Page<Subscription> findByCustomerIdAndSubscriptionNumberContainingIgnoreCaseAndActiveTrue(
            Long customerId, String search, Pageable pageable);

    Page<Subscription> findByCustomerIdAndStatusAndActiveTrue(
            Long customerId, SubscriptionStatus status, Pageable pageable);

    Page<Subscription> findByCustomerIdAndSubscriptionNumberContainingIgnoreCaseAndStatusAndActiveTrue(
            Long customerId, String search, SubscriptionStatus status, Pageable pageable);
}

