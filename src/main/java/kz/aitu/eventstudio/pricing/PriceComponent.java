package kz.aitu.eventstudio.pricing;

import java.math.BigDecimal;
import java.util.List;

public interface PriceComponent {
    BigDecimal unitPrice();
    List<String> descriptions();
}
