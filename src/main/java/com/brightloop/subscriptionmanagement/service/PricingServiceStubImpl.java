package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineRequest;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingLineResult;
import com.brightloop.subscriptionmanagement.dto.pricing.PricingResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary stub implementation of PricingService (zero tax, zero discount).
 * This allows all modules to compile and run while the full pricing engine is being built.
 */
@Service
public class PricingServiceStubImpl implements PricingService {

    @Override
    public PricingResult calculate(List<PricingLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            return PricingResult.builder()
                    .lineResults(new ArrayList<>())
                    .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalTax(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .grandTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }

        List<PricingLineResult> lineResults = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (PricingLineRequest line : lines) {
            BigDecimal qty = BigDecimal.valueOf(line.getQuantity() != null ? line.getQuantity() : 1);
            BigDecimal unitPrice = line.getUnitPrice() != null ? line.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);

            lineResults.add(PricingLineResult.builder()
                    .productId(line.getProductId())
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .taxId(line.getTaxId())
                    .taxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .lineTotal(lineTotal)
                    .build());
        }

        return PricingResult.builder()
                .lineResults(lineResults)
                .subtotal(subtotal)
                .totalTax(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .grandTotal(subtotal)
                .build();
    }
}

