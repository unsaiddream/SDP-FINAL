package kz.aitu.eventstudio;

import kz.aitu.eventstudio.domain.*;
import kz.aitu.eventstudio.events.*;
import kz.aitu.eventstudio.export.*;
import kz.aitu.eventstudio.pricing.*;
import kz.aitu.eventstudio.service.StudioFacade;
import kz.aitu.eventstudio.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StudioFacadeTest {
    @TempDir Path directory;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private StudioFacade studio;
    private EventBus bus;

    @BeforeEach
    void open() throws IOException {
        bus = new EventBus();
        studio = new StudioFacade(new JsonStudioRepository(directory.resolve("studio.json")), bus, CLOCK);
    }

    @AfterEach
    void close() throws IOException { studio.close(); }

    private Event draft(String name, int capacity) {
        return new Event.Builder().title(name).date("2026-12-01").venue("Campus hall").capacity(capacity).build();
    }

    private Event createOpen(int capacity) throws IOException {
        Event event = studio.saveEvent(draft("Defense demo", capacity));
        studio.changeStatus(event.id(), EventStatus.OPEN);
        return studio.event(event.id());
    }

    @Test
    void completeJourneySavesConfiguredEventBooksExportsAndReloads() throws Exception {
        Event event = studio.saveEvent(draft("Winter meetup", 25).toBuilder().packageTier(PackageTier.STUDIO)
                .extras(List.of(Extra.CATERING, Extra.PHOTOGRAPHY)).pricingMode(PricingMode.GROUP).build());
        Participant person = studio.saveParticipant(null, "Sam Demo", "sam@example.com");
        studio.changeStatus(event.id(), EventStatus.OPEN);
        studio.saveBooking(null, event.id(), person.id(), 10);
        assertEquals(new BigDecimal("78300.00"), studio.quote(event.id()).total());
        Path html = studio.export(event.id(), new HtmlExportCreator(), directory.resolve("result.html"));
        assertTrue(Files.readString(html).contains("78300.00"));
        assertTrue(Files.readString(html).contains("Sam Demo"));
        studio.changeStatus(event.id(), EventStatus.COMPLETED);
        studio.close();
        studio = new StudioFacade(new JsonStudioRepository(directory.resolve("studio.json")), bus, CLOCK);
        assertEquals(EventStatus.COMPLETED, studio.event(event.id()).status());
        assertEquals(10, studio.confirmedSeats(event.id()));
        assertEquals(new BigDecimal("78300.00"), studio.quote(event.id()).total());
    }

    @Test
    void switchingStrategiesChangesPersistedQuote() throws IOException {
        Event event = studio.snapshot().events().get(0);
        studio.saveEvent(event.toBuilder().pricingMode(PricingMode.STANDARD).build());
        assertEquals(new BigDecimal("104400.00"), studio.quote(event.id()).total());
        studio.saveEvent(event.toBuilder().pricingMode(PricingMode.GROUP).build());
        assertEquals(new BigDecimal("93960.00"), studio.quote(event.id()).total());
    }

    @Test
    void groupDiscountStartsAtExactlyTenGuests() {
        Event event = draft("Boundary", 20).toBuilder().pricingMode(PricingMode.GROUP).build();
        PriceCalculator calculator = new PriceCalculator();
        assertEquals(new BigDecimal("27000.00"), calculator.calculate(event, 9).total());
        assertEquals(new BigDecimal("27000.00"), calculator.calculate(event, 10).total());
        assertEquals(new BigDecimal("3000.00"), calculator.calculate(event, 10).discount());
        assertEquals(new BigDecimal("0.00"), calculator.calculate(event, 0).total());
    }

    @Test
    void decoratorsComposeWithoutMutatingBasePackage() {
        PriceComponent base = new PackagePrice(PackageTier.ESSENTIAL);
        PriceComponent configured = new ExtraDecorator(new ExtraDecorator(base, Extra.CATERING), Extra.TRANSPORT);
        assertEquals(new BigDecimal("7000"), configured.unitPrice());
        assertEquals(new BigDecimal("3000"), base.unitPrice());
        assertEquals(3, configured.descriptions().size());
    }

    @Test
    void committedChangesNotifyObserversAndUnsubscribeWorks() throws Exception {
        List<StudioEvent> events = new ArrayList<>();
        AutoCloseable subscription = bus.subscribe(events::add);
        Event created = studio.saveEvent(draft("Observed", 10));
        assertEquals(1, events.size());
        assertTrue(events.get(0).message().contains("Observed"));
        assertEquals(CLOCK.instant(), events.get(0).occurredAt());
        assertEquals(created, studio.event(created.id()));
        subscription.close();
        studio.deleteEvent(created.id());
        assertEquals(1, events.size());
    }

    @Test
    void brokenSubscriberDoesNotPreventOtherSubscribersOrSaving() throws IOException {
        List<StudioEvent> delivered = new ArrayList<>();
        bus.subscribe(event -> { throw new IllegalStateException("test subscriber failed"); });
        bus.subscribe(delivered::add);
        Event created = studio.saveEvent(draft("Still saved", 10));
        assertEquals(created, studio.event(created.id()));
        assertEquals(1, delivered.size());
    }

    @Test
    void failedDiskWriteDoesNotMutateStateOrPublishEvent() throws Exception {
        EventBus failingBus = new EventBus();
        List<StudioEvent> events = new ArrayList<>();
        failingBus.subscribe(events::add);
        StudioState initial = studio.snapshot();
        StudioRepository failingRepository = new StudioRepository() {
            public Optional<StudioState> load() { return Optional.of(initial); }
            public void save(StudioState state) throws IOException { throw new IOException("Disk full"); }
            public void close() {}
        };
        try (StudioFacade isolated = new StudioFacade(failingRepository, failingBus, CLOCK)) {
            assertThrows(IOException.class, () -> isolated.saveEvent(draft("No space", 10)));
            assertEquals(initial, isolated.snapshot());
            assertTrue(events.isEmpty());
        }
    }

    @Test
    void rejectBlankFieldsAndMalformedDates() {
        assertThrows(ValidationException.class, () -> draft(" ", 20));
        assertThrows(ValidationException.class, () -> draft("Valid", 20).toBuilder().venue("").build());
        assertThrows(ValidationException.class, () -> draft("Valid", 20).toBuilder().date("2026-02-30").build());
        assertThrows(ValidationException.class, () -> draft("Valid", 20).toBuilder().date("tomorrow").build());
        assertThrows(ValidationException.class, () -> draft("Valid", 20).toBuilder().date("2026-2-03").build());
    }

    @Test
    void rejectCapacityBoundsAndDuplicateExtras() {
        assertThrows(ValidationException.class, () -> draft("Small", 0));
        assertThrows(ValidationException.class, () -> draft("Large", 501));
        assertThrows(ValidationException.class, () -> draft("Duplicate", 10).toBuilder().extras(List.of(Extra.CATERING, Extra.CATERING)).build());
        assertEquals(500, draft("Maximum", 500).capacity());
    }

    @Test
    void rejectPastDateWithoutChangingState() {
        StudioState before = studio.snapshot();
        assertThrows(ValidationException.class, () -> studio.saveEvent(draft("Past", 10).toBuilder().date("2026-11-15").build()));
        assertEquals(before, studio.snapshot());
    }

    @Test
    void rejectInvalidAndDuplicateEmails() throws IOException {
        assertThrows(ValidationException.class, () -> studio.saveParticipant(null, "Sam", "broken"));
        assertThrows(ValidationException.class, () -> studio.saveParticipant(null, "Sam", "a@-example.com"));
        assertThrows(ValidationException.class, () -> studio.saveParticipant(null, "Sam", "a@example..com"));
        assertThrows(ValidationException.class, () -> studio.saveParticipant(null, "Sam", "DANA@EXAMPLE.COM"));
        Participant person = studio.saveParticipant(null, "New", "NEW@example.com");
        assertEquals("new@example.com", person.email());
        Participant updated = studio.saveParticipant(person.id(), "Edited", person.email());
        assertEquals("Edited", studio.participant(updated.id()).name());
    }

    @Test
    void rejectOverbookingAndReducingCapacityBelowOccupiedSeats() throws IOException {
        Event event = createOpen(10);
        Participant first = studio.snapshot().participants().get(0);
        Participant second = studio.snapshot().participants().get(1);
        studio.saveBooking(null, event.id(), first.id(), 10);
        StudioState before = studio.snapshot();
        assertThrows(ValidationException.class, () -> studio.saveBooking(null, event.id(), second.id(), 1));
        assertThrows(ValidationException.class, () -> studio.saveEvent(event.toBuilder().capacity(9).build()));
        assertEquals(before, studio.snapshot());
    }

    @Test
    void rejectZeroNegativeAndExcessiveSeats() throws IOException {
        Event event = createOpen(500);
        String participant = studio.snapshot().participants().get(0).id();
        for (int seats : new int[]{0, -1, 501}) assertThrows(ValidationException.class, () -> studio.saveBooking(null, event.id(), participant, seats));
    }

    @Test
    void bookingCanBeEditedCancelledAndRebooked() throws IOException {
        Event event = createOpen(20);
        String participant = studio.snapshot().participants().get(0).id();
        Booking booking = studio.saveBooking(null, event.id(), participant, 5);
        studio.saveBooking(booking.id(), event.id(), participant, 7);
        assertEquals(7, studio.confirmedSeats(event.id()));
        assertThrows(ValidationException.class, () -> studio.saveBooking(null, event.id(), participant, 1));
        studio.cancelBooking(booking.id());
        assertEquals(0, studio.confirmedSeats(event.id()));
        assertEquals(new BigDecimal("0.00"), studio.quote(event.id()).total());
        assertThrows(ValidationException.class, () -> studio.saveBooking(booking.id(), event.id(), participant, 2));
        studio.saveBooking(null, event.id(), participant, 3);
        assertEquals(3, studio.confirmedSeats(event.id()));
    }

    @Test
    void cancellationCascadesAtomicallyAndPreservesHistory() throws IOException {
        Event event = studio.snapshot().events().get(0);
        studio.changeStatus(event.id(), EventStatus.CANCELLED);
        assertEquals(0, studio.confirmedSeats(event.id()));
        assertEquals(2, studio.report(event.id()).guests().size());
        assertTrue(studio.snapshot().bookings().stream().filter(b -> b.eventId().equals(event.id())).allMatch(b -> b.status() == BookingStatus.CANCELLED));
        assertThrows(ValidationException.class, () -> studio.saveEvent(event));
        assertThrows(ValidationException.class, () -> studio.changeStatus(event.id(), EventStatus.OPEN));
    }

    @Test
    void invalidLifecycleActionsAreRejected() throws IOException {
        Event event = studio.saveEvent(draft("Draft", 10));
        String person = studio.snapshot().participants().get(0).id();
        assertThrows(ValidationException.class, () -> studio.saveBooking(null, event.id(), person, 1));
        assertThrows(ValidationException.class, () -> studio.changeStatus(event.id(), EventStatus.COMPLETED));
        assertThrows(ValidationException.class, () -> studio.saveEvent(event.toBuilder().status(EventStatus.OPEN).build()));
        studio.changeStatus(event.id(), EventStatus.OPEN);
        assertThrows(ValidationException.class, () -> studio.changeStatus(event.id(), EventStatus.COMPLETED));
        assertThrows(ValidationException.class, () -> studio.deleteEvent(event.id()));
        Booking completedBooking = studio.saveBooking(null, event.id(), person, 1);
        studio.changeStatus(event.id(), EventStatus.COMPLETED);
        assertThrows(ValidationException.class, () -> studio.saveBooking(null, event.id(), person, 2));
        assertThrows(ValidationException.class, () -> studio.cancelBooking(completedBooking.id()));
    }

    @Test
    void safeDeletionHonorsRelationships() throws IOException {
        Event event = studio.saveEvent(draft("Disposable draft", 10));
        studio.deleteEvent(event.id());
        assertThrows(ValidationException.class, () -> studio.event(event.id()));
        Participant person = studio.saveParticipant(null, "Disposable", "delete@example.com");
        studio.deleteParticipant(person.id());
        assertThrows(ValidationException.class, () -> studio.participant(person.id()));
        assertThrows(ValidationException.class, () -> studio.deleteParticipant(studio.snapshot().participants().get(0).id()));
    }

    @Test
    void factoryMethodExportsBothFormatsWithIdenticalTotals() throws IOException {
        Event event = studio.snapshot().events().get(0);
        Path csv = studio.export(event.id(), new CsvExportCreator(), directory.resolve("report.csv"));
        Path html = studio.export(event.id(), new HtmlExportCreator(), directory.resolve("report.html"));
        assertTrue(Files.readString(csv).contains("93960.00"));
        assertTrue(Files.readString(html).contains("93960.00"));
        assertTrue(Files.readString(csv).contains("CONFIRMED"));
        assertThrows(IllegalArgumentException.class, () -> studio.export(event.id(), new CsvExportCreator(), directory.resolve("report.exe")));
    }

    @Test
    void exportEscapesMarkupQuotesAndSpreadsheetFormulas() throws IOException {
        Event event = studio.saveEvent(draft("=SUM(1,2) <script>\"Hi\"</script>", 10));
        Participant person = studio.saveParticipant(null, "<b>Guest</b>", "safe@example.com");
        studio.changeStatus(event.id(), EventStatus.OPEN);
        studio.saveBooking(null, event.id(), person.id(), 1);
        String html = Files.readString(studio.export(event.id(), new HtmlExportCreator(), directory.resolve("escape.html")));
        String csv = Files.readString(studio.export(event.id(), new CsvExportCreator(), directory.resolve("escape.csv")));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&lt;b&gt;Guest&lt;/b&gt;"));
        assertTrue(csv.contains("\"'=SUM(1,2) <script>\"\"Hi\"\"</script>\""));
    }

    @Test
    void immutableSnapshotsCannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> studio.snapshot().events().clear());
        assertThrows(UnsupportedOperationException.class, () -> studio.snapshot().events().get(0).extras().clear());
    }

    @Test
    void missingReferencesAreRejected() throws IOException {
        Event event = createOpen(10);
        assertThrows(ValidationException.class, () -> studio.saveBooking(null, event.id(), UUID.randomUUID().toString(), 1));
        assertThrows(ValidationException.class, () -> studio.saveParticipant(UUID.randomUUID().toString(), "Ghost", "ghost@example.com"));
        assertThrows(ValidationException.class, () -> studio.event("missing"));
    }
}
