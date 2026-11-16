# Event Studio

A Java desktop application for planning campus gatherings. Create an event, configure a package and extras, choose a pricing rule, register people, book seats, and export the saved result.

The repository was empty when implementation began. Event Studio follows the Event Studio idea in the supplied FINAL PROJECT assignment. It has three related entities: `Event`, `Participant`, and `Booking`.

## Requirements and launch

- JDK 17 or newer and Maven 3.8.7 or newer.
- A graphical desktop session for the application and GUI tests. No server, database, account, or API key is required.
- Internet access for Maven's first dependency download; the packaged application runs offline.

```sh
mvn clean verify
java -jar target/event-studio.jar
```

To use an isolated workspace or choose a writable location:

```sh
java -jar target/event-studio.jar /absolute/path/to/demo/studio.json
```

Paths containing spaces must be quoted. By default, `data/studio.json` is relative to the directory from which you launch the application. Run from this repository for a consistent default location. Closing the window releases the data file lock. The small `.lock` sidecar may remain on disk; it is the operating system lock, rather than the file's existence, that prevents another writer.

```sh
java -jar target/event-studio.jar --help
```

In IntelliJ IDEA, open `pom.xml` as a Maven project, select JDK 17+, and run `kz.aitu.eventstudio.App` with the project root as its working directory.

## Demonstrate the complete journey

1. Launch the app. A new workspace contains two events, three participants, and two bookings. Sample dates are seven and fourteen days after the first launch. The open Campus design meetup has 12 booked seats and a total of **93,960.00 KZT**.
2. In **People**, use **Add participant** and save a valid name and email.
3. In **Events**, select **New event**. Enter a title, a date today or later in `YYYY-MM-DD` format, venue, and capacity 20. Keep Essential, enable Catering, and select the Group rate. Save the draft.
4. Use **Open bookings**, then **Book seats**. Select the new participant and reserve 10 seats. The total becomes **49,500.00 KZT**: `(3,000 + 2,500) x 10 x 0.90`.
5. Use **Edit** to switch to Standard rate. The total becomes **55,000.00 KZT**. Switch back to Group. Editing, booking, and export actions appear in **Activity**.
6. Use **Edit booking** to change the seat count. Cancelling a booking releases its seats and excludes it from the total, while keeping its history. A new booking can be created for that participant afterward.
7. Use **Export CSV** and **Export HTML** to choose destination files. HTML includes the package, pricing breakdown, guest list, and cancellation history; open it in a browser and use Print if needed. CSV contains a summary and guest list. Both reflect the real saved state.
8. Use **Complete** to make an event read-only, or **Cancel event** to cancel the event and all its bookings together. Completed events require at least one confirmed booking. You can still view and export completed or cancelled events.
9. Close and reopen with the same data path. Events, participants, options, lifecycle states, and bookings remain available.

Only unused draft events and participants without booking history may be deleted. Bookings use cancellation instead of destructive deletion. Confirmed bookings can be edited while the event is open. A participant can have only one confirmed booking per event. Old open sample events can be rescheduled through Edit if you return to the same workspace after their date.

## Six integrated patterns

| Pattern | Role and implementation | Working demonstration |
| --- | --- | --- |
| Builder | `Event.Builder` assembles an immutable, validated event, with defaults and `toBuilder()` for edits | New event / Edit: configure the tier, extras, date, capacity and pricing |
| Factory Method | Abstract `ExportCreator.export()` uses the overridable `createExporter()` factory method; `CsvExportCreator` and `HtmlExportCreator` produce different `ReportExporter` implementations | Export the selected event in both formats |
| Decorator | `ExtraDecorator` wraps a `PriceComponent`; nested decorators add prices and description lines to `PackagePrice` | Select multiple extras; see each line and the resulting total |
| Facade | `StudioFacade` coordinates validation, relationships, persistence, calculation, export and events behind application operations | Booking updates capacity, stored data and quote through one operation |
| Strategy | `StandardPricing` and `GroupPricing` implement `PricingStrategy`; `PriceCalculator` selects the saved choice at runtime | Change the pricing rule for an event with at least 10 confirmed guests |
| Observer | `EventBus` publishes committed changes to `StudioObserver`; the frame subscribes and refreshes selected data and session activity | Book/cancel seats and watch the total, guest list and Activity update |

These are application-owned implementations. No framework-internal pattern is counted. See `docs/PATTERNS.md`, the report, and the UML source for participants, tradeoffs, and code excerpts. No human pattern ownership is asserted: the team must document its actual independent work before submission.

## Architecture and data rules

- `domain`: immutable records, field validation and cross-entity state invariants.
- `pricing`: composable price components and interchangeable strategies, using `BigDecimal`.
- `service`: complete use cases, lifecycle rules and transactions through the facade.
- `storage`: repository interface and UTF-8 JSON implementation using Gson.
- `events`: explicit observer interfaces, subscription lifecycle and event dispatch.
- `export`: factory method creators, escaped CSV and standalone HTML reports.
- `ui`: Swing controls, dialogs and feedback. UI actions call the facade rather than write data directly.

Every mutation builds a validated immutable next state, writes a temporary file, flushes it, replaces the data file, then publishes the committed state and notification. Atomic replacement is used when supported by the file system, with replacement as a fallback. A failed save leaves the in-memory state unchanged and publishes no change event. The application holds an exclusive writer lock for its lifetime. Malformed files, unsupported schema versions, invalid records, duplicate identities/emails, missing relationships and overcapacity fail to load and are not replaced with sample data. Keep a backup before manually editing data; manual edits while the app is running are unsupported.

Money is an estimate in KZT. Package and extra prices are **per confirmed guest**, with no taxes or payment processing. Group pricing applies a 10% discount to the whole subtotal from 10 confirmed guests. Zero guests costs zero. The service catalog is fixed in code; events, people, and bookings are the editable entities. Activity is session-only; core business data persists. This is a single-user local application without authentication or cloud synchronization.

## Verification

```sh
mvn clean verify
mvn -Pgui test
```

The default command runs 27 business and persistence tests and builds the executable JAR. The GUI profile runs three additional tests in a visible graphical desktop session. They operate real Swing controls and file choosers: create a participant and event, configure options, book, export CSV/HTML, complete, reload, validate bad forms, and observe a refresh. They do not replace a student's live demonstration or cross-platform usability review.

```sh
mvn -Pgui test -Dstudio.screenshotDir=docs/screenshots
```

The screenshot option refreshes evidence images from rendered Swing content. By default GUI evidence goes into `target/ui-evidence`. Test evidence and environment details are recorded in `docs/TEST-RESULTS.md`; the required input / expected / actual scenario matrix is in `docs/TEST-SCENARIOS.md`.

Implementation was verified on macOS arm64 with Java 25 and Maven 3.9.16 (Maven itself used its installed Java 27 runtime). Java source targets release 17; a Java 17 runtime and Windows/Linux desktop were not directly tested.

## Documentation and submission

- `docs/report.pdf`: PDF report with introduction, user scenarios, architecture, six-pattern table, rendered UML, screenshots, test results, source excerpts, contribution status, conclusion and future work.
- `docs/diagrams/class-diagram.puml` and `sequence-diagram.puml`: editable PlantUML. Corresponding PNGs are included, so no renderer is needed to read the report.
- `docs/DEFENSE.md`: demonstration, pattern explanations, tradeoffs and live-change exercises.
- `docs/RUBRIC.md`: requirement-by-requirement traceability and honest outstanding items.
- `docs/CONTRIBUTIONS.md`: actual contribution record and proposed allocation template.
- `docs/DEVELOPMENT-PLAN.md`: prospective three-week milestones, not a claim of past progress.
- `scripts/build_report.py`: rebuilds the PDF with Python 3, ReportLab and Pillow, using the checked-in UML images, screenshots and test evidence.

To rebuild the documentation after changing source, first run both test profiles, then `python3 scripts/record_test_results.py`. Install report dependencies with `python3 -m pip install -r scripts/requirements-report.txt`, then run `python3 scripts/build_report.py`. To regenerate the UML images, obtain PlantUML 1.2025.10 and run `java -jar plantuml-1.2025.10.jar -tpng docs/diagrams/*.puml`; the class diagrams use the bundled Smetana layout, so a separate Graphviz installation is unnecessary. `python3 scripts/smoke_packaged.py` additionally checks executable startup with an isolated sample workspace.

After verification, package source and an executable distribution:

```sh
python3 scripts/package_submission.py
```

The source archive is `target/submission/SDP-FINAL-source.zip`, and the runnable distribution is `target/submission/Event-Studio-1.0.0.zip`. Source packaging excludes `.git`, local workspace data, build caches and secrets. The source archive includes Maven configuration, all Java source/tests, documentation, screenshots and the report. The distribution includes the runnable JAR and documentation. Rebuild these archives after later changes.

The configured remote is https://github.com/unsaiddream/SDP-FINAL . Nothing was pushed and no commits were created as part of this implementation. The empty initial repository has no real development history to reconstruct. The team still needs to review and independently understand/adapt the assisted code, confirm 2-4 members and their actual pattern contributions, develop with honest commits, verify the remote repository contents, submit the source ZIP/report/repository link to Moodle, and complete the individual defense. Weekly presentations and defense cannot be completed by generating files.

## AI and external materials

Codex (OpenAI) assisted with implementation, test execution and documentation in this working session. This must not be represented as independent student authorship. The assignment restricts AI to its course policy, so the team must review the permitted scope and disclose actual use. Gson, JUnit Jupiter and Apache Maven plugins are declared with fixed versions in `pom.xml`; Swing and the Java standard library provide the desktop UI and file APIs. Third-party license notices remain inside the executable JAR; Maven Shade's duplicate manifest warning is benign and the application's manifest sets its entry point.
