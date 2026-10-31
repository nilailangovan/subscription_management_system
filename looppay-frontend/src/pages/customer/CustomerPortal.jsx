import React from 'react';
import { Repeat, FileText, CreditCard, Sparkles, Clock, ShieldCheck } from 'lucide-react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';

const CustomerPortal = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-white p-6 sm:p-8 rounded-2xl shadow-soft border border-silver-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-2xl font-bold text-aubergine tracking-tight">
              Hello, {user?.fullName || 'Customer'}!
            </h2>
            <Badge status="CUSTOMER" />
          </div>
          <p className="text-xs text-neutral-500 mt-1">
            Welcome to your LoopPay self-service subscription and billing hub &bull; {user?.email}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="px-3 py-1.5 bg-silver-light rounded-xl border border-silver-border text-xs text-neutral-600 flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-status-success animate-pulse"></span>
            <span>Account Active</span>
          </div>
        </div>
      </div>

      {/* Main Grid: My Subscriptions & My Invoices Empty States */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Section 1: My Subscriptions Empty State */}
        <Card
          title="My Subscriptions"
          subtitle="Active software licenses and recurring payment schedules"
          action={
            <Button variant="outline" size="sm" disabled>
              New Plan
            </Button>
          }
        >
          <div className="py-12 px-4 text-center">
            <div className="w-14 h-14 rounded-2xl bg-silver-light border border-silver-border flex items-center justify-center mx-auto text-neutral-400 mb-4 shadow-2xs">
              <Repeat className="w-7 h-7 text-aubergine/40" />
            </div>

            <h4 className="text-base font-bold text-aubergine">No active subscriptions yet</h4>
            <p className="text-xs text-neutral-500 mt-1.5 max-w-sm mx-auto leading-relaxed">
              You haven't subscribed to any LoopPay software plans yet. When you enroll in a plan, your recurring billing details and quotation status will appear here.
            </p>

            <div className="mt-6 flex items-center justify-center gap-2">
              <Button
                variant="primary"
                size="sm"
                disabled
                icon={Sparkles}
              >
                Browse Plans (Phase 2)
              </Button>
            </div>
          </div>
        </Card>

        {/* Section 2: My Invoices Empty State */}
        <Card
          title="My Invoices & Receipts"
          subtitle="Download tax invoices and inspect payment settlements"
        >
          <div className="py-12 px-4 text-center">
            <div className="w-14 h-14 rounded-2xl bg-silver-light border border-silver-border flex items-center justify-center mx-auto text-neutral-400 mb-4 shadow-2xs">
              <FileText className="w-7 h-7 text-aubergine/40" />
            </div>

            <h4 className="text-base font-bold text-aubergine">No invoices generated yet</h4>
            <p className="text-xs text-neutral-500 mt-1.5 max-w-sm mx-auto leading-relaxed">
              Every billing cycle generates an idempotent invoice tied to your subscription period. Once billing begins, your downloadable invoices will be listed here.
            </p>

            <div className="mt-6 inline-flex items-center gap-1.5 text-xs text-neutral-400 bg-silver-light px-3 py-1.5 rounded-lg border border-silver-border">
              <Clock className="w-3.5 h-3.5" />
              <span>Next billing run pending plan activation</span>
            </div>
          </div>
        </Card>
      </div>

      {/* Account Security Information Card */}
      <Card title="Account Security & Rules" subtitle="System compliance details">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div className="p-4 bg-silver-light rounded-xl border border-silver-border/80">
            <div className="flex items-center gap-2 font-semibold text-aubergine mb-1">
              <ShieldCheck className="w-4 h-4 text-status-success" />
              <span>Protected Identity</span>
            </div>
            <p className="text-[11px] text-neutral-500">
              Your account utilizes stateless JWT authentication with bcrypt salted passwords.
            </p>
          </div>

          <div className="p-4 bg-silver-light rounded-xl border border-silver-border/80">
            <div className="flex items-center gap-2 font-semibold text-aubergine mb-1">
              <CreditCard className="w-4 h-4 text-marigold" />
              <span>Idempotent Billing</span>
            </div>
            <p className="text-[11px] text-neutral-500">
              Guaranteed single-charge constraint prevents duplicate invoices on recurring billing periods.
            </p>
          </div>

          <div className="p-4 bg-silver-light rounded-xl border border-silver-border/80">
            <div className="flex items-center gap-2 font-semibold text-aubergine mb-1">
              <Repeat className="w-4 h-4 text-aubergine" />
              <span>Lifecycle Transparency</span>
            </div>
            <p className="text-[11px] text-neutral-500">
              Status tracking across DRAFT &bull; QUOTATION &bull; CONFIRMED &bull; ACTIVE &bull; CLOSED.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default CustomerPortal;

