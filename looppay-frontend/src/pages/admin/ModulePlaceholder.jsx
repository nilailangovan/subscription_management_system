import React from 'react';
import { Construction } from 'lucide-react';

/**
 * Landing page for console modules whose screens are not built yet
 * (Subscriptions, Invoices, Payments, Discounts, Taxes), so their sidebar links work.
 */
const ModulePlaceholder = ({ title, description }) => (
  <div className="space-y-6">
    <div className="bg-white p-6 rounded-2xl shadow-soft border border-silver-border">
      <h2 className="text-2xl font-bold text-aubergine tracking-tight">{title}</h2>
      <p className="text-xs text-neutral-500 mt-1">{description}</p>
    </div>

    <div className="bg-white py-14 px-6 rounded-2xl shadow-soft border border-silver-border text-center">
      <div className="w-14 h-14 rounded-2xl bg-silver-light border border-silver-border flex items-center justify-center mx-auto mb-4">
        <Construction className="w-7 h-7 text-aubergine/40" />
      </div>
      <h3 className="text-base font-bold text-aubergine">{title} module is coming next</h3>
      <p className="text-xs text-neutral-500 mt-1.5 max-w-sm mx-auto leading-relaxed">
        This screen will connect to the {title.toLowerCase()} APIs once the backend endpoints are available.
      </p>
    </div>
  </div>
);

export default ModulePlaceholder;
