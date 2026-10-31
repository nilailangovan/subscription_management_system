package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for invoices. Customer-scoped queries go through subscription.customer.id.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Used for idempotent generation: one invoice per subscription and billing period */
    Optional<Invoice> findBySubscriptionIdAndBillingPeriod(Long subscriptionId, String billingPeriod);

    Page<Invoice> findBySubscriptionCustomerId(Long customerId, Pageable pageable);

    Page<Invoice> findBySubscriptionCustomerIdAndSubscriptionId(Long customerId, Long subscriptionId, Pageable pageable);

    Optional<Invoice> findByIdAndSubscriptionCustomerId(Long id, Long customerId);

    Optional<Invoice> findTopByOrderByIdDesc();

    boolean existsByInvoiceNumber(String invoiceNumber);
}
