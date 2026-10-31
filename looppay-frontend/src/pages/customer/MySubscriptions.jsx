import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Repeat, Settings2 } from 'lucide-react';
import Table from '../../components/common/Table';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { SkeletonTable, EmptyState, ErrorState } from '../../components/common/States';
import { apiErrorMessage } from '../../components/common/Toast';
import api from '../../api/axios';
import { formatMoney, periodUnit } from '../../utils/format';
import { pageOf } from './portalShared';

const PAGE_SIZE = 10;

/**
 * /portal/subscriptions: the logged-in customer's subscriptions (GET /api/portal/subscriptions).
 */
const MySubscriptions = () => {
  const navigate = useNavigate();
  const [subscriptions, setSubscriptions] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    api
      .get('/api/portal/subscriptions', { params: { page: page - 1, size: PAGE_SIZE } })
      .then((response) => {
        const result = pageOf(response);
        setSubscriptions(result.content);
        setTotalPages(result.totalPages);
        setTotalItems(result.totalElements);
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load your subscriptions.')))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const columns = [
    { header: 'Number', accessor: 'subscriptionNumber', className: 'font-semibold whitespace-nowrap' },
    { header: 'Plan', accessor: 'planName' },
    { header: 'Status', render: (s) => <Badge status={s.status} size="sm" /> },
    { header: 'Start Date', accessor: 'startDate', className: 'whitespace-nowrap' },
    { header: 'Next Billing', render: (s) => s.nextBillingDate || '—', className: 'whitespace-nowrap' },
    {
      header: 'Total',
      className: 'text-right whitespace-nowrap',
      render: (s) => (
        <span>
          <span className="font-semibold">{formatMoney(s.grandTotal)}</span>
          <span className="text-[11px] text-neutral-500"> / {periodUnit(s.billingPeriod)}</span>
        </span>
      ),
    },
    {
      header: 'Actions',
      className: 'text-right',
      render: (s) => (
        <Button variant="outline" size="sm" icon={Settings2} onClick={() => navigate(`/portal/subscriptions/${s.id}`)}>
          Manage
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
        <div>
          <h2 className="text-2xl font-bold text-aubergine tracking-tight">My Subscriptions</h2>
          <p className="text-xs text-neutral-500 mt-1">Your plans, their status and when you are billed next.</p>
        </div>
        <Button variant="primary" icon={Plus} onClick={() => navigate('/portal#available-plans')}>
          New Plan
        </Button>
      </div>

      {loading ? (
        <SkeletonTable rows={4} cols={6} />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : subscriptions.length === 0 ? (
        <div className="bg-white rounded-xl shadow-soft border border-silver-border">
          <EmptyState
            icon={Repeat}
            title="No subscriptions yet"
            message="Pick a plan to get started. It will show up here with its status and next billing date."
            action={<Button variant="primary" size="sm" onClick={() => navigate('/portal#available-plans')}>Browse plans</Button>}
          />
        </div>
      ) : (
        <Table
          columns={columns}
          data={subscriptions}
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

export default MySubscriptions;
