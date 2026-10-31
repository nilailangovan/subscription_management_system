package com.brightloop.subscriptionmanagement.repository;

import com.brightloop.subscriptionmanagement.dto.request.ReportFilterDto;
import com.brightloop.subscriptionmanagement.entity.Invoice;
import com.brightloop.subscriptionmanagement.entity.InvoiceStatus;
import com.brightloop.subscriptionmanagement.entity.Payment;
import com.brightloop.subscriptionmanagement.entity.Subscription;
import com.brightloop.subscriptionmanagement.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Query filters for the report lists, KPIs and exports.
 *
 * Lists, dashboard figures and exported files all use these same specifications,
 * so a file always contains exactly the rows the matching screen shows.
 * Every filter field is optional; a null field adds no condition.
 */
public final class ReportSpecifications {

    private ReportSpecifications() {
    }

    /**
     * Invoices matching the filter. The date range applies to the issue date.
     *
     * @param today reference day for OUTSTANDING / OVERDUE (an unpaid invoice is overdue once its due date has passed)
     */
    public static Specification<Invoice> invoices(ReportFilterDto filter, LocalDate today) {
        return (root, query, cb) -> {
            Join<Invoice, Subscription> subscription = root.join("subscription");
            Join<Subscription, User> customer = subscription.join("customer");

            List<Predicate> predicates = new ArrayList<>();
            addDateRange(predicates, cb, root.get("issueDate"), filter);
            addCustomer(predicates, cb, customer, filter);
            addSearch(predicates, cb, filter, customer, root.get("invoiceNumber"), subscription.get("subscriptionNumber"));

            if (filter.getInvoiceStatus() != null) {
                predicates.add(invoiceState(cb, root.get("status"), root.get("dueDate"), filter.getInvoiceStatus(), today));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Payments matching the filter. The date range applies to the paid date. */
    public static Specification<Payment> payments(ReportFilterDto filter) {
        return (root, query, cb) -> {
            Join<Payment, Invoice> invoice = root.join("invoice");
            Join<Invoice, Subscription> subscription = invoice.join("subscription");
            Join<Subscription, User> customer = subscription.join("customer");

            List<Predicate> predicates = new ArrayList<>();
            addDateRange(predicates, cb, root.get("paidDate"), filter);
            addCustomer(predicates, cb, customer, filter);
            addSearch(predicates, cb, filter, customer, root.get("paymentNumber"), invoice.get("invoiceNumber"));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Live (not soft-deleted) subscriptions matching the filter.
     * The date range applies to the last-updated date, i.e. "subscriptions with activity in this period".
     */
    public static Specification<Subscription> subscriptions(ReportFilterDto filter) {
        return (root, query, cb) -> {
            Join<Subscription, User> customer = root.join("customer");

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            // updatedAt is a timestamp: [from 00:00, the day after "to" 00:00) covers both whole days
            if (filter.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getFrom().atStartOfDay()));
            }
            if (filter.getTo() != null) {
                predicates.add(cb.lessThan(root.get("updatedAt"), filter.getTo().plusDays(1).atStartOfDay()));
            }
            addCustomer(predicates, cb, customer, filter);
            addSearch(predicates, cb, filter, customer, root.get("subscriptionNumber"));

            if (filter.getSubscriptionStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getSubscriptionStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** OUTSTANDING and OVERDUE are CONFIRMED invoices split by due date; the other values match the stored status. */
    private static Predicate invoiceState(CriteriaBuilder cb, Path<InvoiceStatus> status, Path<LocalDate> dueDate,
                                          ReportFilterDto.InvoiceState state, LocalDate today) {
        return switch (state) {
            case DRAFT -> cb.equal(status, InvoiceStatus.DRAFT);
            case CONFIRMED -> cb.equal(status, InvoiceStatus.CONFIRMED);
            case PAID -> cb.equal(status, InvoiceStatus.PAID);
            case OUTSTANDING -> cb.and(cb.equal(status, InvoiceStatus.CONFIRMED), cb.greaterThanOrEqualTo(dueDate, today));
            case OVERDUE -> cb.and(cb.equal(status, InvoiceStatus.CONFIRMED), cb.lessThan(dueDate, today));
        };
    }

    private static void addDateRange(List<Predicate> predicates, CriteriaBuilder cb, Path<LocalDate> date, ReportFilterDto filter) {
        if (filter.getFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(date, filter.getFrom()));
        }
        if (filter.getTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(date, filter.getTo()));
        }
    }

    private static void addCustomer(List<Predicate> predicates, CriteriaBuilder cb, Join<?, User> customer, ReportFilterDto filter) {
        if (filter.getCustomerId() != null) {
            predicates.add(cb.equal(customer.get("id"), filter.getCustomerId()));
        }
    }

    /** Case-insensitive "contains" on the customer's full name, email and the given document numbers. */
    @SafeVarargs
    private static void addSearch(List<Predicate> predicates, CriteriaBuilder cb, ReportFilterDto filter,
                                  Join<?, User> customer, Path<String>... numbers) {
        String search = filter.getSearch() == null ? "" : filter.getSearch().trim();
        if (search.isEmpty()) {
            return;
        }
        // Escape LIKE wildcards so "%" or "_" typed by the user are matched literally
        String pattern = "%" + search.toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";

        List<Expression<String>> fields = new ArrayList<>(Arrays.asList(numbers));
        fields.add(customer.get("email"));
        fields.add(cb.concat(cb.concat(customer.get("firstName"), " "), customer.get("lastName")));

        predicates.add(cb.or(fields.stream()
                .map(field -> cb.like(cb.lower(field), pattern, '\\'))
                .toArray(Predicate[]::new)));
    }
}
