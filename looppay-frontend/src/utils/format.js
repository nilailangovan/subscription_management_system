/**
 * Formats a BigDecimal/number from the backend as a currency string, e.g. 1999 -> "$1,999.00".
 */
export const formatMoney = (value) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value) || 0);
