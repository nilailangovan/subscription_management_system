import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Printer, Repeat } from 'lucide-react';
import Button from '../../components/common/Button';
import { Skeleton, ErrorState } from '../../components/common/States';
import { apiErrorMessage } from '../../components/common/Toast';
import api from '../../api/axios';
import { formatMoney } from '../../utils/format';
import { InvoiceStatusBadge } from './portalShared';

/**
 * /portal/invoices/:id/print: print-ready invoice. Opens the browser print dialog once loaded,
 * where "Save as PDF" downloads it (no PDF library needed).
 */
const InvoicePrint = () => {
  const { id } = useParams();
  const [invoice, setInvoice] = useState(null);
  const [subscription, setSubscription] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get(`/api/portal/invoices/${id}`)
      .then(async (response) => {
        const loaded = response.data.data;
        const sub = await api.get(`/api/portal/subscriptions/${loaded.subscriptionId}`);
        setInvoice(loaded);
        setSubscription(sub.data.data);
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load this invoice.')));
  }, [id]);

  useEffect(() => {
    if (!invoice) return undefined;
    document.title = `${invoice.invoiceNumber} — LoopPay`;
    // Let the page paint before the print dialog opens
    const timer = setTimeout(() => window.print(), 300);
    return () => clearTimeout(timer);
  }, [invoice]);

  if (error) return <div className="max-w-3xl mx-auto p-8"><ErrorState message={error} /></div>;
  if (!invoice || !subscription) {
    return (
      <div className="max-w-3xl mx-auto p-8 space-y-4" role="status" aria-label="Loading invoice">
        <Skeleton className="h-12 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-silver print:bg-white py-8 print:py-0">
      <div className="max-w-3xl mx-auto mb-4 flex justify-end print:hidden">
        <Button variant="primary" icon={Printer} onClick={() => window.print()}>
          Print / Save as PDF
        </Button>
      </div>

      <article className="max-w-3xl mx-auto bg-white p-10 rounded-2xl shadow-soft border border-silver-border print:shadow-none print:border-0 print:rounded-none text-aubergine">
        <header className="flex items-start justify-between gap-6 pb-6 border-b border-silver-border">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-aubergine flex items-center justify-center">
              <Repeat className="w-5 h-5 text-marigold" />
            </div>
            <span className="text-2xl font-bold tracking-tight">Loop<span className="text-marigold">Pay</span></span>
          </div>
          <div className="text-right">
            <h1 className="text-2xl font-bold">Invoice</h1>
            <p className="text-sm font-semibold">{invoice.invoiceNumber}</p>
            <div className="mt-1"><InvoiceStatusBadge invoice={invoice} /></div>
          </div>
        </header>

        <section className="grid grid-cols-2 gap-6 py-6 text-sm">
          <div>
            <p className="text-xs uppercase tracking-wider text-neutral-500 font-semibold">Billed to</p>
            <p className="font-semibold mt-1">{invoice.customerName}</p>
            <p className="text-neutral-600">{invoice.customerEmail}</p>
          </div>
          <dl className="text-right space-y-0.5">
            <div><dt className="inline text-neutral-500">Issued: </dt><dd className="inline font-semibold">{invoice.issueDate}</dd></div>
            <div><dt className="inline text-neutral-500">Due: </dt><dd className="inline font-semibold">{invoice.dueDate}</dd></div>
            <div><dt className="inline text-neutral-500">Period from: </dt><dd className="inline font-semibold">{invoice.billingPeriod}</dd></div>
            <div><dt className="inline text-neutral-500">Subscription: </dt><dd className="inline font-semibold">{invoice.subscriptionNumber} ({invoice.planName})</dd></div>
            <div><dt className="inline text-neutral-500">Terms: </dt><dd className="inline font-semibold">{invoice.paymentTerms}</dd></div>
          </dl>
        </section>

        <table className="w-full text-sm">
          <thead className="text-xs uppercase tracking-wider text-neutral-500 border-b border-silver-border">
            <tr>
              <th className="py-2 text-left">Item</th>
              <th className="py-2 text-right">Qty</th>
              <th className="py-2 text-right">Unit Price</th>
              <th className="py-2 text-right">Tax</th>
              <th className="py-2 text-right">Discount</th>
              <th className="py-2 text-right">Amount</th>
            </tr>
          </thead>
          <tbody>
            {subscription.lines.map((line) => (
              <tr key={line.id} className="border-b border-silver-border/60">
                <td className="py-2.5">
                  {line.productName}
                  {line.variantName && <span className="block text-[11px] text-neutral-500">{line.variantName}</span>}
                </td>
                <td className="py-2.5 text-right">{line.quantity}</td>
                <td className="py-2.5 text-right">{formatMoney(line.unitPrice)}</td>
                <td className="py-2.5 text-right">{formatMoney(line.tax)}</td>
                <td className="py-2.5 text-right">− {formatMoney(line.discount)}</td>
                <td className="py-2.5 text-right font-semibold">{formatMoney(line.amount)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="mt-6 ml-auto max-w-xs space-y-1.5 text-sm">
          <div className="flex justify-between"><span className="text-neutral-500">Subtotal</span><span>{formatMoney(invoice.subtotal)}</span></div>
          <div className="flex justify-between"><span className="text-neutral-500">Tax</span><span>{formatMoney(invoice.totalTax)}</span></div>
          <div className="flex justify-between"><span className="text-neutral-500">Discount</span><span>− {formatMoney(invoice.totalDiscount)}</span></div>
          <div className="flex justify-between text-base font-bold pt-2 border-t border-silver-border">
            <span>Total due</span><span>{formatMoney(invoice.totalAmount)}</span>
          </div>
        </div>
      </article>
    </div>
  );
};

export default InvoicePrint;
