import React from 'react';

/**
 * Status and role badge component.
 * Adheres to LoopPay status colors & contrast rules.
 */
const Badge = ({
  children,
  variant, // 'success' | 'danger' | 'warning' | 'neutral' | 'aubergine' | 'marigold'
  status,  // maps SubscriptionStatus or InvoiceStatus or Role directly
  size = 'md', // 'sm' | 'md'
  className = ''
}) => {
  // Determine variant based on status if variant not explicitly provided
  let activeVariant = variant;

  if (!activeVariant && status) {
    const s = String(status).toUpperCase();
    switch (s) {
      case 'ACTIVE':
      case 'PAID':
      case 'CONFIRMED':
        activeVariant = 'success';
        break;
      case 'QUOTATION':
      case 'PENDING':
        activeVariant = 'warning';
        break;
      case 'DRAFT':
        activeVariant = 'neutral';
        break;
      case 'CLOSED':
      case 'CANCELLED':
      case 'FAILED':
        activeVariant = 'danger';
        break;
      case 'ADMIN':
        activeVariant = 'aubergine';
        break;
      case 'STAFF':
        activeVariant = 'marigold';
        break;
      case 'CUSTOMER':
        activeVariant = 'neutral';
        break;
      default:
        activeVariant = 'neutral';
    }
  }

  const variants = {
    // success: #2E7D5B with subtle background
    success: 'bg-status-success-bg text-status-success border border-status-success/20',
    // danger: #B3261E with subtle background
    danger: 'bg-status-danger-bg text-status-danger border border-status-danger/20',
    // warning: Marigold background with Aubergine text (contrast rule)
    warning: 'bg-marigold text-aubergine font-bold border border-marigold/30 shadow-2xs',
    // neutral: #6B7280 with gray background
    neutral: 'bg-status-neutral-bg text-status-neutral border border-gray-300',
    // aubergine: dark purple badge with white text
    aubergine: 'bg-aubergine text-white border border-aubergine',
    // marigold: highlight badge
    marigold: 'bg-marigold text-aubergine font-bold border border-marigold/40'
  };

  const sizes = {
    sm: 'text-[11px] px-2 py-0.5 rounded-md font-medium tracking-wide',
    md: 'text-xs px-2.5 py-1 rounded-lg font-semibold tracking-wide'
  };

  const content = children || status;

  return (
    <span
      className={`inline-flex items-center justify-center uppercase transition duration-150 ${variants[activeVariant] || variants.neutral} ${sizes[size]} ${className}`}
    >
      {content}
    </span>
  );
};

export default Badge;

