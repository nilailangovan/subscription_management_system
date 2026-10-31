/**
 * Shared money formatting. Currency and locale are configurable through Vite env vars
 * (VITE_CURRENCY, VITE_LOCALE) and default to Indian Rupees, e.g. 1999 -> "₹1,999.00".
 */
export const CURRENCY = import.meta.env.VITE_CURRENCY || 'INR';
const LOCALE = import.meta.env.VITE_LOCALE || 'en-IN';

const moneyFormatter = new Intl.NumberFormat(LOCALE, { style: 'currency', currency: CURRENCY });

/** Formats a BigDecimal/number from the backend as a currency string. */
export const formatMoney = (value) => moneyFormatter.format(Number(value) || 0);

/** Billing period enum -> unit word, e.g. MONTHLY -> "month". */
export const periodUnit = (billingPeriod) =>
  ({ DAILY: 'day', WEEKLY: 'week', MONTHLY: 'month', YEARLY: 'year' })[billingPeriod] || billingPeriod?.toLowerCase();
