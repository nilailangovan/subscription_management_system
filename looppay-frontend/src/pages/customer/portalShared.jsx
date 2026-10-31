import React from 'react';
import Badge from '../../components/common/Badge';

/**
 * Small pieces shared by the customer portal pages.
 */

/** Paid / Due / Overdue badge from an InvoiceResponseDto (status + backend-computed overdue flag). */
export const InvoiceStatusBadge = ({ invoice, size = 'sm' }) => {
  if (invoice.status === 'PAID') return <Badge variant="success" size={size}>Paid</Badge>;
  if (invoice.overdue) return <Badge variant="danger" size={size}>Overdue</Badge>;
  if (invoice.status === 'DRAFT') return <Badge variant="neutral" size={size}>Draft</Badge>;
  return <Badge variant="warning" size={size}>Due</Badge>;
};

/** Opens the print-ready invoice in a new tab, where the browser's "Save as PDF" downloads it. */
export const downloadInvoice = (invoiceId) => {
  window.open(`/portal/invoices/${invoiceId}/print`, '_blank', 'noopener');
};

/** Reads the Spring Page payload from an ApiResponseDto. */
export const pageOf = (response) => response.data.data;
