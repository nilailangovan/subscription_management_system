import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Check, CheckCircle, ShoppingCart } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import { Skeleton, ErrorState } from '../../components/common/States';
import { useToast, apiErrorMessage } from '../../components/common/Toast';
import api from '../../api/axios';
import { formatMoney, periodUnit } from '../../utils/format';

const STEPS = ['Choose', 'Review', 'Confirm'];
const PREVIEW_DELAY_MS = 400;

/** Numbered progress indicator for the three wizard steps. */
const Stepper = ({ current }) => (
  <ol className="flex items-center gap-2 sm:gap-4" aria-label="Subscription steps">
    {STEPS.map((label, index) => {
      const done = index < current;
      const active = index === current;
      return (
        <li key={label} className="flex items-center gap-2 flex-1 last:flex-none" aria-current={active ? 'step' : undefined}>
          <span
            className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${
              done ? 'bg-status-success text-white' : active ? 'bg-marigold text-aubergine' : 'bg-silver text-neutral-500'
            }`}
          >
            {done ? <Check className="w-4 h-4" /> : index + 1}
          </span>
          <span className={`text-sm font-semibold ${active ? 'text-aubergine' : 'text-neutral-500'}`}>{label}</span>
          {index < STEPS.length - 1 && <span className="hidden sm:block flex-1 h-px bg-silver-border" />}
        </li>
      );
    })}
  </ol>
);

/** One label/value row in the totals box. */
const TotalRow = ({ label, value, strong }) => (
  <div className={`flex justify-between ${strong ? 'text-base font-bold text-aubergine pt-2 border-t border-silver-border' : 'text-sm text-neutral-600'}`}>
    <span>{label}</span>
    <span>{value}</span>
  </div>
);

/**
 * /portal/subscribe/:planId: Choose -> Review -> Confirm.
 * Prices come from the preview API (server-side pricing); the page never sends a price.
 */
const SubscribeWizard = () => {
  const { planId } = useParams();
  const navigate = useNavigate();
  const { toast, showToast } = useToast();
  const today = new Date().toLocaleDateString('en-CA');

  const [plan, setPlan] = useState(null);
  const [planError, setPlanError] = useState('');
  const [termsOptions, setTermsOptions] = useState([]);
  const [step, setStep] = useState(0);
  const [form, setForm] = useState({ productId: '', variantId: '', quantity: '', startDate: today, paymentTerms: '' });
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState(null);

  useEffect(() => {
    Promise.all([api.get(`/api/portal/plans/${planId}`), api.get('/api/portal/payment-terms')])
      .then(([planResponse, termsResponse]) => {
        const loadedPlan = planResponse.data.data;
        const terms = termsResponse.data.data;
        setPlan(loadedPlan);
        setTermsOptions(terms);
        setForm((prev) => ({
          ...prev,
          productId: loadedPlan.products[0] ? String(loadedPlan.products[0].id) : '',
          quantity: String(loadedPlan.minQuantity),
          paymentTerms: terms[0] || '',
        }));
      })
      .catch((err) => setPlanError(apiErrorMessage(err, 'Could not load this plan.')));
  }, [planId]);

  const product = plan?.products.find((p) => String(p.id) === form.productId);
  const variant = product?.variants.find((v) => String(v.id) === form.variantId);
  const unitPrice = variant ? variant.finalPrice : product?.salesPrice;
  const quantity = Number(form.quantity);

  // Re-price on every change of product, variant or quantity (debounced; stale responses ignored)
  useEffect(() => {
    if (!plan || !product || !(quantity >= 1)) return undefined;
    let cancelled = false;
    const timer = setTimeout(() => {
      setPreviewLoading(true);
      api
        .post('/api/portal/subscriptions/preview', {
          planId: plan.id,
          lines: [{ productId: product.id, variantId: variant ? variant.id : null, quantity }],
        })
        .then((response) => {
          if (cancelled) return;
          setPreview(response.data.data);
          setPreviewError('');
        })
        .catch((err) => {
          if (cancelled) return;
          setPreview(null);
          setPreviewError(apiErrorMessage(err, 'Could not price this subscription.'));
        })
        .finally(() => !cancelled && setPreviewLoading(false));
    }, PREVIEW_DELAY_MS);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [plan, product, variant, quantity]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    // A new product has its own variants, so clear the old choice
    setForm((prev) => ({ ...prev, [name]: value, ...(name === 'productId' ? { variantId: '' } : {}) }));
  };

  if (planError) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto">
        <ErrorState message={planError} />
        <Link to="/portal#available-plans" className="inline-flex items-center gap-1.5 text-xs font-semibold text-aubergine hover:underline">
          <ArrowLeft className="w-3.5 h-3.5" /> Back to available plans
        </Link>
      </div>
    );
  }

  if (!plan) {
    return (
      <div className="max-w-3xl mx-auto space-y-4" role="status" aria-label="Loading plan">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  // Client-side checks mirror the server rules so errors show inline before submitting
  const errors = {};
  if (!product) errors.productId = 'Choose a product.';
  if (!Number.isInteger(quantity) || quantity < plan.minQuantity) {
    errors.quantity = `Quantity must be at least ${plan.minQuantity} (the plan minimum).`;
  } else if (/quantity/i.test(previewError)) {
    errors.quantity = previewError;
  }
  if (!form.startDate) errors.startDate = 'Choose a start date.';
  else if (form.startDate < today) errors.startDate = 'Start date cannot be in the past.';
  else if (form.startDate >= plan.endDate) errors.startDate = `Start date must be before ${plan.endDate}, when the plan ends.`;
  const chooseValid = Object.keys(errors).length === 0;
  const previewReady = preview && !previewError && !previewLoading;

  const handleConfirm = async () => {
    setSubmitting(true);
    try {
      const response = await api.post('/api/portal/subscriptions', {
        planId: plan.id,
        lines: [{ productId: product.id, variantId: variant ? variant.id : null, quantity }],
        startDate: form.startDate,
        paymentTerms: form.paymentTerms,
      });
      setCreated(response.data.data);
    } catch (err) {
      showToast(apiErrorMessage(err, 'Could not create the subscription.'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (created) {
    return (
      <Card className="max-w-xl mx-auto text-center" bodyClassName="p-8">
        <CheckCircle className="w-12 h-12 text-status-success mx-auto" />
        <h2 className="text-2xl font-bold text-aubergine mt-3">You're subscribed!</h2>
        <p className="text-sm text-neutral-600 mt-1">
          Subscription <strong className="text-aubergine">{created.subscriptionNumber}</strong> for {created.planName}
        </p>
        <div className="mt-3 flex items-center justify-center gap-2">
          <span className="text-xs text-neutral-500">Status</span>
          <Badge status={created.status} />
        </div>
        <p className="text-sm text-neutral-600 mt-3">
          Total {formatMoney(created.grandTotal)} / {periodUnit(created.billingPeriod)}
        </p>
        <div className="mt-6 flex flex-col sm:flex-row gap-2 justify-center">
          <Button variant="primary" onClick={() => navigate('/portal/subscriptions')}>
            Go to My Subscriptions
          </Button>
          <Button variant="outline" onClick={() => navigate(`/portal/subscriptions/${created.id}`)}>
            View details
          </Button>
        </div>
      </Card>
    );
  }

  const line = preview?.lineResults?.[0];

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <Link to="/portal#available-plans" className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-500 hover:text-aubergine">
        <ArrowLeft className="w-3.5 h-3.5" /> Available plans
      </Link>

      <div className="bg-white p-6 rounded-2xl shadow-soft border border-silver-border space-y-5">
        <div>
          <h2 className="text-2xl font-bold text-aubergine tracking-tight">Subscribe to {plan.name}</h2>
          <p className="text-xs text-neutral-500 mt-1">
            Billed every {periodUnit(plan.billingPeriod)} · minimum {plan.minQuantity} {plan.minQuantity === 1 ? 'unit' : 'units'} · available until {plan.endDate}
          </p>
        </div>
        <Stepper current={step} />
      </div>

      {/* Step 1: Choose */}
      {step === 0 && (
        <Card title="Choose your product" subtitle="Prices update as you change the options">
          <div className="space-y-4">
            <Select
              label="Product"
              name="productId"
              value={form.productId}
              onChange={handleChange}
              options={plan.products.map((p) => ({ value: String(p.id), label: `${p.name} — ${formatMoney(p.salesPrice)}` }))}
              error={errors.productId}
              required
            />

            {product && product.variants.length > 0 && (
              <Select
                label="Variant"
                name="variantId"
                value={form.variantId}
                onChange={handleChange}
                placeholder={`Base — ${formatMoney(product.salesPrice)}`}
                options={product.variants.map((v) => ({
                  value: String(v.id),
                  label: `${v.attribute}: ${v.value} (+${formatMoney(v.extraPrice)}) — ${formatMoney(v.finalPrice)}`,
                }))}
              />
            )}

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <Input
                label="Quantity"
                name="quantity"
                type="number"
                min={plan.minQuantity}
                step="1"
                value={form.quantity}
                onChange={handleChange}
                error={errors.quantity}
                helperText={`Minimum ${plan.minQuantity}`}
                required
              />
              <Input
                label="Start Date"
                name="startDate"
                type="date"
                min={today}
                max={plan.endDate}
                value={form.startDate}
                onChange={handleChange}
                error={errors.startDate}
                required
              />
              <Select
                label="Payment Terms"
                name="paymentTerms"
                value={form.paymentTerms}
                onChange={handleChange}
                options={termsOptions.map((terms) => ({ value: terms, label: terms }))}
                required
              />
            </div>

            {product && (
              <div className="p-4 bg-silver-light rounded-xl border border-silver-border/80 flex flex-wrap items-center justify-between gap-2 text-sm">
                <span className="text-neutral-600">
                  Unit price <strong className="text-aubergine">{formatMoney(unitPrice)}</strong> / {periodUnit(plan.billingPeriod)}
                </span>
                <span className="text-neutral-600" aria-live="polite">
                  {previewLoading ? 'Calculating…' : previewReady ? (
                    <>Total <strong className="text-aubergine">{formatMoney(preview.grandTotal)}</strong></>
                  ) : null}
                </span>
              </div>
            )}
          </div>

          <div className="mt-6 flex justify-end">
            <Button variant="primary" icon={ArrowRight} disabled={!chooseValid} onClick={() => setStep(1)}>
              Review
            </Button>
          </div>
        </Card>
      )}

      {/* Step 2: Review */}
      {step === 1 && (
        <Card title="Review your subscription" subtitle="Prices, tax and discounts are calculated by LoopPay">
          {previewError ? (
            <ErrorState message={previewError} />
          ) : !preview || previewLoading ? (
            <div className="space-y-2" role="status" aria-label="Calculating price">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-24 w-full" />
            </div>
          ) : (
            <div className="space-y-5">
              <div className="overflow-x-auto rounded-xl border border-silver-border">
                <table className="w-full text-left text-sm text-aubergine">
                  <thead className="bg-silver-light text-xs font-semibold uppercase tracking-wider text-neutral-600">
                    <tr>
                      <th className="px-4 py-3">Product</th>
                      <th className="px-4 py-3 text-right">Qty</th>
                      <th className="px-4 py-3 text-right">Unit Price</th>
                      <th className="px-4 py-3 text-right">Tax</th>
                      <th className="px-4 py-3 text-right">Discount</th>
                      <th className="px-4 py-3 text-right">Line Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="border-t border-silver-border/60">
                      <td className="px-4 py-3">
                        <span className="font-semibold">{product.name}</span>
                        {variant && <span className="block text-[11px] text-neutral-500">{variant.attribute}: {variant.value}</span>}
                      </td>
                      <td className="px-4 py-3 text-right">{line.quantity}</td>
                      <td className="px-4 py-3 text-right">{formatMoney(line.unitPrice)}</td>
                      <td className="px-4 py-3 text-right">{formatMoney(line.taxAmount)}</td>
                      <td className="px-4 py-3 text-right">− {formatMoney(line.discountAmount)}</td>
                      <td className="px-4 py-3 text-right font-semibold">{formatMoney(line.lineTotal)}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <dl className="text-sm space-y-1.5">
                  <div className="flex gap-2"><dt className="text-neutral-500 w-32">Start date</dt><dd className="font-semibold">{form.startDate}</dd></div>
                  <div className="flex gap-2"><dt className="text-neutral-500 w-32">Billed every</dt><dd className="font-semibold">{periodUnit(plan.billingPeriod)}</dd></div>
                  <div className="flex gap-2"><dt className="text-neutral-500 w-32">Payment terms</dt><dd className="font-semibold">{form.paymentTerms}</dd></div>
                </dl>
                <div className="space-y-1.5 p-4 bg-silver-light rounded-xl border border-silver-border/80">
                  <TotalRow label="Subtotal" value={formatMoney(preview.subtotal)} />
                  <TotalRow label="Total tax" value={formatMoney(preview.totalTax)} />
                  <TotalRow label="Total discount" value={`− ${formatMoney(preview.totalDiscount)}`} />
                  <TotalRow label="Grand total" value={formatMoney(preview.grandTotal)} strong />
                </div>
              </div>
            </div>
          )}

          <div className="mt-6 flex justify-between">
            <Button variant="ghost" icon={ArrowLeft} onClick={() => setStep(0)}>
              Back
            </Button>
            <Button variant="primary" icon={ArrowRight} disabled={!previewReady} onClick={() => setStep(2)}>
              Continue
            </Button>
          </div>
        </Card>
      )}

      {/* Step 3: Confirm */}
      {step === 2 && (
        <Card title="Confirm" subtitle="Your subscription starts once you confirm">
          <div className="p-4 bg-silver-light rounded-xl border border-silver-border/80 text-sm text-neutral-600 space-y-1">
            <p>
              <strong className="text-aubergine">{plan.name}</strong> · {product.name}
              {variant ? ` (${variant.attribute}: ${variant.value})` : ''} × {quantity}
            </p>
            <p>
              <strong className="text-aubergine">{formatMoney(preview.grandTotal)}</strong> every {periodUnit(plan.billingPeriod)}, starting {form.startDate}, {form.paymentTerms}
            </p>
          </div>

          <label className="mt-4 flex items-start gap-2.5 text-sm text-aubergine cursor-pointer">
            <input
              type="checkbox"
              checked={acceptedTerms}
              onChange={(e) => setAcceptedTerms(e.target.checked)}
              className="mt-0.5 w-4 h-4 accent-aubergine"
            />
            <span>I agree to be billed {formatMoney(preview.grandTotal)} every {periodUnit(plan.billingPeriod)} until I cancel or the plan ends on {plan.endDate}.</span>
          </label>

          <div className="mt-6 flex justify-between">
            <Button variant="ghost" icon={ArrowLeft} onClick={() => setStep(1)} disabled={submitting}>
              Back
            </Button>
            <Button variant="primary" icon={ShoppingCart} loading={submitting} disabled={!acceptedTerms} onClick={handleConfirm}>
              Confirm Subscription
            </Button>
          </div>
        </Card>
      )}

      {toast}
    </div>
  );
};

export default SubscribeWizard;
