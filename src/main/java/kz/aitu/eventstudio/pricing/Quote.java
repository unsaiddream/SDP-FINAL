package kz.aitu.eventstudio.pricing;

import java.math.BigDecimal;
import java.util.List;

public record Quote(int guests, BigDecimal unitPrice, BigDecimal subtotal, BigDecimal discount,
                    BigDecimal total, String strategy, List<String> lines) {
    public Quote {
        lines = List.copyOf(lines);
    }
}
