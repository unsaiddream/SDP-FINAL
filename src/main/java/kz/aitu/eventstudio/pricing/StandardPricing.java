package kz.aitu.eventstudio.pricing;

import java.math.BigDecimal;

public final class StandardPricing implements PricingStrategy {
    @Override
    public BigDecimal discountRate(int guests) { return BigDecimal.ZERO; }

    @Override
    public String description() { return "Standard rate"; }
}
