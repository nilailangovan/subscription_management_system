// Run with: node --test src/utils/finance.test.js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { shiftDays, daysBetween, isWithinRange, getInvoiceState, sumAmounts } from './finance.js';

test('shiftDays crosses month and year boundaries', () => {
  assert.equal(shiftDays('2026-10-31', 1), '2026-11-01');
  assert.equal(shiftDays('2026-01-01', -1), '2025-12-31');
});

test('daysBetween counts whole days', () => {
  assert.equal(daysBetween('2026-10-01', '2026-10-31'), 30);
  assert.equal(daysBetween('2026-10-31', '2026-10-31'), 0);
});

test('isWithinRange includes both ends and treats empty bounds as open', () => {
  assert.equal(isWithinRange('2026-10-01', '2026-10-01', '2026-10-31'), true);
  assert.equal(isWithinRange('2026-10-31', '2026-10-01', '2026-10-31'), true);
  assert.equal(isWithinRange('2026-09-30', '2026-10-01', '2026-10-31'), false);
  assert.equal(isWithinRange('2026-11-01', '2026-10-01', '2026-10-31'), false);
  assert.equal(isWithinRange('2020-01-01', '', '2026-10-31'), true);
  assert.equal(isWithinRange('2030-01-01', '2026-10-01', ''), true);
  // From after To matches nothing
  assert.equal(isWithinRange('2026-10-15', '2026-10-31', '2026-10-01'), false);
});

test('getInvoiceState splits CONFIRMED into outstanding and overdue', () => {
  const today = '2026-10-31';
  assert.equal(getInvoiceState({ status: 'PAID', dueDate: '2026-10-01' }, today), 'PAID');
  assert.equal(getInvoiceState({ status: 'DRAFT', dueDate: '2026-10-01' }, today), 'DRAFT');
  assert.equal(getInvoiceState({ status: 'CONFIRMED', dueDate: '2026-10-30' }, today), 'OVERDUE');
  // Due today is not overdue yet
  assert.equal(getInvoiceState({ status: 'CONFIRMED', dueDate: '2026-10-31' }, today), 'OUTSTANDING');
});

test('sumAmounts handles numbers and numeric strings', () => {
  assert.equal(sumAmounts([{ amount: 10.5 }, { amount: '4.5' }]), 15);
  assert.equal(sumAmounts([]), 0);
});
