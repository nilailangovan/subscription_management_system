/**
 * Date and invoice helpers for the finance dashboard.
 * All dates are ISO "yyyy-mm-dd" strings, which compare correctly as plain text.
 */

const DAY_MS = 24 * 60 * 60 * 1000;

/** Today's date in the browser's local timezone, e.g. "2026-10-31". */
export const todayISO = () => new Date().toLocaleDateString('en-CA');

/** Moves an ISO date by a number of days (negative goes back). */
export const shiftDays = (isoDate, days) =>
  new Date(Date.parse(isoDate) + days * DAY_MS).toISOString().slice(0, 10);

/** Whole days from `fromDate` to `toDate` (positive when toDate is later). */
export const daysBetween = (fromDate, toDate) => Math.round((Date.parse(toDate) - Date.parse(fromDate)) / DAY_MS);

/** True when `date` falls between `from` and `to`, both inclusive; an empty bound means no limit on that side. */
export const isWithinRange = (date, from, to) => (!from || date >= from) && (!to || date <= to);

/**
 * Payment state of an invoice as of `today`.
 * DRAFT and PAID come straight from InvoiceStatus; a CONFIRMED (sent, unpaid) invoice is
 * OVERDUE once its due date has passed, otherwise OUTSTANDING.
 */
export const getInvoiceState = (invoice, today) => {
  if (invoice.status !== 'CONFIRMED') return invoice.status;
  return invoice.dueDate < today ? 'OVERDUE' : 'OUTSTANDING';
};

/** Sums the `amount` field of a list of invoices or payments. */
export const sumAmounts = (rows) => rows.reduce((total, row) => total + Number(row.amount), 0);
