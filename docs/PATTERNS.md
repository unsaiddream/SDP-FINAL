# Pattern design and demonstration

The application uses six distinct patterns from the assignment's permitted list. Each is exercised by the Swing UI. Creational: Builder and Factory Method. Structural: Decorator and Facade. Behavioral: Strategy and Observer.

| Pattern | Problem | Participants | Scenario | Responsible student |
| --- | --- | --- | --- | --- |
| Builder | Events have many validated options and safe editing should preserve identity | `Event`, `Event.Builder`, `StudioFrame.eventDialog` | Create or edit a configured event, then save | Unassigned; actual human contribution must be confirmed |
| Factory Method | Export workflow should be shared while the report product varies | `ExportCreator`, `CsvExportCreator`, `HtmlExportCreator`, `ReportExporter`, `CsvReportExporter`, `HtmlReportExporter` | Export a selected event to CSV and HTML | Unassigned |
| Decorator | Combine optional services without one subclass per combination | `PriceComponent`, `PackagePrice`, `ExtraDecorator`, `Extra` | Select Catering and Photography, observe the additive price and descriptions | Unassigned |
| Facade | UI operations must coordinate rules, storage, pricing and notifications consistently | `StudioFacade`, `StudioRepository`, `PriceCalculator`, `EventBus`, `ExportCreator` | Book seats, with validation, persistence and refresh in one operation | Unassigned |
| Strategy | Pricing needs runtime-selectable algorithms | `PricingStrategy`, `StandardPricing`, `GroupPricing`, `PriceCalculator` | Switch between standard/group pricing for 12 guests | Unassigned |
| Observer | A committed change must refresh the quote, lists and activity without coupling the service to Swing | `EventBus`, `StudioObserver`, `StudioEvent`, subscribed `StudioFrame` callback | Add/cancel a booking; selected total and activity update | Unassigned |

## Builder

`Event.Builder` exposes fluent setters, sensible defaults and `build()`. The canonical event constructor validates required fields, date, capacity and uniqueness of extras. `toBuilder()` preserves identity when editing. The GUI uses it for every new or edited event; sample data uses it too. The facade separately enforces time-sensitive and cross-record rules. A builder alone cannot enforce booking capacity or event transitions.

## Factory Method

`ExportCreator` is the abstract Creator. Its final `export` method manages encoding, destination validation, a temporary file, and output replacement. It calls the protected abstract `createExporter()` method. Each concrete creator overrides that factory method to return a different Product implementing `ReportExporter`. The UI selects a concrete creator through the export buttons; the facade passes an `EventReport` built from real stored state. This is an overridden factory method in a shared creator workflow, rather than merely a switch in a class named Factory. Supporting a new format requires one exporter and one creator, plus a UI entry point.

## Decorator

`PackagePrice` is the base component. `ExtraDecorator` implements the same `PriceComponent` interface and holds a wrapped component. Its `unitPrice()` and `descriptions()` extend the wrapped result. `PriceCalculator` constructs one nested decorator for each selected extra. The combination is used for the displayed and exported price. Extras are additive per guest; fixed event fees would need a different component contract.

## Facade

`StudioFacade` exposes use cases such as `saveEvent`, `saveBooking`, `cancelBooking`, `changeStatus`, `quote` and `export`. The UI does not construct storage transactions or traverse relationships to price an event. A mutation builds a complete next state, validates it, saves it, replaces the in-memory snapshot, then publishes. Cancellation changes an event and its bookings in a single persisted state. The facade is deliberately local and synchronous; a larger multi-user app would split the workflows and introduce a transaction-capable store.

## Strategy

`PricingStrategy` provides the discount rule and its description. `StandardPricing` returns no discount. `GroupPricing` returns 10% at ten or more confirmed guests. `PriceCalculator` chooses the implementation using the event's persisted `PricingMode`, and also exposes an overload that accepts another strategy for extension. Both algorithms use the same decorated unit price and rounding rules. A new selectable strategy requires updating the mode, mapping and tests.

## Observer

The custom `EventBus` stores `StudioObserver` subscribers. The frame subscribes once and unsubscribes when closed. Notifications carry a timestamp and user-facing message; a callback refreshes the data models, quote and activity. Changes made outside the UI thread are marshalled onto Swing's event-dispatch thread. A disk failure does not publish a successful change. One broken observer cannot stop other observers or roll back an already persisted change. The activity log is session-only, and subscriber failures go to stderr rather than being persisted.

## Extension exercises

1. Add an `EarlyBirdPricing` strategy with an explicit, testable eligibility rule and a selectable pricing mode.
2. Add a `VENUE_STYLING` extra. Confirm that unit-price descriptions and both export formats update without modifying the decorator.
3. Add a text report by extending `ExportCreator` and implementing `ReportExporter`.
4. Subscribe a second observer that tracks confirmed-seat changes, then demonstrate unsubscription and failure isolation.

These exercises are preparation; no claim is made that a student has independently completed them.
