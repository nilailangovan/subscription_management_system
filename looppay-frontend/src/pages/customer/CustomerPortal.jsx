import React from 'react';
import { Repeat, CreditCard, ShieldCheck } from 'lucide-react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import { useAuth } from '../../context/AuthContext';
import AvailablePlans from './AvailablePlans';
import { RecentSubscriptions, RecentInvoices } from './PortalRecent';

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

      <AvailablePlans />

      {/* Latest records from the API */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentSubscriptions />
        <RecentInvoices />
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

