package kz.aitu.eventstudio.domain;

public record Booking(String id, String eventId, String participantId, int seats, BookingStatus status) {
    public Booking {
        id = Checks.id(id);
        eventId = Checks.id(eventId);
        participantId = Checks.id(participantId);
        Checks.range(seats, "Seats", 1, 500);
        Checks.required(status, "Booking status");
    }
}
