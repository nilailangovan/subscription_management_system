import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import Button from './Button';

/**
 * Loading, empty and error states shared by list pages.
 */

/** Grey pulsing placeholder block. */
export const Skeleton = ({ className = '' }) => (
  <div className={`animate-pulse rounded-lg bg-silver ${className}`} aria-hidden="true" />
);

/** Placeholder table shown while a list loads. */
export const SkeletonTable = ({ rows = 4, cols = 5 }) => (
  <div className="bg-white rounded-xl shadow-soft border border-silver-border/80 overflow-hidden" role="status" aria-label="Loading">
    <div className="bg-silver-light px-5 py-3.5 border-b border-silver-border">
      <Skeleton className="h-3 w-40" />
    </div>
    <div className="divide-y divide-silver-border/60">
      {Array.from({ length: rows }).map((_, row) => (
        <div key={row} className="flex gap-6 px-5 py-4">
          {Array.from({ length: cols }).map((__, col) => (
            <Skeleton key={col} className={`h-4 ${col === 0 ? 'w-32' : 'flex-1'}`} />
          ))}
        </div>
      ))}
    </div>
  </div>
);

/** Centered message for a list with no records. */
export const EmptyState = ({ icon: Icon, title, message, action }) => (
  <div className="py-10 px-4 text-center">
    {Icon && (
      <div className="w-12 h-12 rounded-2xl bg-silver-light border border-silver-border flex items-center justify-center mx-auto mb-3">
        <Icon className="w-6 h-6 text-aubergine/40" />
      </div>
    )}
    <p className="text-sm font-bold text-aubergine">{title}</p>
    {message && <p className="text-xs text-neutral-500 mt-1 max-w-sm mx-auto">{message}</p>}
    {action && <div className="mt-4">{action}</div>}
  </div>
);

/** Error box with an optional retry button. */
export const ErrorState = ({ message, onRetry }) => (
  <div role="alert" className="p-4 bg-status-danger-bg border border-status-danger/30 rounded-xl flex items-start gap-3">
    <AlertCircle className="w-5 h-5 text-status-danger flex-shrink-0 mt-0.5" />
    <div className="flex-1 text-xs text-status-danger">
      <p className="font-semibold">{message}</p>
    </div>
    {onRetry && (
      <Button variant="outline" size="sm" icon={RefreshCw} onClick={onRetry}>
        Retry
      </Button>
    )}
  </div>
);
