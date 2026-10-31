import React, { useState, useEffect } from 'react';
import { Layers, CalendarClock, Users, ChevronLeft, ChevronRight } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import api from '../../api/axios';
import { formatMoney } from '../../utils/format';

const PAGE_SIZE = 6;

/**
 * Customer-facing grid of plans that are ACTIVE and within their date range (GET /api/plans/available).
 */
const AvailablePlans = () => {
  const [plans, setPlans] = useState([]);
  const [page, setPage] = useState(0); // 0-based, as the backend expects
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    api
      .get('/api/plans/available', { params: { page, size: PAGE_SIZE } })
      .then((response) => {
        setPlans(response.data.data.content);
        setTotalPages(response.data.data.totalPages);
      })
      .catch((err) => setError(err.response?.data?.message || 'Could not load available plans.'))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <Card
      title="Available Plans"
      subtitle="Plans you can subscribe to right now"
      action={
        totalPages > 1 && (
          <div className="flex items-center gap-1.5">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              aria-label="Previous plans"
              className="px-2"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
              aria-label="Next plans"
              className="px-2"
            >
              <ChevronRight className="w-3.5 h-3.5" />
            </Button>
          </div>
        )
      }
    >
      {loading ? (
        <p className="py-10 text-center text-xs text-neutral-500">Loading plans...</p>
      ) : error ? (
        <p className="p-4 bg-status-danger-bg border border-status-danger/30 rounded-xl text-xs text-status-danger font-medium">
          {error}
        </p>
      ) : plans.length === 0 ? (
        <div className="py-10 text-center">
          <Layers className="w-7 h-7 text-aubergine/40 mx-auto mb-3" />
          <p className="text-sm font-bold text-aubergine">No plans available right now</p>
          <p className="text-xs text-neutral-500 mt-1">Check back soon for new offerings.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {plans.map((plan) => (
            <div
              key={plan.id}
              className="flex flex-col p-5 rounded-xl border border-silver-border bg-silver-light/40 hover:border-aubergine/40 transition"
            >
              <p className="text-[11px] font-semibold uppercase tracking-wider text-neutral-500">
                {plan.productName || 'LoopPay'}
              </p>
              <h4 className="text-base font-bold text-aubergine mt-0.5">{plan.name}</h4>

              <p className="mt-3">
                <span className="text-2xl font-bold text-aubergine">{formatMoney(plan.price)}</span>
                <span className="text-xs text-neutral-500"> / {plan.billingPeriod.toLowerCase()}</span>
              </p>

              <ul className="mt-4 space-y-1.5 text-xs text-neutral-600">
                <li className="flex items-center gap-2">
                  <Users className="w-3.5 h-3.5 text-aubergine/60" />
                  Minimum {plan.minQuantity} {plan.minQuantity === 1 ? 'unit' : 'units'}
                </li>
                <li className="flex items-center gap-2">
                  <CalendarClock className="w-3.5 h-3.5 text-aubergine/60" />
                  Available until {plan.endDate}
                </li>
              </ul>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
};

export default AvailablePlans;
