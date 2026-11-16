package kz.aitu.eventstudio.domain;

import java.math.BigDecimal;

public enum Extra {
    CATERING("Catering", 2500), PHOTOGRAPHY("Photography", 1200), TRANSPORT("Transport", 1500);

    private final String label;
    private final BigDecimal price;

    Extra(String label, int price) {
        this.label = label;
        this.price = BigDecimal.valueOf(price);
    }

    public BigDecimal price() { return price; }
    public String label() { return label; }

    @Override
    public String toString() { return label + " (+" + price.toPlainString() + " KZT/guest)"; }
}
