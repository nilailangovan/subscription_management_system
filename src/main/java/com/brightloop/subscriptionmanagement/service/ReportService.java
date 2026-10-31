package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.ReportFilterDto;
import com.brightloop.subscriptionmanagement.dto.request.ReportFilterDto.InvoiceState;
import com.brightloop.subscriptionmanagement.dto.response.DashboardSummaryDto;
import com.brightloop.subscriptionmanagement.dto.response.InvoiceResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.PaymentResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
import com.brightloop.subscriptionmanagement.entity.Invoice;
import com.brightloop.subscriptionmanagement.entity.InvoiceStatus;
import com.brightloop.subscriptionmanagement.entity.Payment;
import com.brightloop.subscriptionmanagement.entity.Subscription;
import com.brightloop.subscriptionmanagement.entity.SubscriptionStatus;
import com.brightloop.subscriptionmanagement.entity.User;
import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;
import com.brightloop.subscriptionmanagement.repository.InvoiceRepository;
import com.brightloop.subscriptionmanagement.repository.PaymentRepository;
import com.brightloop.subscriptionmanagement.repository.ReportSpecifications;
import com.brightloop.subscriptionmanagement.repository.SubscriptionRepository;
import com.brightloop.subscriptionmanagement.repository.UserRepository;
import com.brightloop.subscriptionmanagement.service.ExportService.Report;
import com.brightloop.subscriptionmanagement.service.ExportService.Table;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Report lists, dashboard KPIs and the data behind every export.
 *
 * Lists and exports share {@link ReportSpecifications}, so an exported file holds exactly
 * the rows the matching screen shows (exports ignore pagination).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    /** Exports larger than this are refused with a 400 so one request cannot exhaust the server. */
    public static final int MAX_EXPORT_ROWS = 50_000;

    private static final List<String> INVOICE_HEADERS = List.of(
            "Invoice #", "Subscription", "Customer", "Email", "Plan", "Issue Date", "Due Date", "Status", "Days Overdue", "Amount");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final EntityManager entityManager;

    // =========================================================================
    // LISTS (paginated, RULES.md section 9)
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<InvoiceResponseDto> listInvoices(ReportFilterDto filter, Pageable pageable) {
        return invoiceRepository.findAll(ReportSpecifications.invoices(filter, LocalDate.now()), pageable)
                .map(invoiceService::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> listPayments(ReportFilterDto filter, Pageable pageable) {
        return paymentRepository.findAll(ReportSpecifications.payments(filter), pageable)
                .map(paymentService::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponseDto> listSubscriptions(ReportFilterDto filter, Pageable pageable) {
        return subscriptionRepository.findAll(ReportSpecifications.subscriptions(filter), pageable)
                .map(subscriptionService::mapToResponseDto);
    }

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    /**
     * Dashboard KPIs. The date range only narrows collected revenue; receivables and
     * subscription counts are balances as of today. Search and customer apply to everything.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDto dashboardSummary(ReportFilterDto filter) {
        LocalDate today = LocalDate.now();
        ReportFilterDto asOfToday = withoutDatesAndStatuses(filter);

        Specification<Payment> collected = ReportSpecifications.payments(filter);
        Specification<Invoice> outstanding = ReportSpecifications.invoices(
                asOfToday.toBuilder().invoiceStatus(InvoiceState.CONFIRMED).build(), today);
        Specification<Invoice> overdue = ReportSpecifications.invoices(
                asOfToday.toBuilder().invoiceStatus(InvoiceState.OVERDUE).build(), today);
        Specification<Subscription> active = ReportSpecifications.subscriptions(
                asOfToday.toBuilder().subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        return DashboardSummaryDto.builder()
                .collectedAmount(sum(Payment.class, collected, "amount"))
                .paymentCount(paymentRepository.count(collected))
                .outstandingAmount(sum(Invoice.class, outstanding, "totalAmount"))
                .outstandingCount(invoiceRepository.count(outstanding))
                .overdueAmount(sum(Invoice.class, overdue, "totalAmount"))
                .overdueCount(invoiceRepository.count(overdue))
                .activeSubscriptions(subscriptionRepository.count(active))
                .totalSubscriptions(subscriptionRepository.count(ReportSpecifications.subscriptions(asOfToday)))
                .build();
    }

    // =========================================================================
    // EXPORTS
    // =========================================================================

    /** Dashboard export: the KPI cards plus the invoice table (overdue unless another invoice status is chosen). */
    @Transactional(readOnly = true)
    public Report dashboardReport(ReportFilterDto filter) {
        LocalDate today = LocalDate.now();
        DashboardSummaryDto kpis = dashboardSummary(filter);

        Table kpiTable = new Table("Summary", List.of("Metric", "Value"), List.of(
                row("Revenue collected", kpis.getCollectedAmount()),
                row("Payments received", kpis.getPaymentCount()),
                row("Outstanding receivables", kpis.getOutstandingAmount()),
                row("Unpaid invoices", kpis.getOutstandingCount()),
                row("Overdue amount", kpis.getOverdueAmount()),
                row("Overdue invoices", kpis.getOverdueCount()),
                row("Active subscriptions", kpis.getActiveSubscriptions()),
                row("All subscriptions", kpis.getTotalSubscriptions())), false);

        // Like the dashboard table: balances as of today, so the date range does not apply; oldest due date first
        InvoiceState state = filter.getInvoiceStatus() != null ? filter.getInvoiceStatus() : InvoiceState.OVERDUE;
        ReportFilterDto invoiceFilter = withoutDatesAndStatuses(filter).toBuilder().invoiceStatus(state).build();
        Specification<Invoice> spec = ReportSpecifications.invoices(invoiceFilter, today);
        checkRowCap(invoiceRepository.count(spec));
        List<Invoice> invoices = invoiceRepository.findAll(spec, Sort.by("dueDate", "id"));

        String summary = describe(filter.toBuilder().invoiceStatus(state).build(), "Payments received");
        return new Report("dashboard", "Finance Dashboard", summary,
                List.of(kpiTable, invoiceTable(label(state) + " invoices", invoices, today)));
    }

    @Transactional(readOnly = true)
    public Report invoicesReport(ReportFilterDto filter, Sort sort) {
        LocalDate today = LocalDate.now();
        Specification<Invoice> spec = ReportSpecifications.invoices(filter, today);
        checkRowCap(invoiceRepository.count(spec));
        List<Invoice> invoices = invoiceRepository.findAll(spec, sort);
        return new Report("invoices", "Invoices", describe(filter, "Issued"), List.of(invoiceTable("Invoices", invoices, today)));
    }

    /** The logged-in customer's own invoices; the customer comes from the JWT, never from the request. */
    @Transactional(readOnly = true)
    public Report myInvoicesReport(User customer) {
        ReportFilterDto filter = ReportFilterDto.builder().customerId(customer.getId()).build();
        LocalDate today = LocalDate.now();
        Specification<Invoice> spec = ReportSpecifications.invoices(filter, today);
        checkRowCap(invoiceRepository.count(spec));
        List<Invoice> invoices = invoiceRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        String title = "Invoices for " + customer.getFirstName() + " " + customer.getLastName();
        return new Report("my-invoices", title, "All your invoices", List.of(invoiceTable("Invoices", invoices, today)));
    }

    @Transactional(readOnly = true)
    public Report paymentsReport(ReportFilterDto filter, Sort sort) {
        Specification<Payment> spec = ReportSpecifications.payments(filter);
        checkRowCap(paymentRepository.count(spec));
        List<List<Object>> rows = paymentRepository.findAll(spec, sort).stream()
                .map(payment -> {
                    Invoice invoice = payment.getInvoice();
                    User customer = invoice.getSubscription().getCustomer();
                    return row(payment.getPaymentNumber(), invoice.getInvoiceNumber(), fullName(customer), customer.getEmail(),
                            label(payment.getMethod()), payment.getReference(), payment.getPaidDate(), payment.getAmount());
                })
                .toList();
        Table table = new Table("Payments",
                row("Payment #", "Invoice #", "Customer", "Email", "Method", "Reference", "Paid Date", "Amount"), rows, true);
        return new Report("payments", "Payments", describe(filter, "Paid"), List.of(table));
    }

    @Transactional(readOnly = true)
    public Report subscriptionsReport(ReportFilterDto filter, Sort sort) {
        LocalDate today = LocalDate.now();
        Specification<Subscription> spec = ReportSpecifications.subscriptions(filter);
        checkRowCap(subscriptionRepository.count(spec));
        List<List<Object>> rows = subscriptionRepository.findAll(spec, sort).stream()
                .map(subscription -> {
                    User customer = subscription.getCustomer();
                    var period = subscription.getPlan().getBillingPeriod();
                    return row(subscription.getSubscriptionNumber(), fullName(customer), customer.getEmail(),
                            subscription.getPlan().getName(), label(period), subscription.getStartDate(),
                            BillingCalendar.nextBillingDate(subscription.getStartDate(), subscription.getExpiryDate(),
                                    period, subscription.getStatus(), today),
                            label(subscription.getStatus()), subscription.getUpdatedAt(), subscription.getGrandTotal());
                })
                .toList();
        // No totals row: recurring values mix monthly, yearly, ... periods, so their sum means nothing
        Table table = new Table("Subscriptions", List.of("Subscription #", "Customer", "Email", "Plan", "Billing", "Start Date",
                "Next Billing", "Status", "Last Updated", "Recurring Value"), rows, false);
        return new Report("subscriptions", "Subscriptions", describe(filter, "Updated"), List.of(table));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** @throws BusinessRuleException (400) when the export would exceed {@link #MAX_EXPORT_ROWS} rows */
    void checkRowCap(long rowCount) {
        if (rowCount > MAX_EXPORT_ROWS) {
            throw new BusinessRuleException(String.format(Locale.ENGLISH,
                    "This export would contain %,d rows, more than the limit of %,d. Narrow the date range or add a search, then try again.",
                    rowCount, MAX_EXPORT_ROWS));
        }
    }

    private Table invoiceTable(String name, List<Invoice> invoices, LocalDate today) {
        List<List<Object>> rows = invoices.stream()
                .map(invoice -> {
                    Subscription subscription = invoice.getSubscription();
                    User customer = subscription.getCustomer();
                    boolean overdue = invoice.getStatus() == InvoiceStatus.CONFIRMED && invoice.getDueDate().isBefore(today);
                    return row(invoice.getInvoiceNumber(), subscription.getSubscriptionNumber(), fullName(customer),
                            customer.getEmail(), subscription.getPlan().getName(), invoice.getIssueDate(), invoice.getDueDate(),
                            invoiceStateLabel(invoice, today),
                            overdue ? ChronoUnit.DAYS.between(invoice.getDueDate(), today) : null,
                            invoice.getTotalAmount());
                })
                .toList();
        return new Table(name, INVOICE_HEADERS, rows, true);
    }

    /** Same states the screens show: Draft, Paid, or a sent invoice split into Outstanding / Overdue. */
    private String invoiceStateLabel(Invoice invoice, LocalDate today) {
        return switch (invoice.getStatus()) {
            case DRAFT -> "Draft";
            case PAID -> "Paid";
            case CONFIRMED -> invoice.getDueDate().isBefore(today) ? "Overdue" : "Outstanding";
        };
    }

    /** Human-readable filter line for the file header, e.g. "Issued 2026-10-01 to 2026-10-31 · Search: “acme”". */
    private String describe(ReportFilterDto filter, String dateLabel) {
        List<String> parts = new ArrayList<>();
        if (filter.getFrom() != null && filter.getTo() != null) {
            parts.add(dateLabel + " " + filter.getFrom() + " to " + filter.getTo());
        } else if (filter.getFrom() != null) {
            parts.add(dateLabel + " from " + filter.getFrom());
        } else if (filter.getTo() != null) {
            parts.add(dateLabel + " up to " + filter.getTo());
        }
        if (filter.getInvoiceStatus() != null) {
            parts.add("Invoice status: " + label(filter.getInvoiceStatus()));
        }
        if (filter.getSubscriptionStatus() != null) {
            parts.add("Subscription status: " + label(filter.getSubscriptionStatus()));
        }
        if (filter.getCustomerId() != null) {
            parts.add("Customer: " + userRepository.findById(filter.getCustomerId())
                    .map(customer -> fullName(customer) + " (" + customer.getEmail() + ")")
                    .orElse("#" + filter.getCustomerId()));
        }
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            parts.add("Search: “" + filter.getSearch().trim() + "”");
        }
        return parts.isEmpty() ? "None (all records)" : String.join(" · ", parts);
    }

    /** Copy of the filter without the date range and statuses, for "as of today" figures. */
    private ReportFilterDto withoutDatesAndStatuses(ReportFilterDto filter) {
        return filter.toBuilder().from(null).to(null).invoiceStatus(null).subscriptionStatus(null).build();
    }

    /** SUM(field) over the rows a specification matches; zero when nothing matches. */
    private <T> BigDecimal sum(Class<T> type, Specification<T> spec, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<T> root = query.from(type);
        query.select(cb.sum(root.get(field))).where(spec.toPredicate(root, query, cb));
        BigDecimal total = entityManager.createQuery(query).getSingleResult();
        return total != null ? total : BigDecimal.ZERO;
    }

    /** Row of cell values; unlike List.of it accepts nulls (empty cells). */
    private static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    /** BANK_TRANSFER → "Bank transfer" */
    private static String label(Enum<?> value) {
        String words = value.name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
