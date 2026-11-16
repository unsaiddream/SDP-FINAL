package kz.aitu.eventstudio.pricing;

import java.math.BigDecimal;

public final class GroupPricing implements PricingStrategy {
    @Override
    public BigDecimal discountRate(int guests) { return guests >= 10 ? new BigDecimal("0.10") : BigDecimal.ZERO; }

    @Override
    public String description() { return "Group rate: 10% off at 10 guests"; }
}
