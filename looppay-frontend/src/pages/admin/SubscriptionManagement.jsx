import React, { useState, useEffect } from 'react';
import { 
  Repeat, Plus, Search, Eye, ArrowRight, CheckCircle2, 
  Send, XCircle, FileText, Calendar, DollarSign, Layers, AlertCircle 
} from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Table from '../../components/common/Table';
import api from '../../api/axios';

const STATUS_TABS = ['ALL', 'DRAFT', 'QUOTATION', 'CONFIRMED', 'ACTIVE', 'CLOSED'];

const SubscriptionManagement = () => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);

  // Notifications
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // View Details Modal
  const [selectedSub, setSelectedSub] = useState(null);
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  // Create Modal
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  const [formData, setFormData] = useState({
    customerId: '',
    planId: '',
    startDate: new Date().toISOString().split('T')[0],
    expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    paymentTerms: 'Immediate Payment',
    lines: [{ productId: '', quantity: 1, unitPrice: '' }]
  });

  const fetchSubscriptions = async () => {
    setLoading(true);
    setError('');
    try {
      const params = {
        page: page - 1,
        size: 10,
        sort: 'id,desc'
      };
      if (activeTab !== 'ALL') {
        params.status = activeTab;
      }
      if (search.trim()) {
        params.search = search.trim();
      }

      const res = await api.get('/api/subscriptions', { params });
      if (res.data?.data) {
        setSubscriptions(res.data.data.content || []);
        setTotalPages(res.data.data.totalPages || 1);
        setTotalItems(res.data.data.totalElements || 0);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load subscriptions.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions();
  }, [page, activeTab]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(1);
    fetchSubscriptions();
  };

  // Status transitions
  const handleTransition = async (subId, nextStatus) => {
    setError('');
    setSuccess('');
    try {
      const res = await api.patch(`/api/subscriptions/${subId}/status`, { status: nextStatus });
      setSuccess(`Subscription transitioned to ${nextStatus} successfully.`);
      fetchSubscriptions();
      if (selectedSub && selectedSub.id === subId) {
        setSelectedSub(res.data.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to transition status to ${nextStatus}.`);
    }
  };

  // Line items in Create Form
  const addLine = () => {
    setFormData((prev) => ({
      ...prev,
      lines: [...prev.lines, { productId: '', quantity: 1, unitPrice: '' }]
    }));
  };

  const removeLine = (index) => {
    if (formData.lines.length <= 1) return;
    setFormData((prev) => ({
      ...prev,
      lines: prev.lines.filter((_, i) => i !== index)
    }));
  };

  const updateLine = (index, field, value) => {
    setFormData((prev) => {
      const updated = [...prev.lines];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, lines: updated };
    });
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setFormErrors({});
    setError('');

    const errors = {};
    if (!formData.customerId) errors.customerId = 'Customer ID is required.';
    if (!formData.planId) errors.planId = 'Plan ID is required.';
    if (!formData.startDate) errors.startDate = 'Start date is required.';
    if (!formData.expiryDate) errors.expiryDate = 'Expiry date is required.';
    if (new Date(formData.expiryDate) <= new Date(formData.startDate)) {
      errors.expiryDate = 'Expiry date must be after start date.';
    }

    const cleanedLines = [];
    formData.lines.forEach((l, idx) => {
      if (!l.productId) errors[`line_${idx}_productId`] = 'Product ID is required.';
      if (!l.quantity || l.quantity < 1) errors[`line_${idx}_qty`] = 'Quantity >= 1 required.';
      cleanedLines.push({
        productId: Number(l.productId),
        quantity: Number(l.quantity),
        unitPrice: l.unitPrice ? Number(l.unitPrice) : null
      });
    });

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setCreateLoading(true);
    try {
      const payload = {
        customerId: Number(formData.customerId),
        planId: Number(formData.planId),
        startDate: formData.startDate,
        expiryDate: formData.expiryDate,
        paymentTerms: formData.paymentTerms,
        lines: cleanedLines
      };

      const res = await api.post('/api/subscriptions', payload);
      setSuccess(`Subscription ${res.data.data.subscriptionNumber} created successfully in DRAFT status!`);
      setIsCreateOpen(false);
      setFormData({
        customerId: '',
        planId: '',
        startDate: new Date().toISOString().split('T')[0],
        expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        paymentTerms: 'Immediate Payment',
        lines: [{ productId: '', quantity: 1, unitPrice: '' }]
      });
      fetchSubscriptions();
    } catch (err) {
      if (err.response?.data?.errors) {
        setFormErrors(err.response.data.errors);
      } else {
        setError(err.response?.data?.message || 'Failed to create subscription.');
      }
    } finally {
      setCreateLoading(false);
    }
  };

  const getNextTransition = (status) => {
    switch (status) {
      case 'DRAFT':
        return { label: 'Send Quote', target: 'QUOTATION', icon: Send, variant: 'warning' };
      case 'QUOTATION':
        return { label: 'Confirm', target: 'CONFIRMED', icon: CheckCircle2, variant: 'primary' };
      case 'CONFIRMED':
        return { label: 'Activate', target: 'ACTIVE', icon: ArrowRight, variant: 'primary' };
      case 'ACTIVE':
        return { label: 'Close', target: 'CLOSED', icon: XCircle, variant: 'danger' };
      default:
        return null;
    }
  };

  const columns = [
    {
      header: 'Sub Number',
      render: (row) => (
        <span className="font-mono font-bold text-aubergine text-xs bg-silver-light px-2 py-1 rounded-md border border-silver-border">
          {row.subscriptionNumber}
        </span>
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
      header: 'Plan',
      render: (row) => (
        <span className="text-xs font-medium text-aubergine">{row.planName}</span>
      )
    },
    {
      header: 'Validity Period',
      render: (row) => (
        <div className="text-xs text-neutral-600">
          <span>{row.startDate}</span> &rarr; <span>{row.expiryDate}</span>
        </div>
      )
    },
    {
      header: 'Status',
      render: (row) => <Badge status={row.status} />
    },
    {
      header: 'Grand Total',
      render: (row) => (
        <span className="font-semibold text-xs text-aubergine">
          ${Number(row.grandTotal).toLocaleString('en-US', { minimumFractionDigits: 2 })}
        </span>
      )
    },
    {
      header: 'Actions',
      className: 'text-right',
      render: (row) => {
        const next = getNextTransition(row.status);
        return (
          <div className="flex items-center justify-end gap-2">
            {next && (
              <Button
                size="sm"
                variant={next.variant}
                onClick={() => handleTransition(row.id, next.target)}
                className="text-[11px] py-1 px-2.5 h-auto"
              >
                <next.icon className="w-3 h-3 mr-1" />
                {next.label}
              </Button>
            )}
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                setSelectedSub(row);
                setIsDetailsOpen(true);
              }}
              className="text-[11px] py-1 px-2 h-auto text-neutral-600"
            >
              <Eye className="w-3 h-3" />
            </Button>
          </div>
        );
      }
    }
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-aubergine flex items-center justify-center text-marigold shadow-inner">
              <Repeat className="w-5 h-5 text-marigold" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-aubergine tracking-tight">Subscriptions Management</h2>
              <p className="text-xs text-neutral-500">
                Oversee customer subscriptions, contract lifecycles, and automated recurring billing
              </p>
            </div>
          </div>
        </div>

        <Button onClick={() => setIsCreateOpen(true)} className="flex items-center gap-2">
          <Plus className="w-4 h-4" />
          <span>New Subscription</span>
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <div className="p-4 rounded-xl bg-status-danger-bg border border-status-danger/30 text-status-danger text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}
      {success && (
        <div className="p-4 rounded-xl bg-status-success-bg border border-status-success/30 text-status-success text-sm flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
          <span>{success}</span>
        </div>
      )}

      {/* Status Filter Tabs & Search */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Status Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => {
                setActiveTab(tab);
                setPage(1);
              }}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold tracking-wide transition duration-150 whitespace-nowrap ${
                activeTab === tab
                  ? 'bg-aubergine text-white shadow-soft'
                  : 'bg-white text-neutral-600 hover:bg-silver-light border border-silver-border'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} className="flex items-center gap-2">
          <div className="relative">
            <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search SUB-0001..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 pr-4 py-2 bg-white rounded-xl border border-silver-border text-xs text-aubergine focus:outline-none focus:ring-2 focus:ring-marigold w-52 sm:w-64"
            />
          </div>
          <Button type="submit" variant="outline" size="sm">
            Search
          </Button>
        </form>
      </div>

      {/* Data Table */}
      <Table
        columns={columns}
        data={subscriptions}
        page={page}
        totalPages={totalPages}
        totalItems={totalItems}
        onPageChange={(p) => setPage(p)}
        loading={loading}
        emptyMessage={`No ${activeTab !== 'ALL' ? activeTab : ''} subscriptions found.`}
      />

      {/* ========================================================================= */}
      {/* VIEW DETAILS MODAL */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isDetailsOpen}
        onClose={() => setIsDetailsOpen(false)}
        title={selectedSub ? `Subscription Details: ${selectedSub.subscriptionNumber}` : 'Subscription Details'}
        maxWidth="max-w-2xl"
      >
        {selectedSub && (
          <div className="space-y-6">
            {/* Summary Top Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border">
                <span className="text-[10px] text-neutral-500 uppercase font-semibold block">Status</span>
                <Badge status={selectedSub.status} size="sm" className="mt-1" />
              </div>
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border">
                <span className="text-[10px] text-neutral-500 uppercase font-semibold block">Grand Total</span>
                <span className="text-sm font-bold text-aubergine block mt-1">
                  ${Number(selectedSub.grandTotal).toFixed(2)}
                </span>
              </div>
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border">
                <span className="text-[10px] text-neutral-500 uppercase font-semibold block">Start Date</span>
                <span className="text-xs font-semibold text-neutral-700 block mt-1">{selectedSub.startDate}</span>
              </div>
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border">
                <span className="text-[10px] text-neutral-500 uppercase font-semibold block">Expiry Date</span>
                <span className="text-xs font-semibold text-neutral-700 block mt-1">{selectedSub.expiryDate}</span>
              </div>
            </div>

            {/* Customer & Plan Info */}
            <div className="p-4 bg-white rounded-xl border border-silver-border space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-neutral-500">Customer:</span>
                <span className="font-semibold text-aubergine">{selectedSub.customerName} ({selectedSub.customerEmail})</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Plan:</span>
                <span className="font-semibold text-aubergine">{selectedSub.planName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-neutral-500">Payment Terms:</span>
                <span className="font-medium text-neutral-700">{selectedSub.paymentTerms}</span>
              </div>
            </div>

            {/* Line Items Table */}
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-500 mb-2">Order Line Items</h4>
              <div className="rounded-xl border border-silver-border overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-silver-light text-neutral-600 font-semibold border-b border-silver-border">
                    <tr>
                      <th className="p-2.5">Product</th>
                      <th className="p-2.5 text-center">Qty</th>
                      <th className="p-2.5 text-right">Unit Price</th>
                      <th className="p-2.5 text-right">Tax</th>
                      <th className="p-2.5 text-right">Line Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-silver-border">
                    {selectedSub.lines?.map((line, idx) => (
                      <tr key={idx} className="hover:bg-silver-light/40">
                        <td className="p-2.5 font-medium text-aubergine">{line.productName}</td>
                        <td className="p-2.5 text-center">{line.quantity}</td>
                        <td className="p-2.5 text-right">${Number(line.unitPrice).toFixed(2)}</td>
                        <td className="p-2.5 text-right">${Number(line.tax).toFixed(2)}</td>
                        <td className="p-2.5 text-right font-bold text-aubergine">${Number(line.amount).toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Pricing Totals */}
            <div className="p-4 bg-silver-light rounded-xl space-y-1.5 text-xs">
              <div className="flex justify-between text-neutral-600">
                <span>Subtotal:</span>
                <span className="font-semibold">${Number(selectedSub.subtotal).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-neutral-600">
                <span>Total Tax:</span>
                <span className="font-semibold">${Number(selectedSub.totalTax).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-neutral-600">
                <span>Total Discount:</span>
                <span className="font-semibold">-${Number(selectedSub.totalDiscount).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm font-bold text-aubergine pt-2 border-t border-silver-border">
                <span>Grand Total:</span>
                <span>${Number(selectedSub.grandTotal).toFixed(2)}</span>
              </div>
            </div>

            {/* Transition Action in Modal */}
            {getNextTransition(selectedSub.status) && (
              <div className="flex justify-end pt-2">
                <Button
                  variant={getNextTransition(selectedSub.status).variant}
                  onClick={() => handleTransition(selectedSub.id, getNextTransition(selectedSub.status).target)}
                >
                  Advance to {getNextTransition(selectedSub.status).target}
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>

      {/* ========================================================================= */}
      {/* CREATE SUBSCRIPTION MODAL */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Create New Subscription"
        maxWidth="max-w-xl"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4 text-xs">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Customer ID"
              type="text"
              inputMode="numeric"
              placeholder="e.g. 2 (Jane Doe) or 1"
              value={formData.customerId}
              onChange={(e) => setFormData({ ...formData, customerId: e.target.value.replace(/[^0-9]/g, '') })}
              helperText="Enter user ID (e.g. 2 for Jane Doe, 1 for Admin)"
              error={formErrors.customerId}
              required
            />
            <Input
              label="Plan ID"
              type="text"
              inputMode="numeric"
              placeholder="e.g. 1 (Enterprise) or 2 (Monthly)"
              value={formData.planId}
              onChange={(e) => setFormData({ ...formData, planId: e.target.value.replace(/[^0-9]/g, '') })}
              helperText="Enter plan ID (e.g. 1 for Enterprise, 2 for Monthly)"
              error={formErrors.planId}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Start Date"
              type="date"
              value={formData.startDate}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              error={formErrors.startDate}
              required
            />
            <Input
              label="Expiry Date"
              type="date"
              value={formData.expiryDate}
              onChange={(e) => setFormData({ ...formData, expiryDate: e.target.value })}
              error={formErrors.expiryDate}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Payment Terms <span className="text-status-danger">*</span>
            </label>
            <select
              value={formData.paymentTerms}
              onChange={(e) => setFormData({ ...formData, paymentTerms: e.target.value })}
              className="w-full px-3 py-2 bg-white rounded-xl border border-silver-border text-xs text-aubergine focus:outline-none focus:ring-2 focus:ring-marigold"
            >
              <option value="Immediate Payment">Immediate Payment</option>
              <option value="Net 15">Net 15</option>
              <option value="Net 30">Net 30</option>
              <option value="Net 60">Net 60</option>
            </select>
          </div>

          {/* Line Items Section */}
          <div className="space-y-2 pt-2 border-t border-silver-border">
            <div className="flex justify-between items-center">
              <div>
                <span className="font-bold text-aubergine uppercase text-[11px] tracking-wide">Product Lines</span>
                <p className="text-[10px] text-neutral-500 mt-0.5">
                  The specific products, software licenses, or services included in this contract
                </p>
              </div>
              <Button type="button" size="sm" variant="outline" onClick={addLine}>
                <Plus className="w-3 h-3 mr-1" /> Add Line
              </Button>
            </div>

            {formData.lines.map((line, idx) => (
              <div key={idx} className="flex items-center gap-2 bg-silver-light p-2.5 rounded-xl border border-silver-border">
                <div className="flex-1">
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="Product ID (e.g. 1)"
                    value={line.productId}
                    onChange={(e) => updateLine(idx, 'productId', e.target.value.replace(/[^0-9]/g, ''))}
                    className="w-full px-2.5 py-1.5 bg-white rounded-lg border border-silver-border text-xs text-aubergine focus:outline-none focus:ring-1 focus:ring-marigold"
                    required
                  />
                  {formErrors[`line_${idx}_productId`] && (
                    <span className="text-[10px] text-status-danger">{formErrors[`line_${idx}_productId`]}</span>
                  )}
                </div>

                <div className="w-20">
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="Qty"
                    value={line.quantity}
                    onChange={(e) => updateLine(idx, 'quantity', e.target.value.replace(/[^0-9]/g, ''))}
                    className="w-full px-2.5 py-1.5 bg-white rounded-lg border border-silver-border text-xs text-aubergine text-center focus:outline-none focus:ring-1 focus:ring-marigold"
                    required
                  />
                </div>

                <div className="w-28">
                  <input
                    type="number"
                    step="0.01"
                    placeholder="Price (Opt)"
                    value={line.unitPrice}
                    onChange={(e) => updateLine(idx, 'unitPrice', e.target.value)}
                    className="w-full px-2.5 py-1.5 bg-white rounded-lg border border-silver-border text-xs text-aubergine text-right focus:outline-none focus:ring-1 focus:ring-marigold"
                  />
                </div>

                {formData.lines.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeLine(idx)}
                    className="text-status-danger hover:text-red-700 p-1"
                    title="Remove item"
                  >
                    <XCircle className="w-4 h-4" />
                  </button>
                )}
              </div>
            ))}
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-silver-border">
            <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={createLoading}>
              Create Subscription
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default SubscriptionManagement;

