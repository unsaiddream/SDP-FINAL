package kz.aitu.eventstudio.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

public record Event(String id, String title, String date, String venue, int capacity,
                    PackageTier packageTier, List<Extra> extras, PricingMode pricingMode, EventStatus status) {
    public Event {
        id = Checks.id(id);
        title = Checks.text(title, "Event title", 100);
        venue = Checks.text(venue, "Venue", 120);
        date = Checks.text(date, "Date", 10);
        try {
            if (!LocalDate.parse(date).toString().equals(date)) throw new DateTimeParseException("Invalid", date, 0);
        } catch (DateTimeParseException exception) {
            throw new ValidationException("Date must be a real date in YYYY-MM-DD format.");
        }
        Checks.range(capacity, "Capacity", 1, 500);
        Checks.required(packageTier, "Package");
        Checks.required(pricingMode, "Pricing mode");
        Checks.required(status, "Status");
        Checks.required(extras, "Extras");
        if (extras.stream().anyMatch(java.util.Objects::isNull)) throw new ValidationException("Invalid extra.");
        if (extras.stream().distinct().count() != extras.size()) throw new ValidationException("Extras must be unique.");
        extras = List.copyOf(extras);
    }

    public Builder toBuilder() {
        return new Builder().id(id).title(title).date(date).venue(venue).capacity(capacity)
                .packageTier(packageTier).extras(extras).pricingMode(pricingMode).status(status);
    }

    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private String title;
        private String date;
        private String venue;
        private int capacity = 20;
        private PackageTier packageTier = PackageTier.ESSENTIAL;
        private List<Extra> extras = List.of();
        private PricingMode pricingMode = PricingMode.STANDARD;
        private EventStatus status = EventStatus.DRAFT;

        public Builder id(String value) { id = value; return this; }
        public Builder title(String value) { title = value; return this; }
        public Builder date(String value) { date = value; return this; }
        public Builder venue(String value) { venue = value; return this; }
        public Builder capacity(int value) { capacity = value; return this; }
        public Builder packageTier(PackageTier value) { packageTier = value; return this; }
        public Builder extras(List<Extra> value) { extras = value; return this; }
        public Builder pricingMode(PricingMode value) { pricingMode = value; return this; }
        public Builder status(EventStatus value) { status = value; return this; }

        public Event build() {
            return new Event(id, title, date, venue, capacity, packageTier, extras, pricingMode, status);
        }
    }
}
