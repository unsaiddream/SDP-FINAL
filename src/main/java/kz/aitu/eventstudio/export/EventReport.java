package kz.aitu.eventstudio.export;

import kz.aitu.eventstudio.domain.Event;
import kz.aitu.eventstudio.pricing.Quote;
import java.util.List;

public record EventReport(Event event, Quote quote, List<GuestRow> guests) {
    public EventReport { guests = List.copyOf(guests); }
    public record GuestRow(String name, String email, int seats, String status) {}
}
