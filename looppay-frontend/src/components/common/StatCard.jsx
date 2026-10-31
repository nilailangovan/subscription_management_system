import React from 'react';
import Card from './Card';
import { TrendingUp, TrendingDown } from 'lucide-react';

/**
 * StatCard component for KPI metrics on dashboards.
 */
const StatCard = ({
  title,
  value,
  change,
  isPositive = true,
  icon: Icon,
  helperText,
  className = ''
}) => {
  return (
    <Card className={`hover:border-silver-dark/80 transition-shadow ${className}`} bodyClassName="p-5">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold uppercase tracking-wider text-neutral-500">{title}</p>
        {Icon && (
          <div className="w-10 h-10 rounded-xl bg-aubergine-50 flex items-center justify-center text-aubergine">
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      <div className="mt-3 flex items-baseline justify-between gap-2">
        <h3 className="text-2xl font-bold text-aubergine">{value}</h3>
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
        <p className="text-xs text-neutral-400 mt-2">{helperText}</p>
      )}
    </Card>
  );
};

export default StatCard;

