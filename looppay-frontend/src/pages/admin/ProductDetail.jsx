import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Plus, Pencil, Trash2, Save } from 'lucide-react';
import Card from '../../components/common/Card';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import { useToast, apiErrorMessage } from '../../components/common/Toast';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { formatMoney } from '../../utils/format';

const PAGE_SIZE = 10;
const EMPTY_VARIANT = { attribute: '', value: '', extraPrice: '' };

/**
 * Product detail with its Variants. Final price = product sales price + variant extra price.
 * ADMIN can add, edit and remove variants; STAFF is view-only.
 */
const ProductDetail = () => {
  const { id } = useParams();
  const { role } = useAuth();
  const isAdmin = role === 'ADMIN';
  const { toast, showToast } = useToast();

  const [product, setProduct] = useState(null);
  const [notFound, setNotFound] = useState(false);

  // Variants list state (page is 1-based for the Table)
  const [variants, setVariants] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(false);

  // Add / Edit variant modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_VARIANT);
  const [formErrors, setFormErrors] = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api
      .get(`/api/products/${id}`)
      .then((response) => setProduct(response.data.data))
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
        showToast(apiErrorMessage(err, 'Failed to load product.'), 'error');
      });
  }, [id, showToast]);

  const fetchVariants = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get(`/api/products/${id}/variants`, {
        params: { page: page - 1, size: PAGE_SIZE },
      });
      const result = response.data.data;
      setVariants(result.content);
      setTotalPages(result.totalPages);
      setTotalItems(result.totalElements);
    } catch (err) {
      showToast(apiErrorMessage(err, 'Failed to load variants.'), 'error');
    } finally {
      setLoading(false);
    }
  }, [id, page, showToast]);

  useEffect(() => {
    fetchVariants();
  }, [fetchVariants]);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_VARIANT);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEdit = (variant) => {
    setEditingId(variant.id);
    setForm({ attribute: variant.attribute, value: variant.value, extraPrice: String(variant.extraPrice) });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) setFormErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const handleSave = async (e) => {
    e.preventDefault();

    const errors = {};
    if (!form.attribute.trim()) errors.attribute = 'Attribute is required (e.g. Edition, Storage).';
    if (!form.value.trim()) errors.value = 'Value is required (e.g. Premium, 100GB).';
    if (form.extraPrice === '' || Number(form.extraPrice) < 0) errors.extraPrice = 'Enter an extra price of 0 or more.';
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    const payload = {
      attribute: form.attribute.trim(),
      value: form.value.trim(),
      extraPrice: Number(form.extraPrice),
    };

    setSaving(true);
    try {
      const response = editingId
        ? await api.put(`/api/products/variants/${editingId}`, payload)
        : await api.post(`/api/products/${id}/variants`, payload);
      showToast(response.data.message);
      setIsModalOpen(false);
      fetchVariants();
    } catch (err) {
      if (err.response?.data?.errors) setFormErrors(err.response.data.errors);
      showToast(apiErrorMessage(err, 'Failed to save variant.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (variant) => {
    if (!window.confirm(`Remove variant "${variant.attribute}: ${variant.value}"?`)) return;

    try {
      const response = await api.delete(`/api/products/variants/${variant.id}`);
      showToast(response.data.message);
      // Step back a page if we just removed the last row on it
      if (variants.length === 1 && page > 1) setPage(page - 1);
      else fetchVariants();
    } catch (err) {
      showToast(apiErrorMessage(err, 'Failed to remove variant.'), 'error');
    }
  };

  const salesPrice = Number(product?.salesPrice) || 0;

  const columns = [
    { header: 'Attribute', accessor: 'attribute', className: 'font-semibold' },
    { header: 'Value', accessor: 'value' },
    { header: 'Sales Price', render: () => formatMoney(salesPrice), className: 'text-right text-neutral-500' },
    { header: 'Extra Price', render: (v) => `+ ${formatMoney(v.extraPrice)}`, className: 'text-right' },
    {
      header: 'Final Price',
      render: (v) => <span className="font-bold">{formatMoney(salesPrice + Number(v.extraPrice))}</span>,
      className: 'text-right',
    },
    ...(isAdmin
      ? [
          {
            header: 'Actions',
            className: 'text-right',
            render: (v) => (
              <div className="flex items-center justify-end gap-1">
                <Button variant="ghost" size="sm" icon={Pencil} onClick={() => openEdit(v)}>
                  Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  icon={Trash2}
                  onClick={() => handleDelete(v)}
                  className="text-status-danger hover:bg-status-danger-bg"
                >
                  Remove
                </Button>
              </div>
            ),
          },
        ]
      : []),
  ];

  if (notFound) {
    return (
      <Card title="Product not found" subtitle="It may have been deleted.">
        <Link to="/admin/products" className="text-sm font-semibold text-aubergine hover:underline">
          Back to products
        </Link>
        {toast}
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Link
        to="/admin/products"
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-aubergine"
      >
        <ArrowLeft className="w-3.5 h-3.5" /> All products
      </Link>

      {/* Product summary */}
      <div className="bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        {product ? (
          <>
            <div className="flex items-center gap-2">
              <h2 className="text-2xl font-bold text-aubergine tracking-tight">{product.name}</h2>
              <Badge variant="neutral">{product.type}</Badge>
            </div>
            {product.notes && <p className="text-xs text-neutral-500 mt-1">{product.notes}</p>}

            <dl className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-5 text-xs">
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border/80">
                <dt className="text-neutral-500">Sales Price</dt>
                <dd className="text-base font-bold text-aubergine mt-0.5">{formatMoney(product.salesPrice)}</dd>
              </div>
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border/80">
                <dt className="text-neutral-500">Cost Price</dt>
                <dd className="text-base font-bold text-aubergine mt-0.5">{formatMoney(product.costPrice)}</dd>
              </div>
              <div className="p-3 bg-silver-light rounded-xl border border-silver-border/80">
                <dt className="text-neutral-500">Variants</dt>
                <dd className="text-base font-bold text-aubergine mt-0.5">{totalItems}</dd>
              </div>
            </dl>
          </>
        ) : (
          <p className="text-sm text-neutral-500">Loading product...</p>
        )}
      </div>

      {/* Variants */}
      <Card
        title="Variants"
        subtitle="Final price = sales price + extra price"
        action={
          isAdmin && (
            <Button variant="primary" size="sm" icon={Plus} onClick={openCreate} disabled={!product}>
              Add Variant
            </Button>
          )
        }
        bodyClassName="p-6 pt-4"
      >
        <Table
          columns={columns}
          data={variants}
          page={page}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
          loading={loading}
          emptyMessage="This product has no variants yet."
        />
      </Card>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingId ? 'Edit Variant' : 'Add Variant'}
        subtitle={product?.name}
      >
        <form onSubmit={handleSave} className="space-y-3.5">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Attribute"
              name="attribute"
              placeholder="Edition"
              value={form.attribute}
              onChange={handleChange}
              error={formErrors.attribute}
              required
            />
            <Input
              label="Value"
              name="value"
              placeholder="Premium"
              value={form.value}
              onChange={handleChange}
              error={formErrors.value}
              required
            />
          </div>

          <Input
            label="Extra Price"
            name="extraPrice"
            type="number"
            min="0"
            step="0.01"
            value={form.extraPrice}
            onChange={handleChange}
            error={formErrors.extraPrice}
            required
          />

          <div className="p-3 bg-silver-light rounded-xl border border-silver-border/80 text-xs text-neutral-600 flex justify-between">
            <span>
              {formatMoney(salesPrice)} + {formatMoney(form.extraPrice)}
            </span>
            <span className="font-bold text-aubergine">
              Final price: {formatMoney(salesPrice + (Number(form.extraPrice) || 0))}
            </span>
          </div>

          <div className="pt-3 flex items-center justify-end gap-2.5">
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" loading={saving} icon={editingId ? Save : Plus}>
              {editingId ? 'Save Changes' : 'Add Variant'}
            </Button>
          </div>
        </form>
      </Modal>

      {toast}
    </div>
  );
};

export default ProductDetail;
