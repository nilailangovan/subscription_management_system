import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Layers, CalendarClock, Users, Package, ChevronLeft, ChevronRight, ArrowRight } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import { Skeleton, EmptyState, ErrorState } from '../../components/common/States';
import api from '../../api/axios';
import { apiErrorMessage } from '../../components/common/Toast';
import { formatMoney, periodUnit } from '../../utils/format';
import { pageOf } from './portalShared';

const PAGE_SIZE = 6;

/** Lowest price a customer can pay per unit on this plan (cheapest product or variant). */
const startingPrice = (plan) => {
  const prices = plan.products.flatMap((product) => [
    Number(product.salesPrice),
    ...product.variants.map((variant) => Number(variant.finalPrice)),
  ]);
  return prices.length ? Math.min(...prices) : null;
};

/**
 * Customer-facing grid of plans open for subscription (GET /api/portal/plans).
 * Everything shown comes from the API, so new admin products, variants and plans appear on refresh.
 */
const AvailablePlans = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const sectionRef = useRef(null);

  const [plans, setPlans] = useState([]);
  const [page, setPage] = useState(0); // 0-based, as the backend expects
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    api
      .get('/api/portal/plans', { params: { page, size: PAGE_SIZE } })
      .then((response) => {
        setPlans(pageOf(response).content);
        setTotalPages(pageOf(response).totalPages);
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load available plans.')))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  // "New Plan" buttons link to /portal#available-plans
  useEffect(() => {
    if (location.hash === '#available-plans') sectionRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [location.hash]);

  return (
    <div id="available-plans" ref={sectionRef} className="scroll-mt-6">
      <Card
        title="Available Plans"
        subtitle="Plans you can subscribe to right now"
        action={
          totalPages > 1 && (
            <div className="flex items-center gap-1.5">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Previous plans" className="px-2">
                <ChevronLeft className="w-3.5 h-3.5" />
              </Button>
              <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)} aria-label="Next plans" className="px-2">
                <ChevronRight className="w-3.5 h-3.5" />
              </Button>
            </div>
          )
        }
      >
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4" role="status" aria-label="Loading plans">
            {[0, 1, 2].map((i) => (
              <div key={i} className="p-5 rounded-xl border border-silver-border space-y-3">
                <Skeleton className="h-3 w-24" />
                <Skeleton className="h-5 w-40" />
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-9 w-full" />
              </div>
            ))}
          </div>
        ) : error ? (
          <ErrorState message={error} onRetry={load} />
        ) : plans.length === 0 ? (
          <EmptyState icon={Layers} title="No plans available right now" message="Check back soon for new offerings." />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {plans.map((plan) => {
              const fromPrice = startingPrice(plan);
              return (
                <div
                  key={plan.id}
                  className="flex flex-col p-5 rounded-xl border border-silver-border bg-silver-light/40 hover:border-aubergine/40 transition"
                >
                  <p className="text-[11px] font-semibold uppercase tracking-wider text-neutral-500">
                    Billed every {periodUnit(plan.billingPeriod)}
                  </p>
                  <h4 className="text-base font-bold text-aubergine mt-0.5">{plan.name}</h4>

                  <p className="mt-3">
                    {fromPrice !== null ? (
                      <>
                        <span className="text-xs text-neutral-500">from </span>
                        <span className="text-2xl font-bold text-aubergine">{formatMoney(fromPrice)}</span>
                        <span className="text-xs text-neutral-500"> / {periodUnit(plan.billingPeriod)}</span>
                      </>
                    ) : (
                      <span className="text-sm text-neutral-500">No products yet</span>
                    )}
                  </p>

                  <ul className="mt-4 space-y-1.5 text-xs text-neutral-600 flex-1">
                    <li className="flex items-start gap-2">
                      <Package className="w-3.5 h-3.5 text-aubergine/60 mt-0.5 flex-shrink-0" />
                      <span>{plan.products.map((p) => p.name).join(', ') || 'No products'}</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Users className="w-3.5 h-3.5 text-aubergine/60" />
                      Minimum {plan.minQuantity} {plan.minQuantity === 1 ? 'unit' : 'units'}
                    </li>
                    <li className="flex items-center gap-2">
                      <CalendarClock className="w-3.5 h-3.5 text-aubergine/60" />
                      Available until {plan.endDate}
                    </li>
                  </ul>

                  <Button
                    variant="primary"
                    size="sm"
                    className="mt-4 w-full"
                    disabled={plan.products.length === 0}
                    onClick={() => navigate(`/portal/subscribe/${plan.id}`)}
                  >
                    Subscribe <ArrowRight className="w-3.5 h-3.5" />
                  </Button>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
};

export default AvailablePlans;
