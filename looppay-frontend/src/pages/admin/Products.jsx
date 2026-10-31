import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Pencil, Trash2, Eye, Save } from 'lucide-react';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Modal from '../../components/common/Modal';
import { useToast, apiErrorMessage } from '../../components/common/Toast';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { formatMoney, CURRENCY } from '../../utils/format';

const PAGE_SIZE = 10;
const PRODUCT_TYPES = ['SERVICE', 'GOODS', 'SOFTWARE'].map((t) => ({ value: t, label: t }));
const EMPTY_FORM = { name: '', type: 'SERVICE', salesPrice: '', costPrice: '', notes: '' };

/**
 * Product catalogue: paginated list with search. ADMIN can add, edit and soft delete; STAFF is view-only.
 */
const Products = () => {
  const { role } = useAuth();
  const isAdmin = role === 'ADMIN';
  const navigate = useNavigate();
  const { toast, showToast } = useToast();

  // List state (page is 1-based for the Table, the backend is 0-based)
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  // Add / Edit modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formErrors, setFormErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/products', {
        params: { page: page - 1, size: PAGE_SIZE, search: search || undefined },
      });
      const result = response.data.data;
      setProducts(result.content);
      setTotalPages(result.totalPages);
      setTotalItems(result.totalElements);
    } catch (err) {
      showToast(apiErrorMessage(err, 'Failed to load products.'), 'error');
    } finally {
      setLoading(false);
    }
  }, [page, search, showToast]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(1);
    setSearch(searchInput.trim());
  };

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormErrors({});
    setIsModalOpen(true);
  };

  const openEdit = (product) => {
    setEditingId(product.id);
    setForm({
      name: product.name,
      type: product.type,
      salesPrice: String(product.salesPrice),
      costPrice: String(product.costPrice),
      notes: product.notes || '',
    });
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
    if (!form.name.trim()) errors.name = 'Product name is required.';
    if (form.salesPrice === '' || Number(form.salesPrice) < 0) errors.salesPrice = 'Enter a sales price of 0 or more.';
    if (form.costPrice === '' || Number(form.costPrice) < 0) errors.costPrice = 'Enter a cost price of 0 or more.';
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    const payload = {
      name: form.name.trim(),
      type: form.type,
      salesPrice: Number(form.salesPrice),
      costPrice: Number(form.costPrice),
      notes: form.notes.trim(),
    };

    setSaving(true);
    try {
      const response = editingId
        ? await api.put(`/api/products/${editingId}`, payload)
        : await api.post('/api/products', payload);
      showToast(response.data.message);
      setIsModalOpen(false);
      fetchProducts();
    } catch (err) {
      if (err.response?.data?.errors) setFormErrors(err.response.data.errors);
      showToast(apiErrorMessage(err, 'Failed to save product.'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (product) => {
    if (!window.confirm(`Delete product "${product.name}"? It will be hidden from the catalogue.`)) return;

    try {
      const response = await api.delete(`/api/products/${product.id}`);
      showToast(response.data.message);
      // Step back a page if we just removed the last row on it
      if (products.length === 1 && page > 1) setPage(page - 1);
      else fetchProducts();
    } catch (err) {
      showToast(apiErrorMessage(err, 'Failed to delete product.'), 'error');
    }
  };

  const columns = [
    {
      header: 'Product',
      render: (p) => (
        <button
          onClick={() => navigate(`/admin/products/${p.id}`)}
          className="text-left font-semibold text-aubergine hover:underline focus:outline-none focus:underline"
        >
          {p.name}
          {p.notes && <span className="block text-[11px] font-normal text-neutral-500 truncate max-w-xs">{p.notes}</span>}
        </button>
      ),
    },
    { header: 'Type', render: (p) => <Badge variant="neutral" size="sm">{p.type}</Badge> },
    { header: 'Sales Price', render: (p) => formatMoney(p.salesPrice), className: 'text-right' },
    { header: 'Cost Price', render: (p) => formatMoney(p.costPrice), className: 'text-right' },
    { header: 'Variants', accessor: 'variantsCount', className: 'text-center' },
    {
      header: 'Actions',
      className: 'text-right',
      render: (p) => (
        <div className="flex items-center justify-end gap-1">
          <Button variant="ghost" size="sm" icon={Eye} onClick={() => navigate(`/admin/products/${p.id}`)}>
            View
          </Button>
          {isAdmin && (
            <>
              <Button variant="ghost" size="sm" icon={Pencil} onClick={() => openEdit(p)}>
                Edit
              </Button>
              <Button
                variant="ghost"
                size="sm"
                icon={Trash2}
                onClick={() => handleDelete(p)}
                className="text-status-danger hover:bg-status-danger-bg"
              >
                Delete
              </Button>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <div>
          <h2 className="text-2xl font-bold text-aubergine tracking-tight">Products</h2>
          <p className="text-xs text-neutral-500 mt-1">
            {isAdmin
              ? 'Manage the catalogue that subscription plans are built on.'
              : 'Read-only view of the product catalogue.'}
          </p>
        </div>

        {isAdmin && (
          <Button variant="primary" icon={Plus} onClick={openCreate}>
            Add Product
          </Button>
        )}
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 max-w-md">
        <Input
          name="search"
          placeholder="Search products by name"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          icon={Search}
          aria-label="Search products"
        />
        <Button variant="secondary" type="submit">
          Search
        </Button>
      </form>

      <Table
        columns={columns}
        data={products}
        page={page}
        totalPages={totalPages}
        totalItems={totalItems}
        pageSize={PAGE_SIZE}
        onPageChange={setPage}
        loading={loading}
        emptyMessage={search ? `No products match "${search}".` : 'No products yet.'}
      />

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingId ? 'Edit Product' : 'Add Product'}
        subtitle={`Prices are in ${CURRENCY}`}
      >
        <form onSubmit={handleSave} className="space-y-3.5">
          <Input label="Product Name" name="name" value={form.name} onChange={handleChange} error={formErrors.name} required />

          <Select
            label="Type"
            name="type"
            value={form.type}
            onChange={handleChange}
            options={PRODUCT_TYPES}
            error={formErrors.type}
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Sales Price"
              name="salesPrice"
              type="number"
              min="0"
              step="0.01"
              value={form.salesPrice}
              onChange={handleChange}
              error={formErrors.salesPrice}
              required
            />
            <Input
              label="Cost Price"
              name="costPrice"
              type="number"
              min="0"
              step="0.01"
              value={form.costPrice}
              onChange={handleChange}
              error={formErrors.costPrice}
              required
            />
          </div>

          <Input label="Notes" name="notes" value={form.notes} onChange={handleChange} error={formErrors.notes} />

          <div className="pt-3 flex items-center justify-end gap-2.5">
            <Button variant="ghost" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" loading={saving} icon={editingId ? Save : Plus}>
              {editingId ? 'Save Changes' : 'Create Product'}
            </Button>
          </div>
        </form>
      </Modal>

      {toast}
    </div>
  );
};

export default Products;
