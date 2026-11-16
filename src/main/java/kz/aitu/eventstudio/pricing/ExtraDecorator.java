package kz.aitu.eventstudio.pricing;

import kz.aitu.eventstudio.domain.Extra;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record ExtraDecorator(PriceComponent wrapped, Extra extra) implements PriceComponent {
    @Override
    public BigDecimal unitPrice() { return wrapped.unitPrice().add(extra.price()); }

    @Override
    public List<String> descriptions() {
        var result = new ArrayList<>(wrapped.descriptions());
        result.add(extra.toString());
        return List.copyOf(result);
    }
}
