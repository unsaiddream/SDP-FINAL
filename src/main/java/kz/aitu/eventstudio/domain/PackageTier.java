package kz.aitu.eventstudio.domain;

import java.math.BigDecimal;

public enum PackageTier {
    ESSENTIAL("Essential", 3000), STUDIO("Studio", 5000), SIGNATURE("Signature", 8000);

    private final String label;
    private final BigDecimal price;

    PackageTier(String label, int price) {
        this.label = label;
        this.price = BigDecimal.valueOf(price);
    }

    public BigDecimal price() { return price; }

    @Override
    public String toString() { return label + " (" + price.toPlainString() + " KZT/guest)"; }
}
