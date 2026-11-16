package kz.aitu.eventstudio.service;

import kz.aitu.eventstudio.domain.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class SampleData {
    private SampleData() {}

    public static StudioState create(Clock clock) {
        Participant dana = new Participant("10000000-0000-0000-0000-000000000001", "Dana Sadykova", "dana@example.com");
        Participant alik = new Participant("10000000-0000-0000-0000-000000000002", "Alik Omarov", "alik@example.com");
        Participant mira = new Participant("10000000-0000-0000-0000-000000000003", "Mira Chen", "mira@example.com");
        Event meetup = new Event.Builder().id("20000000-0000-0000-0000-000000000001").title("Campus design meetup")
                .date(LocalDate.now(clock).plusDays(7).toString()).venue("AITU creative room").capacity(30)
                .packageTier(PackageTier.STUDIO).extras(List.of(Extra.CATERING, Extra.PHOTOGRAPHY))
                .pricingMode(PricingMode.GROUP).status(EventStatus.OPEN).build();
        Event workshop = new Event.Builder().id("20000000-0000-0000-0000-000000000002").title("Weekend photo walk")
                .date(LocalDate.now(clock).plusDays(14).toString()).venue("Central park, Astana").capacity(16)
                .extras(List.of(Extra.TRANSPORT)).build();
        return new StudioState(1, List.of(meetup, workshop), List.of(dana, alik, mira), List.of(
                new Booking("30000000-0000-0000-0000-000000000001", meetup.id(), dana.id(), 8, BookingStatus.CONFIRMED),
                new Booking("30000000-0000-0000-0000-000000000002", meetup.id(), alik.id(), 4, BookingStatus.CONFIRMED)));
    }
}
