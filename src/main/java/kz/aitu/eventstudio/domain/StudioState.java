package kz.aitu.eventstudio.domain;

import java.util.List;

public record StudioState(int version, List<Event> events, List<Participant> participants, List<Booking> bookings) {
    public StudioState {
        if (version != 1) throw new ValidationException("Unsupported data version: " + version);
        events = List.copyOf(Checks.required(events, "Events"));
        participants = List.copyOf(Checks.required(participants, "Participants"));
        bookings = List.copyOf(Checks.required(bookings, "Bookings"));
        unique(events.stream().map(Event::id).toList());
        unique(participants.stream().map(Participant::id).toList());
        unique(bookings.stream().map(Booking::id).toList());
        unique(participants.stream().map(Participant::email).toList());
        for (Booking booking : bookings) {
            Event event = events.stream().filter(e -> e.id().equals(booking.eventId())).findFirst()
                    .orElseThrow(() -> new ValidationException("Booking refers to a missing event."));
            if (participants.stream().noneMatch(p -> p.id().equals(booking.participantId()))) {
                throw new ValidationException("Booking refers to a missing participant.");
            }
            if (booking.status() == BookingStatus.CONFIRMED && (event.status() == EventStatus.DRAFT || event.status() == EventStatus.CANCELLED)) {
                throw new ValidationException("Draft or cancelled events cannot have confirmed bookings.");
            }
        }
        for (Event event : events) {
            var confirmed = bookings.stream().filter(b -> b.eventId().equals(event.id()) && b.status() == BookingStatus.CONFIRMED).toList();
            if (confirmed.stream().mapToLong(Booking::seats).sum() > event.capacity()) throw new ValidationException("Bookings exceed event capacity.");
            unique(confirmed.stream().map(Booking::participantId).toList());
            if (event.status() == EventStatus.COMPLETED && confirmed.isEmpty()) throw new ValidationException("Completed events must have attendees.");
        }
    }

    private static void unique(List<String> values) {
        if (values.stream().distinct().count() != values.size()) throw new ValidationException("Duplicate identifier, email, or active booking.");
    }
}
