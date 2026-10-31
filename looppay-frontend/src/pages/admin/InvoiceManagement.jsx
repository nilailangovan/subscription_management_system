import React, { useState } from 'react';
import { FileText, Download, CheckCircle2, Clock, AlertCircle, DollarSign, Filter, Search } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Table from '../../components/common/Table';

const InvoiceManagement = () => {
  const [activeTab, setActiveTab] = useState('ALL');
  const [search, setSearch] = useState('');

  // Sample and confirmed invoices linked to active subscriptions
  const [invoices, setInvoices] = useState([
    {
      id: 1,
      invoiceNumber: 'INV-2026-001',
      subscriptionNumber: 'SUB-0001',
      customerName: 'Alice Customer',
      customerEmail: 'alice@example.com',
      issueDate: '2026-10-31',
      dueDate: '2026-11-30',
      status: 'CONFIRMED',
      amount: 1500.00
    },
    {
      id: 2,
      invoiceNumber: 'INV-2026-002',
      subscriptionNumber: 'SUB-0002',
      customerName: 'Jane Doe',
      customerEmail: 'jane.doe@example.com',
      issueDate: '2026-10-15',
      dueDate: '2026-11-15',
      status: 'PAID',
      amount: 2400.00
    },
    {
      id: 3,
      invoiceNumber: 'INV-2026-003',
      subscriptionNumber: 'SUB-0003',
      customerName: 'Mark Staff Partner',
      customerEmail: 'mark.staff@looppay.com',
      issueDate: '2026-10-28',
      dueDate: '2026-11-28',
      status: 'DRAFT',
      amount: 750.00
    }
  ]);

  const handleMarkPaid = (id) => {
    setInvoices((prev) =>
      prev.map((inv) => (inv.id === id ? { ...inv, status: 'PAID' } : inv))
    );
  };

  const filteredInvoices = invoices.filter((inv) => {
    const matchesTab = activeTab === 'ALL' || inv.status === activeTab;
    const matchesSearch =
      inv.invoiceNumber.toLowerCase().includes(search.toLowerCase()) ||
      inv.subscriptionNumber.toLowerCase().includes(search.toLowerCase()) ||
      inv.customerName.toLowerCase().includes(search.toLowerCase());
    return matchesTab && matchesSearch;
  });

  const columns = [
    {
      header: 'Invoice #',
      render: (row) => (
        <span className="font-mono font-bold text-aubergine text-xs bg-silver-light px-2 py-1 rounded-md border border-silver-border">
          {row.invoiceNumber}
        </span>
      )
    },
    {
      header: 'Subscription Ref',
      render: (row) => (
        <span className="font-mono text-xs text-neutral-600 font-semibold">{row.subscriptionNumber}</span>
      )
    },
    {
      header: 'Customer',
      render: (row) => (
        <div>
          <p className="font-semibold text-aubergine">{row.customerName}</p>
          <p className="text-[11px] text-neutral-500">{row.customerEmail}</p>
        </div>
      )
    },
    {
      header: 'Due Date',
      render: (row) => <span className="text-xs text-neutral-600">{row.dueDate}</span>
    },
    {
      header: 'Status',
      render: (row) => <Badge status={row.status} />
    },
    {
      header: 'Amount',
      render: (row) => (
        <span className="font-semibold text-xs text-aubergine">
          ${row.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}
        </span>
      )
    },
    {
      header: 'Actions',
      className: 'text-right',
      render: (row) => (
        <div className="flex items-center justify-end gap-2">
          {row.status !== 'PAID' && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => handleMarkPaid(row.id)}
              className="text-[11px] py-1 px-2.5 h-auto text-status-success hover:border-status-success"
            >
              <CheckCircle2 className="w-3 h-3 mr-1" />
              Mark Paid
            </Button>
          )}
          <Button
            size="sm"
            variant="outline"
            onClick={() => alert(`Downloading ${row.invoiceNumber}...`)}
            className="text-[11px] py-1 px-2 h-auto text-neutral-600"
            title="Download PDF"
          >
            <Download className="w-3 h-3" />
          </Button>
        </div>
      )
    }
  ];

  const totalInvoiced = invoices.reduce((acc, curr) => acc + curr.amount, 0);
  const totalPaid = invoices.filter((i) => i.status === 'PAID').reduce((acc, curr) => acc + curr.amount, 0);
  const totalPending = invoices.filter((i) => i.status !== 'PAID').reduce((acc, curr) => acc + curr.amount, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <div className="flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-xl bg-aubergine flex items-center justify-center text-marigold shadow-inner">
            <FileText className="w-5 h-5 text-marigold" />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-aubergine tracking-tight">Invoice & Settlement Hub</h2>
            <p className="text-xs text-neutral-500">
              Track recurring subscription billings, payment settlement, and invoice records
            </p>
          </div>
        </div>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-soft border border-silver-border">
          <div className="flex justify-between items-center text-neutral-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Invoiced</span>
            <DollarSign className="w-4 h-4 text-aubergine" />
          </div>
          <span className="text-2xl font-bold text-aubergine">
            ${totalInvoiced.toLocaleString('en-US', { minimumFractionDigits: 2 })}
          </span>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-soft border border-silver-border">
          <div className="flex justify-between items-center text-neutral-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Paid</span>
            <CheckCircle2 className="w-4 h-4 text-status-success" />
          </div>
          <span className="text-2xl font-bold text-status-success">
            ${totalPaid.toLocaleString('en-US', { minimumFractionDigits: 2 })}
          </span>
        </div>

        <div className="bg-white p-5 rounded-2xl shadow-soft border border-silver-border">
          <div className="flex justify-between items-center text-neutral-500 mb-1">
            <span className="text-xs font-semibold uppercase tracking-wider">Pending Settlement</span>
            <Clock className="w-4 h-4 text-marigold" />
          </div>
          <span className="text-2xl font-bold text-aubergine">
            ${totalPending.toLocaleString('en-US', { minimumFractionDigits: 2 })}
          </span>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {['ALL', 'CONFIRMED', 'PAID', 'DRAFT'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold tracking-wide transition duration-150 ${
                activeTab === tab
                  ? 'bg-aubergine text-white shadow-soft'
                  : 'bg-white text-neutral-600 hover:bg-silver-light border border-silver-border'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="relative">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search invoice or customer..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 pr-4 py-2 bg-white rounded-xl border border-silver-border text-xs text-aubergine focus:outline-none focus:ring-2 focus:ring-marigold w-64"
          />
        </div>
      </div>

      {/* Table */}
      <Table
        columns={columns}
        data={filteredInvoices}
        page={1}
        totalPages={1}
        totalItems={filteredInvoices.length}
        emptyMessage="No invoices found matching criteria."
      />
    </div>
  );
};

export default InvoiceManagement;

