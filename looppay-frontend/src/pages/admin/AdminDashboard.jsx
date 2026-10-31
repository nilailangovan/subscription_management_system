import React, { useState } from 'react';
import { Wallet, HandCoins, AlertTriangle, Repeat, Search } from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import Card from '../../components/common/Card';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import { useToast } from '../../components/common/Toast';
import { mockInvoices, mockPayments, mockSubscriptions } from '../../data/mockData';
import { formatMoney } from '../../utils/format';
import { todayISO, isWithinRange, getInvoiceState, daysBetween, sumAmounts } from '../../utils/finance';

const PAGE_SIZE = 5;

const SUBSCRIPTION_STATUSES = [
  { value: 'ALL', label: 'All' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'PAUSED', label: 'Paused' },
  { value: 'CLOSED', label: 'Closed' },
];
const INVOICE_STATUSES = [
  { value: 'ALL', label: 'All' },
  { value: 'PAID', label: 'Paid' },
  { value: 'OUTSTANDING', label: 'Outstanding' },
  { value: 'OVERDUE', label: 'Overdue' },
];
const INVOICE_TABLE_TITLES = {
  ALL: 'All Invoices',
  PAID: 'Paid Invoices',
  OUTSTANDING: 'Outstanding Invoices',
  OVERDUE: 'Overdue Invoices',
};
const PERIOD_UNIT = { DAILY: 'day', WEEKLY: 'week', MONTHLY: 'month', YEARLY: 'year' };

const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`;

/** Case-insensitive match of the (already lower-cased) search text against any of the fields. */
const matchesSearch = (search, ...fields) => !search || fields.some((field) => field?.toLowerCase().includes(search));

/** Client-side paging for the summary tables; clamps the page when a filter shrinks the list. */
const paginate = (rows, page) => {
  const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const current = Math.min(page, totalPages);
  return { current, totalPages, rows: rows.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE) };
};

/** Two-line customer cell: name and email. */
const CustomerCell = ({ name, email }) => (
  <div>
    <span className="font-semibold">{name}</span>
    <span className="block text-[11px] text-neutral-500">{email}</span>
  </div>
);

/**
 * Admin & Staff console home: global filter bar, financial KPI cards,
 * the invoice (overdue by default) table and recent subscription activity.
 */
const AdminDashboard = () => {
  const { toast, showToast } = useToast();
  const today = todayISO();

  // Date range defaults to the current month so far
  const [filters, setFilters] = useState({
    from: `${today.slice(0, 8)}01`,
    to: today,
    subscriptionStatus: 'ALL',
    invoiceStatus: 'OVERDUE',
    search: '',
  });
  // shortcut: invoices and payments are local state seeded from mock data; load them from the API once the backend has invoice/payment endpoints
  const [invoices, setInvoices] = useState(mockInvoices);
  const [payments, setPayments] = useState(mockPayments);
  const [invoicePage, setInvoicePage] = useState(1);
  const [subscriptionPage, setSubscriptionPage] = useState(1);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const search = filters.search.trim().toLowerCase();
  const rangeError = filters.from && filters.to && filters.from > filters.to ? 'Must be on or after the From date' : '';
  const rangeLabel = `${filters.from || 'any date'} → ${filters.to || 'any date'}`;

  // Revenue follows the date range; receivables are balances as of today, whatever the range
  const collected = payments.filter(
    (p) =>
      isWithinRange(p.paidAt, filters.from, filters.to) &&
      matchesSearch(search, p.customerName, p.customerEmail, p.invoiceNumber)
  );
  const searchedInvoices = invoices
    .map((invoice) => ({ ...invoice, state: getInvoiceState(invoice, today) }))
    .filter((invoice) => matchesSearch(search, invoice.customerName, invoice.customerEmail, invoice.invoiceNumber));
  const unpaid = searchedInvoices.filter((invoice) => invoice.state === 'OUTSTANDING' || invoice.state === 'OVERDUE');
  const overdue = unpaid.filter((invoice) => invoice.state === 'OVERDUE');

  const searchedSubscriptions = mockSubscriptions.filter((s) =>
    matchesSearch(search, s.customerName, s.customerEmail, s.id)
  );
  const activeCount = searchedSubscriptions.filter((s) => s.status === 'ACTIVE').length;
  const pausedCount = searchedSubscriptions.filter((s) => s.status === 'PAUSED').length;

  // Oldest due date first, so the most overdue invoices lead
  const invoiceRows = searchedInvoices
    .filter((invoice) => filters.invoiceStatus === 'ALL' || invoice.state === filters.invoiceStatus)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  const subscriptionRows = searchedSubscriptions
    .filter(
      (s) =>
        (filters.subscriptionStatus === 'ALL' || s.status === filters.subscriptionStatus) &&
        isWithinRange(s.updatedAt, filters.from, filters.to)
    )
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));

  const invoicePageData = paginate(invoiceRows, invoicePage);
  const subscriptionPageData = paginate(subscriptionRows, subscriptionPage);

  const recordPayment = (invoice) => {
    if (
      !window.confirm(
        `Record a payment of ${formatMoney(invoice.amount)} from ${invoice.customerName} for ${invoice.invoiceNumber}?`
      )
    )
      return;

    setInvoices((prev) => prev.map((i) => (i.id === invoice.id ? { ...i, status: 'PAID' } : i)));
    setPayments((prev) => [
      {
        id: `PAY-${Date.now()}`,
        invoiceNumber: invoice.invoiceNumber,
        customerName: invoice.customerName,
        customerEmail: invoice.customerEmail,
        amount: invoice.amount,
        method: 'Manual entry',
        paidAt: today,
      },
      ...prev,
    ]);
    showToast(`Payment of ${formatMoney(invoice.amount)} recorded for ${invoice.invoiceNumber}.`);
  };

  const invoiceColumns = [
    {
      header: 'Invoice #',
      render: (i) => (
        <div className="whitespace-nowrap">
          <span className="font-semibold">{i.invoiceNumber}</span>
          <span className="block text-[11px] text-neutral-500">{i.subscriptionId}</span>
        </div>
      ),
    },
    { header: 'Customer', render: (i) => <CustomerCell name={i.customerName} email={i.customerEmail} /> },
    { header: 'Amount Due', render: (i) => <span className="font-semibold">{formatMoney(i.amount)}</span>, className: 'text-right' },
    { header: 'Due Date', accessor: 'dueDate', className: 'whitespace-nowrap' },
    {
      header: 'Days Overdue',
      className: 'whitespace-nowrap',
      render: (i) => {
        if (i.state === 'OVERDUE') {
          return <span className="font-bold text-status-danger">{plural(daysBetween(i.dueDate, today), 'day')}</span>;
        }
        if (i.state === 'OUTSTANDING') {
          const dueIn = daysBetween(today, i.dueDate);
          return <span className="text-neutral-500">{dueIn === 0 ? 'Due today' : `Due in ${plural(dueIn, 'day')}`}</span>;
        }
        return <Badge status={i.state} size="sm" />;
      },
    },
    {
      header: 'Action',
      className: 'text-right',
      render: (i) =>
        i.state === 'OUTSTANDING' || i.state === 'OVERDUE' ? (
          <Button variant="outline" size="sm" icon={HandCoins} onClick={() => recordPayment(i)} className="whitespace-nowrap">
            Record Payment
          </Button>
        ) : (
          <span className="text-xs text-neutral-400">{i.state === 'DRAFT' ? 'Not sent' : 'Settled'}</span>
        ),
    },
  ];

  const subscriptionColumns = [
    { header: 'Subscription ID', accessor: 'id', className: 'font-semibold whitespace-nowrap' },
    { header: 'Customer', render: (s) => <CustomerCell name={s.customerName} email={s.customerEmail} /> },
    { header: 'Plan', accessor: 'planName' },
    {
      header: 'Recurring Value',
      className: 'text-right whitespace-nowrap',
      render: (s) => (
        <span>
          {formatMoney(s.recurringValue)}
          <span className="text-[11px] text-neutral-500"> / {PERIOD_UNIT[s.billingPeriod]}</span>
        </span>
      ),
    },
    { header: 'Next Billing Date', render: (s) => s.nextBillingDate || '—', className: 'whitespace-nowrap' },
    {
      header: 'Status',
      // PAUSED has no default Badge colour, so give it the warning style
      render: (s) => <Badge status={s.status} variant={s.status === 'PAUSED' ? 'warning' : undefined} size="sm" />,
    },
  ];

  return (
    <div className="space-y-6">
      {/* No visible page header; keep a heading for screen readers */}
      <h2 className="sr-only">Console Overview</h2>

      {/* Global filter bar */}
      <div
        role="search"
        aria-label="Dashboard filters"
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3 bg-white p-4 rounded-2xl shadow-soft border border-silver-border"
      >
        <Input
          label="From"
          name="from"
          type="date"
          value={filters.from}
          max={filters.to || undefined}
          onChange={handleFilterChange}
        />
        <Input
          label="To"
          name="to"
          type="date"
          value={filters.to}
          min={filters.from || undefined}
          onChange={handleFilterChange}
          error={rangeError}
        />
        <Select
          label="Subscription Status"
          name="subscriptionStatus"
          value={filters.subscriptionStatus}
          onChange={handleFilterChange}
          options={SUBSCRIPTION_STATUSES}
        />
        <Select
          label="Invoice Status"
          name="invoiceStatus"
          value={filters.invoiceStatus}
          onChange={handleFilterChange}
          options={INVOICE_STATUSES}
        />
        <Input
          label="Search"
          name="search"
          placeholder="Customer, email or invoice #"
          value={filters.search}
          onChange={handleFilterChange}
          icon={Search}
        />
      </div>

      {/* Financial KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Revenue Collected"
          value={formatMoney(sumAmounts(collected))}
          icon={Wallet}
          helperText={`${plural(collected.length, 'payment')} received · ${rangeLabel}`}
        />
        <StatCard
          title="Outstanding Receivables"
          value={formatMoney(sumAmounts(unpaid))}
          icon={HandCoins}
          helperText={`Owed across ${plural(unpaid.length, 'unpaid invoice')} · as of today`}
        />
        <StatCard
          title="Overdue Invoices"
          value={formatMoney(sumAmounts(overdue))}
          icon={AlertTriangle}
          tone={overdue.length > 0 ? 'danger' : 'default'}
          helperText={overdue.length > 0 ? `${plural(overdue.length, 'invoice')} past due date` : 'Nothing past due'}
        />
        <StatCard
          title="Active Subscriptions"
          value={activeCount}
          icon={Repeat}
          helperText={`${pausedCount} paused`}
        />
      </div>

      {/* Invoices: Overdue by default, follows the Invoice Status filter */}
      <Card
        title={INVOICE_TABLE_TITLES[filters.invoiceStatus]}
        subtitle="Oldest due date first · balances as of today"
        bodyClassName="p-6 pt-4"
      >
        <Table
          columns={invoiceColumns}
          data={invoicePageData.rows}
          page={invoicePageData.current}
          totalPages={invoicePageData.totalPages}
          totalItems={invoiceRows.length}
          pageSize={PAGE_SIZE}
          onPageChange={setInvoicePage}
          emptyMessage="No invoices match these filters."
        />
      </Card>

      <Card
        title="Recent Subscription Activity"
        subtitle={`Subscriptions changed · ${rangeLabel}`}
        bodyClassName="p-6 pt-4"
      >
        <Table
          columns={subscriptionColumns}
          data={subscriptionPageData.rows}
          page={subscriptionPageData.current}
          totalPages={subscriptionPageData.totalPages}
          totalItems={subscriptionRows.length}
          pageSize={PAGE_SIZE}
          onPageChange={setSubscriptionPage}
          emptyMessage="No subscription activity matches these filters."
        />
      </Card>

      {toast}
    </div>
  );
};

export default AdminDashboard;
