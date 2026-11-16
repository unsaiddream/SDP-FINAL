package kz.aitu.eventstudio.service;

import kz.aitu.eventstudio.domain.*;
import kz.aitu.eventstudio.events.EventBus;
import kz.aitu.eventstudio.events.StudioEvent;
import kz.aitu.eventstudio.export.EventReport;
import kz.aitu.eventstudio.export.ExportCreator;
import kz.aitu.eventstudio.pricing.PriceCalculator;
import kz.aitu.eventstudio.pricing.Quote;
import kz.aitu.eventstudio.storage.StudioRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StudioFacade implements AutoCloseable {
    private final StudioRepository repository;
    private final EventBus eventBus;
    private final Clock clock;
    private final PriceCalculator calculator = new PriceCalculator();
    private StudioState state;

    public StudioFacade(StudioRepository repository, EventBus eventBus, Clock clock) throws IOException {
        this.repository = repository;
        this.eventBus = eventBus;
        this.clock = clock;
        var saved = repository.load();
        state = saved.orElseGet(() -> SampleData.create(clock));
        if (saved.isEmpty()) repository.save(state);
    }

    public synchronized StudioState snapshot() { return state; }

    public synchronized Event event(String id) {
        return state.events().stream().filter(e -> e.id().equals(id)).findFirst()
                .orElseThrow(() -> new ValidationException("Select an existing event."));
    }

    public synchronized Participant participant(String id) {
        return state.participants().stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ValidationException("Select an existing participant."));
    }

    public synchronized Booking booking(String id) {
        return state.bookings().stream().filter(b -> b.id().equals(id)).findFirst()
                .orElseThrow(() -> new ValidationException("Select an existing booking."));
    }

    public synchronized int confirmedSeats(String eventId) {
        return state.bookings().stream().filter(b -> b.eventId().equals(eventId) && b.status() == BookingStatus.CONFIRMED)
                .mapToInt(Booking::seats).sum();
    }

    public synchronized Quote quote(String eventId) {
        return calculator.calculate(event(eventId), confirmedSeats(eventId));
    }

    public synchronized Event saveEvent(Event candidate) throws IOException {
        Event existing = state.events().stream().filter(e -> e.id().equals(candidate.id())).findFirst().orElse(null);
        if (existing == null && candidate.status() != EventStatus.DRAFT) throw new ValidationException("New events must start as drafts.");
        if (existing != null) {
            editable(existing);
            if (candidate.status() != existing.status()) throw new ValidationException("Use the status action to open, complete, or cancel an event.");
        }
        future(candidate);
        var events = new ArrayList<>(state.events());
        events.removeIf(e -> e.id().equals(candidate.id()));
        events.add(candidate);
        commit(new StudioState(1, events, state.participants(), state.bookings()), "Saved event: " + candidate.title());
        return candidate;
    }

    public synchronized void changeStatus(String eventId, EventStatus target) throws IOException {
        Event existing = event(eventId);
        Checks.required(target, "Target status");
        boolean allowed = existing.status() == EventStatus.DRAFT && (target == EventStatus.OPEN || target == EventStatus.CANCELLED)
                || existing.status() == EventStatus.OPEN && (target == EventStatus.COMPLETED || target == EventStatus.CANCELLED);
        if (!allowed) throw new ValidationException("Allowed transitions: Draft to Open/Cancelled; Open to Completed/Cancelled.");
        if (target == EventStatus.OPEN) future(existing);
        if (target == EventStatus.COMPLETED && confirmedSeats(eventId) == 0) throw new ValidationException("Add at least one confirmed booking before completing an event.");
        Event changed = existing.toBuilder().status(target).build();
        var events = state.events().stream().map(e -> e.id().equals(eventId) ? changed : e).toList();
        var bookings = state.bookings().stream().map(b -> target == EventStatus.CANCELLED && b.eventId().equals(eventId)
                ? new Booking(b.id(), b.eventId(), b.participantId(), b.seats(), BookingStatus.CANCELLED) : b).toList();
        commit(new StudioState(1, events, state.participants(), bookings), "Event " + target.name().toLowerCase(java.util.Locale.ROOT) + ": " + existing.title());
    }

    public synchronized void deleteEvent(String id) throws IOException {
        Event existing = event(id);
        if (existing.status() != EventStatus.DRAFT) throw new ValidationException("Only draft events can be deleted. Cancel an open event to preserve its history.");
        if (state.bookings().stream().anyMatch(b -> b.eventId().equals(id))) throw new ValidationException("This event has booking history and cannot be deleted.");
        commit(new StudioState(1, state.events().stream().filter(e -> !e.id().equals(id)).toList(), state.participants(), state.bookings()), "Deleted draft: " + existing.title());
    }

    public synchronized Participant saveParticipant(String id, String name, String email) throws IOException {
        if (id != null) participant(id);
        Participant changed = new Participant(id == null ? UUID.randomUUID().toString() : id, name, email);
        if (state.participants().stream().anyMatch(p -> !p.id().equals(changed.id()) && p.email().equals(changed.email()))) {
            throw new ValidationException("A participant with this email already exists.");
        }
        var participants = new ArrayList<>(state.participants());
        participants.removeIf(p -> p.id().equals(changed.id()));
        participants.add(changed);
        commit(new StudioState(1, state.events(), participants, state.bookings()), "Saved participant: " + changed.name());
        return changed;
    }

    public synchronized void deleteParticipant(String id) throws IOException {
        Participant existing = participant(id);
        if (state.bookings().stream().anyMatch(b -> b.participantId().equals(id))) throw new ValidationException("This participant has booking history and cannot be deleted.");
        commit(new StudioState(1, state.events(), state.participants().stream().filter(p -> !p.id().equals(id)).toList(), state.bookings()), "Deleted participant: " + existing.name());
    }

    public synchronized Booking saveBooking(String id, String eventId, String participantId, int seats) throws IOException {
        Event event = event(eventId);
        if (event.status() != EventStatus.OPEN) throw new ValidationException("Bookings require an open event.");
        participant(participantId);
        if (id != null) {
            Booking existing = booking(id);
            if (!existing.eventId().equals(eventId) || existing.status() != BookingStatus.CONFIRMED) throw new ValidationException("Only confirmed bookings in this event can be edited.");
        }
        Booking changed = new Booking(id == null ? UUID.randomUUID().toString() : id, eventId, participantId, seats, BookingStatus.CONFIRMED);
        if (state.bookings().stream().anyMatch(b -> !b.id().equals(changed.id()) && b.eventId().equals(eventId)
                && b.participantId().equals(participantId) && b.status() == BookingStatus.CONFIRMED)) {
            throw new ValidationException("This participant already has a confirmed booking for this event.");
        }
        var bookings = new ArrayList<>(state.bookings());
        bookings.removeIf(b -> b.id().equals(changed.id()));
        bookings.add(changed);
        commit(new StudioState(1, state.events(), state.participants(), bookings), "Saved " + seats + " seats for " + participant(participantId).name());
        return changed;
    }

    public synchronized void cancelBooking(String id) throws IOException {
        Booking booking = booking(id);
        if (event(booking.eventId()).status() != EventStatus.OPEN || booking.status() != BookingStatus.CONFIRMED) {
            throw new ValidationException("Only a confirmed booking in an open event can be cancelled.");
        }
        var bookings = state.bookings().stream().map(b -> b.id().equals(id)
                ? new Booking(b.id(), b.eventId(), b.participantId(), b.seats(), BookingStatus.CANCELLED) : b).toList();
        commit(new StudioState(1, state.events(), state.participants(), bookings), "Cancelled booking for " + participant(booking.participantId()).name());
    }

    public synchronized EventReport report(String eventId) {
        var guests = state.bookings().stream().filter(b -> b.eventId().equals(eventId)).map(b -> {
            Participant person = participant(b.participantId());
            return new EventReport.GuestRow(person.name(), person.email(), b.seats(), b.status().name());
        }).toList();
        return new EventReport(event(eventId), quote(eventId), guests);
    }

    public synchronized Path export(String eventId, ExportCreator creator, Path destination) throws IOException {
        Path result = creator.export(report(eventId), destination);
        eventBus.publish(new StudioEvent(Instant.now(clock), "Exported " + creator.extension().toUpperCase(java.util.Locale.ROOT) + ": " + event(eventId).title()));
        return result;
    }

    private void editable(Event event) {
        if (event.status() == EventStatus.COMPLETED || event.status() == EventStatus.CANCELLED) throw new ValidationException("Completed and cancelled events are read-only.");
    }

    private void future(Event event) {
        if (LocalDate.parse(event.date()).isBefore(LocalDate.now(clock))) throw new ValidationException("Event date must be today or later.");
    }

    private void commit(StudioState next, String message) throws IOException {
        repository.save(next);
        state = next;
        eventBus.publish(new StudioEvent(Instant.now(clock), message));
    }

    @Override
    public synchronized void close() throws IOException { repository.close(); }
}
