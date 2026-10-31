package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.entity.Payment;
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
 * Repository for payments. Filtered lists load the invoice, subscription and customer
 * in the same query (entity graph), so mapping rows causes no extra queries.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Override
    @EntityGraph(attributePaths = {"invoice", "invoice.subscription", "invoice.subscription.customer"})
    Page<Payment> findAll(Specification<Payment> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"invoice", "invoice.subscription", "invoice.subscription.customer"})
    List<Payment> findAll(Specification<Payment> spec, Sort sort);

    boolean existsByInvoiceId(Long invoiceId);

    Optional<Payment> findTopByOrderByIdDesc();

    boolean existsByPaymentNumber(String paymentNumber);
}
