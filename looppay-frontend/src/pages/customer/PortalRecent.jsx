import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Repeat, FileText, ArrowRight, Download } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { Skeleton, EmptyState, ErrorState } from '../../components/common/States';
import { apiErrorMessage } from '../../components/common/Toast';
import api from '../../api/axios';
import { formatMoney, periodUnit } from '../../utils/format';
import { InvoiceStatusBadge, downloadInvoice, pageOf } from './portalShared';

const LATEST = 3;

/** Loads the newest few records from a paginated portal endpoint. */
const useLatest = (url, fallbackError) => {
  const [records, setRecords] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setError('');
    setRecords(null);
    api
      .get(url, { params: { page: 0, size: LATEST } })
      .then((response) => setRecords(pageOf(response).content))
      .catch((err) => setError(apiErrorMessage(err, fallbackError)));
  }, [url, fallbackError]);

  useEffect(() => {
    load();
  }, [load]);

  return { records, error, load };
};

const ViewAll = ({ to }) => (
  <Link to={to} className="inline-flex items-center gap-1 text-xs font-semibold text-aubergine hover:underline">
    View all <ArrowRight className="w-3.5 h-3.5" />
  </Link>
);

const ListSkeleton = () => (
  <div className="space-y-3" role="status" aria-label="Loading">
    {Array.from({ length: LATEST }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
  </div>
);

/** Dashboard card: the customer's 3 newest subscriptions. */
export const RecentSubscriptions = () => {
  const navigate = useNavigate();
  const { records, error, load } = useLatest('/api/portal/subscriptions', 'Could not load your subscriptions.');

  return (
    <Card title="My Subscriptions" subtitle="Your latest subscriptions" action={<ViewAll to="/portal/subscriptions" />}>
      {error ? (
        <ErrorState message={error} onRetry={load} />
      ) : records === null ? (
        <ListSkeleton />
      ) : records.length === 0 ? (
        <EmptyState
          icon={Repeat}
          title="No subscriptions yet"
          message="Pick one of the available plans above to get started."
          action={<Button variant="primary" size="sm" onClick={() => navigate('/portal#available-plans')}>Browse plans</Button>}
        />
      ) : (
        <ul className="divide-y divide-silver-border/60">
          {records.map((s) => (
            <li key={s.id}>
              <Link to={`/portal/subscriptions/${s.id}`} className="py-3 flex items-center justify-between gap-3 text-sm hover:bg-silver-light/50 -mx-2 px-2 rounded-lg">
                <div>
                  <span className="font-semibold text-aubergine">{s.subscriptionNumber}</span>
                  <span className="block text-[11px] text-neutral-500">
                    {s.planName}{s.nextBillingDate ? ` · next bill ${s.nextBillingDate}` : ''}
                  </span>
                </div>
                <div className="flex items-center gap-3 text-right">
                  <span className="font-semibold whitespace-nowrap">
                    {formatMoney(s.grandTotal)}
                    <span className="text-[11px] font-normal text-neutral-500"> / {periodUnit(s.billingPeriod)}</span>
                  </span>
                  <Badge status={s.status} size="sm" />
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
};

/** Dashboard card: the customer's 3 newest invoices. */
export const RecentInvoices = () => {
  const { records, error, load } = useLatest('/api/portal/invoices', 'Could not load your invoices.');

  return (
    <Card title="My Invoices" subtitle="Your latest invoices" action={<ViewAll to="/portal/invoices" />}>
      {error ? (
        <ErrorState message={error} onRetry={load} />
      ) : records === null ? (
        <ListSkeleton />
      ) : records.length === 0 ? (
        <EmptyState icon={FileText} title="No invoices yet" message="An invoice is issued when a subscription becomes active." />
      ) : (
        <ul className="divide-y divide-silver-border/60">
          {records.map((i) => (
            <li key={i.id} className="py-3 flex items-center justify-between gap-3 text-sm">
              <div>
                <span className="font-semibold text-aubergine">{i.invoiceNumber}</span>
                <span className="block text-[11px] text-neutral-500">{i.subscriptionNumber} · due {i.dueDate}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-semibold whitespace-nowrap">{formatMoney(i.totalAmount)}</span>
                <InvoiceStatusBadge invoice={i} />
                <Button variant="ghost" size="sm" onClick={() => downloadInvoice(i.id)} aria-label={`Download ${i.invoiceNumber} as PDF`} className="px-2">
                  <Download className="w-4 h-4" />
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
};
