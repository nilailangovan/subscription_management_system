package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineRequest;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;

import java.util.List;

/**
 * Pricing engine interface for calculating taxes, discounts, and totals
 * for line items across subscriptions and quotations.
 *
 * NOTE: Do not change this interface contract as other modules depend on it.
 */
public interface PricingService {

    /**
     * Calculates line-level and total pricing for given line items.
     *
     * @param lines list of pricing line requests (productId, quantity, unitPrice, taxId)
     * @return PricingResult with per-line tax and discount, subtotal, totalTax, totalDiscount, grandTotal
     */
    PricingResult calculate(List<PricingLineRequest> lines);
}

