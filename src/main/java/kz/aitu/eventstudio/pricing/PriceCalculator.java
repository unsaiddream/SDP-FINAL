package kz.aitu.eventstudio.pricing;

import kz.aitu.eventstudio.domain.Checks;
import kz.aitu.eventstudio.domain.Event;
import kz.aitu.eventstudio.domain.PricingMode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {
    public Quote calculate(Event event, int guests) {
        PricingStrategy strategy = event.pricingMode() == PricingMode.GROUP ? new GroupPricing() : new StandardPricing();
        return calculate(event, guests, strategy);
    }

    public Quote calculate(Event event, int guests, PricingStrategy strategy) {
        Checks.range(guests, "Guests", 0, 500);
        PriceComponent component = new PackagePrice(event.packageTier());
        for (var extra : event.extras()) component = new ExtraDecorator(component, extra);
        BigDecimal subtotal = component.unitPrice().multiply(BigDecimal.valueOf(guests)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = subtotal.multiply(strategy.discountRate(guests)).setScale(2, RoundingMode.HALF_UP);
        return new Quote(guests, component.unitPrice(), subtotal, discount, subtotal.subtract(discount), strategy.description(), component.descriptions());
    }
}
