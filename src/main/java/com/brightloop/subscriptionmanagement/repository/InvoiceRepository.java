package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for invoices. Customer-scoped queries go through subscription.customer.id.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    // Report lists and exports: subscription, customer and plan come in the same query (no N+1)
    @Override
    @EntityGraph(attributePaths = {"subscription", "subscription.customer", "subscription.plan"})
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"subscription", "subscription.customer", "subscription.plan"})
    List<Invoice> findAll(Specification<Invoice> spec, Sort sort);

    /** Used for idempotent generation: one invoice per subscription and billing period */
    Optional<Invoice> findBySubscriptionIdAndBillingPeriod(Long subscriptionId, String billingPeriod);

    Page<Invoice> findBySubscriptionCustomerId(Long customerId, Pageable pageable);

    Page<Invoice> findBySubscriptionCustomerIdAndSubscriptionId(Long customerId, Long subscriptionId, Pageable pageable);

    Optional<Invoice> findByIdAndSubscriptionCustomerId(Long id, Long customerId);

    Optional<Invoice> findTopByOrderByIdDesc();

    boolean existsByInvoiceNumber(String invoiceNumber);
}
