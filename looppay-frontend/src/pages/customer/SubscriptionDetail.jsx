import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Check, Download, FileText, Zap } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { Skeleton, EmptyState, ErrorState } from '../../components/common/States';
import { useToast, apiErrorMessage } from '../../components/common/Toast';
import api from '../../api/axios';
import { formatMoney, periodUnit } from '../../utils/format';
import { InvoiceStatusBadge, downloadInvoice, pageOf } from './portalShared';

// SubscriptionStatus lifecycle order (RULES.md section 10)
const LIFECYCLE = ['DRAFT', 'QUOTATION', 'CONFIRMED', 'ACTIVE', 'CLOSED'];
const titleCase = (status) => status.charAt(0) + status.slice(1).toLowerCase();

/** Horizontal Draft -> Closed timeline; steps up to the current status are marked done. */
const StatusTimeline = ({ status }) => {
  const reached = LIFECYCLE.indexOf(status);
  return (
    <ol className="flex flex-wrap items-center gap-y-3" aria-label="Subscription status timeline">
      {LIFECYCLE.map((step, index) => {
        const done = index <= reached;
        const current = index === reached;
        return (
          <li key={step} className="flex items-center" aria-current={current ? 'step' : undefined}>
            <span className="flex items-center gap-2">
              <span
                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
                  current ? 'bg-marigold text-aubergine' : done ? 'bg-status-success text-white' : 'bg-silver text-neutral-500'
                }`}
              >
                {done && !current ? <Check className="w-3.5 h-3.5" /> : index + 1}
              </span>
              <span className={`text-xs font-semibold ${done ? 'text-aubergine' : 'text-neutral-400'}`}>{titleCase(step)}</span>
            </span>
            {index < LIFECYCLE.length - 1 && <span className={`mx-3 h-px w-8 sm:w-12 ${index < reached ? 'bg-status-success' : 'bg-silver-border'}`} />}
          </li>
        );
      })}
    </ol>
  );
};

/**
 * /portal/subscriptions/:id: one of the customer's subscriptions with lines, totals,
 * lifecycle timeline and its invoices. Someone else's id comes back as 404 from the API.
 */
const SubscriptionDetail = () => {
  const { id } = useParams();
  const { toast, showToast } = useToast();

  const [subscription, setSubscription] = useState(null);
  const [error, setError] = useState('');
  const [invoices, setInvoices] = useState(null);
  const [invoiceError, setInvoiceError] = useState('');
  const [activating, setActivating] = useState(false);

  const load = useCallback(() => {
    setError('');
    setInvoiceError('');
    api
      .get(`/api/portal/subscriptions/${id}`)
      .then((response) => setSubscription(response.data.data))
      .catch((err) => setError(apiErrorMessage(err, 'Could not load this subscription.')));
    api
      .get('/api/portal/invoices', { params: { subscriptionId: id, page: 0, size: 20 } })
      .then((response) => setInvoices(pageOf(response).content))
      .catch((err) => setInvoiceError(apiErrorMessage(err, 'Could not load invoices.')));
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const handleActivate = async () => {
    setActivating(true);
    try {
      const response = await api.post(`/api/portal/subscriptions/${id}/activate`);
      setSubscription(response.data.data);
      showToast(response.data.message);
      load();
    } catch (err) {
      showToast(apiErrorMessage(err, 'Could not activate the subscription.'), 'error');
    } finally {
      setActivating(false);
    }
  };

  if (error) {
    return (
      <div className="space-y-4">
        <ErrorState message={error} />
        <Link to="/portal/subscriptions" className="inline-flex items-center gap-1.5 text-xs font-semibold text-aubergine hover:underline">
          <ArrowLeft className="w-3.5 h-3.5" /> My Subscriptions
        </Link>
      </div>
    );
  }

  if (!subscription) {
    return (
      <div className="space-y-4" role="status" aria-label="Loading subscription">
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  const unit = periodUnit(subscription.billingPeriod);

  return (
    <div className="space-y-6">
      <Link to="/portal/subscriptions" className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-aubergine">
        <ArrowLeft className="w-3.5 h-3.5" /> My Subscriptions
      </Link>

      <div className="bg-white p-6 rounded-2xl shadow-soft border border-silver-border space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-2xl font-bold text-aubergine tracking-tight">{subscription.subscriptionNumber}</h2>
              <Badge status={subscription.status} />
            </div>
            <p className="text-xs text-neutral-500 mt-1">{subscription.planName} · billed every {unit}</p>
          </div>
          {subscription.status === 'CONFIRMED' && (
            <Button variant="primary" icon={Zap} loading={activating} onClick={handleActivate}>
              Activate
            </Button>
          )}
        </div>
        <StatusTimeline status={subscription.status} />
        <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          {[
            ['Start date', subscription.startDate],
            ['Ends', subscription.expiryDate],
            ['Next billing', subscription.nextBillingDate || '—'],
            ['Payment terms', subscription.paymentTerms || '—'],
          ].map(([label, value]) => (
            <div key={label} className="p-3 bg-silver-light rounded-xl border border-silver-border/80">
              <dt className="text-neutral-500">{label}</dt>
              <dd className="text-sm font-bold text-aubergine mt-0.5">{value}</dd>
            </div>
          ))}
        </dl>
      </div>

      <Card title="Lines & totals" subtitle={`Amounts per ${unit}`}>
        <div className="overflow-x-auto rounded-xl border border-silver-border">
          <table className="w-full text-left text-sm text-aubergine">
            <thead className="bg-silver-light text-xs font-semibold uppercase tracking-wider text-neutral-600">
              <tr>
                <th className="px-4 py-3">Product</th>
                <th className="px-4 py-3 text-right">Qty</th>
                <th className="px-4 py-3 text-right">Unit Price</th>
                <th className="px-4 py-3 text-right">Tax</th>
                <th className="px-4 py-3 text-right">Discount</th>
                <th className="px-4 py-3 text-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {subscription.lines.map((line) => (
                <tr key={line.id} className="border-t border-silver-border/60">
                  <td className="px-4 py-3">
                    <span className="font-semibold">{line.productName}</span>
                    {line.variantName && <span className="block text-[11px] text-neutral-500">{line.variantName}</span>}
                  </td>
                  <td className="px-4 py-3 text-right">{line.quantity}</td>
                  <td className="px-4 py-3 text-right">{formatMoney(line.unitPrice)}</td>
                  <td className="px-4 py-3 text-right">{formatMoney(line.tax)}</td>
                  <td className="px-4 py-3 text-right">− {formatMoney(line.discount)}</td>
                  <td className="px-4 py-3 text-right font-semibold">{formatMoney(line.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 ml-auto max-w-xs space-y-1.5 text-sm">
          <div className="flex justify-between text-neutral-600"><span>Subtotal</span><span>{formatMoney(subscription.subtotal)}</span></div>
          <div className="flex justify-between text-neutral-600"><span>Total tax</span><span>{formatMoney(subscription.totalTax)}</span></div>
          <div className="flex justify-between text-neutral-600"><span>Total discount</span><span>− {formatMoney(subscription.totalDiscount)}</span></div>
          <div className="flex justify-between text-base font-bold text-aubergine pt-2 border-t border-silver-border">
            <span>Grand total</span><span>{formatMoney(subscription.grandTotal)}</span>
          </div>
        </div>
      </Card>

      <Card title="Invoices" subtitle="Invoices issued for this subscription">
        {invoiceError ? (
          <ErrorState message={invoiceError} onRetry={load} />
        ) : invoices === null ? (
          <Skeleton className="h-16 w-full" />
        ) : invoices.length === 0 ? (
          <EmptyState icon={FileText} title="No invoices yet" message="The first invoice is issued when the subscription becomes active." />
        ) : (
          <ul className="divide-y divide-silver-border/60">
            {invoices.map((invoice) => (
              <li key={invoice.id} className="py-3 flex flex-wrap items-center justify-between gap-3 text-sm">
                <div>
                  <span className="font-semibold text-aubergine">{invoice.invoiceNumber}</span>
                  <span className="block text-[11px] text-neutral-500">Issued {invoice.issueDate} · due {invoice.dueDate}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-semibold">{formatMoney(invoice.totalAmount)}</span>
                  <InvoiceStatusBadge invoice={invoice} />
                  <Button variant="ghost" size="sm" icon={Download} onClick={() => downloadInvoice(invoice.id)}>
                    PDF
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      {toast}
    </div>
  );
};

export default SubscriptionDetail;
