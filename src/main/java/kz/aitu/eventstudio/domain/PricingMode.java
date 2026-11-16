package kz.aitu.eventstudio.domain;

public enum PricingMode {
    STANDARD("Standard rate"), GROUP("Group rate: 10% off at 10 guests");

    private final String label;

    PricingMode(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
