import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Download, FileText } from 'lucide-react';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import { SkeletonTable, EmptyState, ErrorState } from '../../components/common/States';
import { apiErrorMessage } from '../../components/common/Toast';
import api from '../../api/axios';
import { formatMoney } from '../../utils/format';
import { InvoiceStatusBadge, downloadInvoice, pageOf } from './portalShared';

const PAGE_SIZE = 10;

/**
 * /portal/invoices: the logged-in customer's invoices (GET /api/portal/invoices).
 */
const MyInvoices = () => {
  const [invoices, setInvoices] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    api
      .get('/api/portal/invoices', { params: { page: page - 1, size: PAGE_SIZE } })
      .then((response) => {
        const result = pageOf(response);
        setInvoices(result.content);
        setTotalPages(result.totalPages);
        setTotalItems(result.totalElements);
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load your invoices.')))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const columns = [
    { header: 'Number', accessor: 'invoiceNumber', className: 'font-semibold whitespace-nowrap' },
    {
      header: 'Subscription',
      render: (i) => (
        <Link to={`/portal/subscriptions/${i.subscriptionId}`} className="hover:underline">
          {i.subscriptionNumber}
          <span className="block text-[11px] text-neutral-500">{i.planName}</span>
        </Link>
      ),
    },
    { header: 'Issued', accessor: 'issueDate', className: 'whitespace-nowrap' },
    { header: 'Due', accessor: 'dueDate', className: 'whitespace-nowrap' },
    { header: 'Amount', render: (i) => <span className="font-semibold">{formatMoney(i.totalAmount)}</span>, className: 'text-right' },
    { header: 'Status', render: (i) => <InvoiceStatusBadge invoice={i} /> },
    {
      header: 'Actions',
      className: 'text-right',
      render: (i) => (
        <Button variant="outline" size="sm" icon={Download} onClick={() => downloadInvoice(i.id)} className="whitespace-nowrap">
          Download PDF
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <h2 className="text-2xl font-bold text-aubergine tracking-tight">My Invoices</h2>
        <p className="text-xs text-neutral-500 mt-1">One invoice per billing period of each subscription.</p>
      </div>

      {loading ? (
        <SkeletonTable rows={4} cols={6} />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : invoices.length === 0 ? (
        <div className="bg-white rounded-xl shadow-soft border border-silver-border">
          <EmptyState icon={FileText} title="No invoices yet" message="An invoice is issued when a subscription becomes active." />
        </div>
      ) : (
        <Table
          columns={columns}
          data={invoices}
          page={page}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
        />
      )}
    </div>
  );
};

export default MyInvoices;
