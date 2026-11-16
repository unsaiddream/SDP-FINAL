package kz.aitu.eventstudio.pricing;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal discountRate(int guests);
    String description();
}
