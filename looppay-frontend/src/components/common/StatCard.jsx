import React from 'react';
import Card from './Card';
import { TrendingUp, TrendingDown } from 'lucide-react';

/**
 * StatCard component for KPI metrics on dashboards.
 * tone="danger" switches the card to an alert style (red accent, icon and value).
 */
const StatCard = ({
  title,
  value,
  change,
  isPositive = true,
  icon: Icon,
  helperText,
  tone = 'default',
  className = ''
}) => {
  const isDanger = tone === 'danger';

  return (
    <Card
      className={`hover:border-silver-dark/80 transition-shadow ${
        isDanger ? 'border-l-4 border-l-status-danger ring-1 ring-status-danger/20' : ''
      } ${className}`}
      bodyClassName="p-5"
    >
      <div className="flex items-center justify-between">
        <p className={`text-xs font-semibold uppercase tracking-wider ${isDanger ? 'text-status-danger' : 'text-neutral-500'}`}>
          {title}
        </p>
        {Icon && (
          <div
            className={`w-10 h-10 rounded-xl flex items-center justify-center ${
              isDanger ? 'bg-status-danger text-white' : 'bg-aubergine-50 text-aubergine'
            }`}
          >
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      <div className="mt-3 flex items-baseline justify-between gap-2">
        <h3 className={`text-2xl font-bold ${isDanger ? 'text-status-danger' : 'text-aubergine'}`}>{value}</h3>
        {change && (
          <div
            className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-md ${
              isPositive
                ? 'bg-status-success-bg text-status-success'
                : 'bg-status-danger-bg text-status-danger'
            }`}
          >
            {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
            <span>{change}</span>
          </div>
        )}
      </div>

      {helperText && (
        <p className={`text-xs mt-2 ${isDanger ? 'text-status-danger font-medium' : 'text-neutral-400'}`}>{helperText}</p>
      )}
    </Card>
  );
};

export default StatCard;

