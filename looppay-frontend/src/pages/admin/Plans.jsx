import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Search, Pause, Play, RefreshCw, XCircle } from 'lucide-react';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Modal from '../../components/common/Modal';
import { useToast, apiErrorMessage } from '../../components/common/Toast';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { formatMoney } from '../../utils/format';

const PAGE_SIZE = 10;
const BILLING_PERIODS = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'].map((p) => ({ value: p, label: p }));
// Plan statuses use their own colours (CLOSED is neutral here, unlike subscriptions)
const PLAN_STATUS_VARIANT = { ACTIVE: 'success', PAUSED: 'warning', CLOSED: 'neutral' };
const EMPTY_FORM = {
  name: '',
  productId: '',
  price: '',
  billingPeriod: 'MONTHLY',
  minQuantity: '1',
  startDate: '',
  endDate: '',
  autoClose: false,
  closable: true,
  pausable: true,
  renewable: true,
};
const FLAGS = [
  { name: 'autoClose', label: 'Auto close', description: 'Close automatically after the end date' },
  { name: 'closable', label: 'Closable', description: 'Admins can close this plan' },
  { name: 'pausable', label: 'Pausable', description: 'Admins can pause this plan' },
  { name: 'renewable', label: 'Renewable', description: 'Admins can extend the end date' },
];

/**
 * Returns whether each row action is allowed for a plan, with the reason when it is not.
 * Mirrors the rules enforced in PlanService.
 */
const getActionRules = (plan) => ({
  pause: {
    allowed: plan.pausable && plan.status === 'ACTIVE',
    reason: !plan.pausable ? 'This plan is not pausable' : 'Only ACTIVE plans can be paused',
  },
  resume: { allowed: plan.status === 'PAUSED', reason: 'Only PAUSED plans can be resumed' },
  renew: { allowed: plan.renewable, reason: 'This plan is not renewable' },
  close: {
    allowed: plan.closable && plan.status !== 'CLOSED',
    reason: !plan.closable ? 'This plan is not closable' : 'This plan is already closed',
  },
});

const ACTIONS = [
  { key: 'pause', label: 'Pause', icon: Pause },
  { key: 'resume', label: 'Resume', icon: Play },
  { key: 'renew', label: 'Renew', icon: RefreshCw },
  { key: 'close', label: 'Close', icon: XCircle },
];

/** Accessible on/off switch for the plan flags. */
const Toggle = ({ label, description, checked, onChange }) => (
  <label className="flex items-center justify-between gap-3 p-3 rounded-xl border border-silver-border bg-silver-light/40 cursor-pointer">
    <span>
      <span className="block text-sm font-semibold text-aubergine">{label}</span>
      <span className="block text-[11px] text-neutral-500">{description}</span>
    </span>
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={`relative flex-shrink-0 w-10 h-6 rounded-full transition focus:outline-none focus:ring-2 focus:ring-aubergine focus:ring-offset-2 ${
        checked ? 'bg-aubergine' : 'bg-silver-border'
      }`}
    >
      <span
        className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform ${
          checked ? 'translate-x-4' : ''
        }`}
      />
    </button>
  </label>
);

/**
 * Subscription plans: paginated list with search and lifecycle actions.
 * ADMIN can add plans and run Close / Pause / Resume / Renew; STAFF is view-only.
 */
const Plans = () => {
  const { role } = useAuth();
  const isAdmin = role === 'ADMIN';
  const { toast, showToast } = useToast();

  // List state (page is 1-based for the Table, the backend is 0-based)
  const [plans, setPlans] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [busyAction, setBusyAction] = useState(null); // `${planId}-${action}` while a request runs

  // Add Plan modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formErrors, setFormErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [productOptions, setProductOptions] = useState([]);

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/plans', {
        params: { page: page - 1, size: PAGE_SIZE, search: search || undefined },
      });
      const result = response.data.data;
      setPlans(result.content);
      setTotalPages(result.totalPages);
      setTotalItems(result.totalElements);
    } catch (err) {
      showToast(apiErrorMessage(err, 'Failed to load plans.'), 'error');
    } finally {
      setLoading(false);
    }
  }, [page, search, showToast]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(1);
    setSearch(searchInput.trim());
  };

  const openCreate = async () => {
    setForm(EMPTY_FORM);
    setFormErrors({});
    setIsModalOpen(true);

    try {
      // shortcut: loads the first 100 products only; switch to a searchable picker if the catalogue grows past that
      const response = await api.get('/api/products', { params: { page: 0, size: 100, sort: 'name,asc' } });
      setProductOptions(response.data.data.content.map((p) => ({ value: p.id, label: p.name })));
    } catch (err) {
      showToast(apiErrorMessage(err, 'Failed to load products.'), 'error');
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) setFormErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const handleCreate = async (e) => {
    e.preventDefault();

    const errors = {};
    if (!form.name.trim()) errors.name = 'Plan name is required.';
    if (form.price === '' || Number(form.price) < 0) errors.price = 'Enter a price of 0 or more.';
    if (!(Number(form.minQuantity) >= 1)) errors.minQuantity = 'Minimum quantity must be at least 1.';
    if (!form.startDate) errors.startDate = 'Start date is required.';
    if (!form.endDate) errors.endDate = 'End date is required.';
    // ISO date strings (yyyy-mm-dd) compare correctly as text
    else if (form.startDate && form.endDate <= form.startDate) errors.endDate = 'End date must be after the start date.';
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    const payload = {
      ...form,
      name: form.name.trim(),
      productId: form.productId ? Number(form.productId) : null,
      price: Number(form.price),
      minQuantity: Number(form.minQuantity),
    };

    setSaving(true);
    try {
      const response = await api.post('/api/plans', payload);
      showToast(response.data.message);
      setIsModalOpen(false);
      fetchPlans();
    } catch (err) {
      if (err.response?.data?.errors) setFormErrors(err.response.data.errors);
      showToast(apiErrorMessage(err, 'Failed to create plan.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const runAction = async (plan, action) => {
    if (action === 'close' && !window.confirm(`Close plan "${plan.name}"? Customers will no longer see it.`)) return;

    setBusyAction(`${plan.id}-${action}`);
    try {
      // Renew without a body extends the end date by one billing cycle
      const response =
        action === 'renew'
          ? await api.post(`/api/plans/${plan.id}/renew`)
          : await api.patch(`/api/plans/${plan.id}/${action}`);
      const updated = response.data.data;
      setPlans((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      showToast(response.data.message);
    } catch (err) {
      showToast(apiErrorMessage(err, `Failed to ${action} plan.`), 'error');
    } finally {
      setBusyAction(null);
    }
  };

  const columns = [
    {
      header: 'Plan',
      render: (p) => (
        <div>
          <span className="font-semibold">{p.name}</span>
          <span className="block text-[11px] text-neutral-500">{p.productName || 'No product'}</span>
        </div>
      ),
    },
    {
      header: 'Price',
      render: (p) => (
        <span>
          {formatMoney(p.price)}
          <span className="text-[11px] text-neutral-500"> / {p.billingPeriod.toLowerCase()}</span>
        </span>
      ),
    },
    { header: 'Min Qty', accessor: 'minQuantity', className: 'text-center' },
    {
      header: 'Validity',
      render: (p) => (
        <span className="text-xs text-neutral-600 whitespace-nowrap">
          {p.startDate} &rarr; {p.endDate}
        </span>
      ),
    },
    {
      header: 'Status',
      render: (p) => (
        <Badge variant={PLAN_STATUS_VARIANT[p.status]} size="sm">
          {p.status}
        </Badge>
      ),
    },
    ...(isAdmin
      ? [
          {
            header: 'Actions',
            className: 'text-right',
            render: (p) => {
              const rules = getActionRules(p);
              return (
                <div className="flex items-center justify-end gap-1">
                  {ACTIONS.map(({ key, label, icon }) => (
                    <Button
                      key={key}
                      variant="ghost"
                      size="sm"
                      icon={icon}
                      disabled={!rules[key].allowed || busyAction !== null}
                      loading={busyAction === `${p.id}-${key}`}
                      title={rules[key].allowed ? `${label} plan` : rules[key].reason}
                      onClick={() => runAction(p, key)}
                    >
                      {label}
                    </Button>
                  ))}
                </div>
              );
            },
          },
        ]
      : []),
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <div>
          <h2 className="text-2xl font-bold text-aubergine tracking-tight">Plans</h2>
          <p className="text-xs text-neutral-500 mt-1">
            {isAdmin
              ? 'Create recurring plans and control their lifecycle.'
              : 'Read-only view of subscription plans.'}
          </p>
        </div>

        {isAdmin && (
          <Button variant="primary" icon={Plus} onClick={openCreate}>
            Add Plan
          </Button>
        )}
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 max-w-md">
        <Input
          name="search"
          placeholder="Search plans by name"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          icon={Search}
          aria-label="Search plans"
        />
        <Button variant="secondary" type="submit">
          Search
        </Button>
      </form>

      <Table
        columns={columns}
        data={plans}
        page={page}
        totalPages={totalPages}
        totalItems={totalItems}
        pageSize={PAGE_SIZE}
        onPageChange={setPage}
        loading={loading}
        emptyMessage={search ? `No plans match "${search}".` : 'No plans yet.'}
      />

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add Plan"
        subtitle="Prices are in USD"
        maxWidth="max-w-2xl"
      >
        <form onSubmit={handleCreate} className="space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input label="Plan Name" name="name" value={form.name} onChange={handleChange} error={formErrors.name} required />
            <Select
              label="Product"
              name="productId"
              value={form.productId}
              onChange={handleChange}
              options={productOptions}
              placeholder="No product"
              error={formErrors.productId}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Input
              label="Price"
              name="price"
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={handleChange}
              error={formErrors.price}
              required
            />
            <Select
              label="Billing Period"
              name="billingPeriod"
              value={form.billingPeriod}
              onChange={handleChange}
              options={BILLING_PERIODS}
              error={formErrors.billingPeriod}
              required
            />
            <Input
              label="Min Quantity"
              name="minQuantity"
              type="number"
              min="1"
              step="1"
              value={form.minQuantity}
              onChange={handleChange}
              error={formErrors.minQuantity}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Start Date"
              name="startDate"
              type="date"
              value={form.startDate}
              onChange={handleChange}
              error={formErrors.startDate}
              required
            />
            <Input
              label="End Date"
              name="endDate"
              type="date"
              min={form.startDate || undefined}
              value={form.endDate}
              onChange={handleChange}
              error={formErrors.endDate}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {FLAGS.map((flag) => (
              <Toggle
                key={flag.name}
                label={flag.label}
                description={flag.description}
                checked={form[flag.name]}
                onChange={(value) => setForm((prev) => ({ ...prev, [flag.name]: value }))}
              />
            ))}
          </div>

          <div className="pt-3 flex items-center justify-end gap-2.5">
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" loading={saving} icon={Plus}>
              Create Plan
            </Button>
          </div>
        </form>
      </Modal>

      {toast}
    </div>
  );
};

export default Plans;
