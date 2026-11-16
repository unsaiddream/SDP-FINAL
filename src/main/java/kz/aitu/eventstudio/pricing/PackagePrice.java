package kz.aitu.eventstudio.pricing;

import kz.aitu.eventstudio.domain.PackageTier;
import java.math.BigDecimal;
import java.util.List;

public record PackagePrice(PackageTier tier) implements PriceComponent {
    @Override
    public BigDecimal unitPrice() { return tier.price(); }

    @Override
    public List<String> descriptions() { return List.of(tier.toString()); }
}
