# Defense preparation

## Six-minute application demonstration

1. Launch `java -jar target/event-studio.jar` with a fresh data path. Explain the three entities and initial 12-guest quote of 93,960 KZT.
2. Add a participant in People. Create an Essential event with Catering, capacity 20, and Group pricing. Save and open bookings.
3. Book 10 seats for the participant. Explain the calculation: 5,500 KZT per guest, 55,000 subtotal, 5,500 discount, 49,500 total.
4. Edit the pricing to Standard and explain why the total becomes 55,000. Restore Group. Show how removing Catering changes both the component list and price.
5. Show Activity after booking or editing. Try an overcapacity booking and show that neither occupancy nor persisted state changed.
6. Export CSV and HTML, open the HTML in a browser, then restart with the same data file. Show that options and bookings survived.
7. Complete the event to freeze it, or cancel an open event and show its released seats and preserved cancelled guest rows.

## Trace one mutation

`StudioFrame` receives the booking form action. It calls `StudioFacade.saveBooking`, which checks the event, participant, seat bounds, duplicate booking and total capacity. `StudioState` validates complete relationships. The repository writes a next-state JSON file before the facade replaces its current snapshot. `EventBus.publish` informs the subscribed frame. The frame refreshes its guest table, quote, occupancy and session activity. `PriceCalculator` composes the decorators and applies the selected strategy.

## Questions to practice

- Why are the three entities separate? A participant may join several events, and a booking stores the relationship, seats and cancellation state.
- Why Builder rather than a long call from the UI? The form configures many options, and the builder makes defaults and immutable edits explicit. The canonical constructor still validates every construction route.
- What makes Factory Method different from a helper returning a format? The creator has a shared export workflow that calls an abstract creation method overridden by creator subclasses.
- What does Decorator wrap? Another `PriceComponent`, so any number of allowed extras composes recursively without one class per service combination.
- How does Strategy differ from Decorator here? Decorator composes the package's unit price. Strategy replaces the calculation rule applied to the subtotal at runtime.
- Why a Facade? The UI invokes one complete booking/cancellation/export use case without coordinating storage, validation, prices and notifications itself.
- What is the Observer subject and subscriber? The explicit `EventBus` is the publisher, and a custom `StudioObserver` callback in the frame is the subscriber. Swing's internal listeners are not counted as the application's pattern.
- What happens if saving fails? The next snapshot is not installed and no success event is published. The form remains open or an error appears in the frame.
- Why keep cancelled records? They explain the result and preserve relationships; cancellation releases capacity without losing history.
- What is the durability limit? Atomic replacement is file-system dependent; fallback replacement is not guaranteed atomic. There is no distributed transaction or cloud backup.
- What is not persisted? Session activity and calculated totals. Totals are recomputed from saved configuration and bookings.
- What are the limits of the implementation? Local single-user access, fixed service catalog, no actual payments/email/cloud, and no direct testing on Windows/Linux or Java 17.

## Individual live modifications

Each member should practice at least one extension relevant to their actual contribution:

- Add a third pricing strategy and boundary tests; connect it to the event form.
- Add a service extra and show that nested pricing and exports pick it up.
- Add a new exporter using a concrete creator and product.
- Add and remove a second observer, then explain when notifications occur.

Do not memorize an authorship story. Use `CONTRIBUTIONS.md` to record actual independent work and disclose Codex assistance. Confirm the course's allowed AI scope and personal-defense requirements with the instructor.
