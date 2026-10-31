/**
 * Mock data for building UI without the backend.
 * Shapes mirror the backend response DTOs (UserResponseDto, ProductResponseDto,
 * ProductVariantResponseDto, PlanResponseDto). Subscriptions, invoices and payments have no
 * backend DTO yet, so their shapes are what the finance dashboard needs.
 */
import { todayISO, shiftDays } from '../utils/finance';

export const mockUsers = [
  { id: 1, firstName: 'Asha', lastName: 'Raman', email: 'admin@looppay.com', role: 'ADMIN', active: true, createdAt: '2026-09-01T09:00:00' },
  { id: 2, firstName: 'Karthik', lastName: 'Selvam', email: 'karthik.staff@looppay.com', role: 'STAFF', active: true, createdAt: '2026-09-05T10:30:00' },
  { id: 3, firstName: 'Divya', lastName: 'Murugan', email: 'divya.staff@looppay.com', role: 'STAFF', active: false, createdAt: '2026-09-12T14:15:00' },
  { id: 4, firstName: 'Priya', lastName: 'Kumar', email: 'priya@example.com', role: 'CUSTOMER', active: true, createdAt: '2026-10-01T11:00:00' },
  { id: 5, firstName: 'Arun', lastName: 'Prakash', email: 'arun@example.com', role: 'CUSTOMER', active: true, createdAt: '2026-10-03T16:45:00' },
  { id: 6, firstName: 'Meena', lastName: 'Iyer', email: 'meena@example.com', role: 'CUSTOMER', active: true, createdAt: '2026-10-10T08:20:00' },
];

export const mockProducts = [
  { id: 1, name: 'LoopPay Cloud CRM', type: 'SOFTWARE', salesPrice: 1999, costPrice: 600, notes: 'Per-seat CRM license', active: true, variantsCount: 2, createdAt: '2026-09-02T09:00:00', updatedAt: '2026-09-02T09:00:00' },
  { id: 2, name: 'Managed IT Support', type: 'SERVICE', salesPrice: 4999, costPrice: 2500, notes: 'Remote helpdesk, business hours', active: true, variantsCount: 1, createdAt: '2026-09-03T09:00:00', updatedAt: '2026-09-20T12:00:00' },
  { id: 3, name: 'Office Water Delivery', type: 'GOODS', salesPrice: 799, costPrice: 400, notes: '20L cans, weekly drop', active: false, variantsCount: 0, createdAt: '2026-09-04T09:00:00', updatedAt: '2026-10-15T17:00:00' },
];

export const mockProductVariants = [
  { id: 1, productId: 1, productName: 'LoopPay Cloud CRM', attribute: 'Edition', value: 'Standard', baseSalesPrice: 1999, extraPrice: 0, finalPrice: 1999, active: true, createdAt: '2026-09-02T09:10:00' },
  { id: 2, productId: 1, productName: 'LoopPay Cloud CRM', attribute: 'Edition', value: 'Premium', baseSalesPrice: 1999, extraPrice: 1000, finalPrice: 2999, active: true, createdAt: '2026-09-02T09:12:00' },
  { id: 3, productId: 2, productName: 'Managed IT Support', attribute: 'Coverage', value: '24x7', baseSalesPrice: 4999, extraPrice: 2500, finalPrice: 7499, active: true, createdAt: '2026-09-03T09:10:00' },
];

export const mockPlans = [
  { id: 1, name: 'CRM Monthly', productId: 1, productName: 'LoopPay Cloud CRM', price: 1999, billingPeriod: 'MONTHLY', minQuantity: 1, startDate: '2026-09-01', endDate: '2027-08-31', autoClose: false, closable: true, pausable: true, renewable: true, status: 'ACTIVE', active: true, createdAt: '2026-09-02T10:00:00', updatedAt: '2026-09-02T10:00:00' },
  { id: 2, name: 'CRM Yearly', productId: 1, productName: 'LoopPay Cloud CRM', price: 19999, billingPeriod: 'YEARLY', minQuantity: 5, startDate: '2026-09-01', endDate: '2027-08-31', autoClose: true, closable: true, pausable: false, renewable: true, status: 'ACTIVE', active: true, createdAt: '2026-09-02T10:05:00', updatedAt: '2026-09-02T10:05:00' },
  { id: 3, name: 'IT Support Weekly', productId: 2, productName: 'Managed IT Support', price: 1499, billingPeriod: 'WEEKLY', minQuantity: 1, startDate: '2026-09-15', endDate: '2026-12-31', autoClose: false, closable: true, pausable: true, renewable: false, status: 'PAUSED', active: true, createdAt: '2026-09-15T10:00:00', updatedAt: '2026-10-20T09:00:00' },
  { id: 4, name: 'Water Daily Trial', productId: 3, productName: 'Office Water Delivery', price: 99, billingPeriod: 'DAILY', minQuantity: 1, startDate: '2026-09-04', endDate: '2026-10-04', autoClose: true, closable: true, pausable: false, renewable: false, status: 'CLOSED', active: false, createdAt: '2026-09-04T10:00:00', updatedAt: '2026-10-04T23:59:00' },
];

// Finance dates are relative to today so overdue and "this month" figures stay realistic whenever the demo runs
const today = todayISO();
const day = (offset) => shiftDays(today, offset);

// Subscription statuses: ACTIVE / PAUSED / CLOSED. recurringValue = plan price x quantity per billing period.
export const mockSubscriptions = [
  { id: 'SUB-1001', customerName: 'Priya Kumar', customerEmail: 'priya@example.com', planName: 'CRM Monthly', recurringValue: 5997, billingPeriod: 'MONTHLY', nextBillingDate: day(1), status: 'ACTIVE', updatedAt: day(-2) },
  { id: 'SUB-1002', customerName: 'Arun Prakash', customerEmail: 'arun@example.com', planName: 'CRM Yearly', recurringValue: 99995, billingPeriod: 'YEARLY', nextBillingDate: day(184), status: 'ACTIVE', updatedAt: day(-5) },
  { id: 'SUB-1003', customerName: 'Meena Iyer', customerEmail: 'meena@example.com', planName: 'IT Support Weekly', recurringValue: 1499, billingPeriod: 'WEEKLY', nextBillingDate: null, status: 'PAUSED', updatedAt: day(-1) },
  { id: 'SUB-1004', customerName: 'Rahul Verma', customerEmail: 'rahul.verma@brightmart.in', planName: 'CRM Monthly', recurringValue: 9995, billingPeriod: 'MONTHLY', nextBillingDate: day(12), status: 'ACTIVE', updatedAt: day(-8) },
  { id: 'SUB-1005', customerName: 'Sofia Martins', customerEmail: 'sofia@northwind.io', planName: 'IT Support Weekly', recurringValue: 2998, billingPeriod: 'WEEKLY', nextBillingDate: day(3), status: 'ACTIVE', updatedAt: day(-3) },
  { id: 'SUB-1006', customerName: 'Daniel Okafor', customerEmail: 'daniel@okaforlogistics.com', planName: 'CRM Monthly', recurringValue: 1999, billingPeriod: 'MONTHLY', nextBillingDate: null, status: 'CLOSED', updatedAt: day(-12) },
  { id: 'SUB-1007', customerName: 'Aisha Rahman', customerEmail: 'aisha@lumenstudio.co', planName: 'CRM Monthly', recurringValue: 7996, billingPeriod: 'MONTHLY', nextBillingDate: day(16), status: 'ACTIVE', updatedAt: day(-20) },
  { id: 'SUB-1008', customerName: 'Kenji Watanabe', customerEmail: 'kenji@sakuraretail.jp', planName: 'CRM Monthly', recurringValue: 3998, billingPeriod: 'MONTHLY', nextBillingDate: null, status: 'PAUSED', updatedAt: day(-26) },
  { id: 'SUB-1009', customerName: 'Lucas Moreau', customerEmail: 'lucas@moreau-design.fr', planName: 'IT Support Weekly', recurringValue: 1499, billingPeriod: 'WEEKLY', nextBillingDate: day(5), status: 'ACTIVE', updatedAt: day(-40) },
  { id: 'SUB-1010', customerName: 'Emily Carter', customerEmail: 'emily@carterlegal.com', planName: 'CRM Monthly', recurringValue: 7996, billingPeriod: 'MONTHLY', nextBillingDate: null, status: 'CLOSED', updatedAt: day(-75) },
];

// InvoiceStatus: DRAFT / CONFIRMED (sent, unpaid) / PAID. A CONFIRMED invoice past its dueDate is overdue.
export const mockInvoices = [
  { id: 1, invoiceNumber: 'INV-2026-0090', subscriptionId: 'SUB-1010', customerName: 'Emily Carter', customerEmail: 'emily@carterlegal.com', amount: 7996, issueDate: day(-80), dueDate: day(-65), status: 'PAID' },
  { id: 2, invoiceNumber: 'INV-2026-0098', subscriptionId: 'SUB-1006', customerName: 'Daniel Okafor', customerEmail: 'daniel@okaforlogistics.com', amount: 1999, issueDate: day(-70), dueDate: day(-55), status: 'CONFIRMED' },
  { id: 3, invoiceNumber: 'INV-2026-0101', subscriptionId: 'SUB-1001', customerName: 'Priya Kumar', customerEmail: 'priya@example.com', amount: 5997, issueDate: day(-62), dueDate: day(-47), status: 'PAID' },
  { id: 4, invoiceNumber: 'INV-2026-0103', subscriptionId: 'SUB-1007', customerName: 'Aisha Rahman', customerEmail: 'aisha@lumenstudio.co', amount: 7996, issueDate: day(-45), dueDate: day(-30), status: 'PAID' },
  { id: 5, invoiceNumber: 'INV-2026-0109', subscriptionId: 'SUB-1004', customerName: 'Rahul Verma', customerEmail: 'rahul.verma@brightmart.in', amount: 9995, issueDate: day(-45), dueDate: day(-30), status: 'CONFIRMED' },
  { id: 6, invoiceNumber: 'INV-2026-0112', subscriptionId: 'SUB-1005', customerName: 'Sofia Martins', customerEmail: 'sofia@northwind.io', amount: 2998, issueDate: day(-24), dueDate: day(-17), status: 'CONFIRMED' },
  { id: 7, invoiceNumber: 'INV-2026-0115', subscriptionId: 'SUB-1009', customerName: 'Lucas Moreau', customerEmail: 'lucas@moreau-design.fr', amount: 1499, issueDate: day(-13), dueDate: day(-6), status: 'CONFIRMED' },
  { id: 8, invoiceNumber: 'INV-2026-0118', subscriptionId: 'SUB-1001', customerName: 'Priya Kumar', customerEmail: 'priya@example.com', amount: 5997, issueDate: day(-31), dueDate: day(-16), status: 'PAID' },
  { id: 9, invoiceNumber: 'INV-2026-0121', subscriptionId: 'SUB-1007', customerName: 'Aisha Rahman', customerEmail: 'aisha@lumenstudio.co', amount: 7996, issueDate: day(-20), dueDate: day(-5), status: 'CONFIRMED' },
  { id: 10, invoiceNumber: 'INV-2026-0124', subscriptionId: 'SUB-1008', customerName: 'Kenji Watanabe', customerEmail: 'kenji@sakuraretail.jp', amount: 3998, issueDate: day(-28), dueDate: day(-13), status: 'PAID' },
  { id: 11, invoiceNumber: 'INV-2026-0127', subscriptionId: 'SUB-1004', customerName: 'Rahul Verma', customerEmail: 'rahul.verma@brightmart.in', amount: 9995, issueDate: day(-15), dueDate: day(0), status: 'CONFIRMED' },
  { id: 12, invoiceNumber: 'INV-2026-0129', subscriptionId: 'SUB-1005', customerName: 'Sofia Martins', customerEmail: 'sofia@northwind.io', amount: 2998, issueDate: day(-10), dueDate: day(-3), status: 'PAID' },
  { id: 13, invoiceNumber: 'INV-2026-0131', subscriptionId: 'SUB-1009', customerName: 'Lucas Moreau', customerEmail: 'lucas@moreau-design.fr', amount: 1499, issueDate: day(-6), dueDate: day(1), status: 'CONFIRMED' },
  { id: 14, invoiceNumber: 'INV-2026-0135', subscriptionId: 'SUB-1001', customerName: 'Priya Kumar', customerEmail: 'priya@example.com', amount: 5997, issueDate: day(-1), dueDate: day(14), status: 'CONFIRMED' },
  { id: 15, invoiceNumber: 'INV-2026-0137', subscriptionId: 'SUB-1002', customerName: 'Arun Prakash', customerEmail: 'arun@example.com', amount: 99995, issueDate: day(0), dueDate: day(30), status: 'DRAFT' },
];

// One payment per PAID invoice.
export const mockPayments = [
  { id: 'PAY-5001', invoiceNumber: 'INV-2026-0090', customerName: 'Emily Carter', customerEmail: 'emily@carterlegal.com', amount: 7996, method: 'Bank Transfer', paidAt: day(-66) },
  { id: 'PAY-5002', invoiceNumber: 'INV-2026-0101', customerName: 'Priya Kumar', customerEmail: 'priya@example.com', amount: 5997, method: 'Card', paidAt: day(-50) },
  { id: 'PAY-5003', invoiceNumber: 'INV-2026-0103', customerName: 'Aisha Rahman', customerEmail: 'aisha@lumenstudio.co', amount: 7996, method: 'Bank Transfer', paidAt: day(-30) },
  { id: 'PAY-5004', invoiceNumber: 'INV-2026-0118', customerName: 'Priya Kumar', customerEmail: 'priya@example.com', amount: 5997, method: 'Card', paidAt: day(-18) },
  { id: 'PAY-5005', invoiceNumber: 'INV-2026-0124', customerName: 'Kenji Watanabe', customerEmail: 'kenji@sakuraretail.jp', amount: 3998, method: 'UPI', paidAt: day(-14) },
  { id: 'PAY-5006', invoiceNumber: 'INV-2026-0129', customerName: 'Sofia Martins', customerEmail: 'sofia@northwind.io', amount: 2998, method: 'Card', paidAt: day(-4) },
];
