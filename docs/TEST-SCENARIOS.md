# Required test scenarios

Actual results were observed in automated business/persistence and visible Swing integration runs on 16 November 2026. Inputs below are concrete values used by the tests, rather than hypothetical outcomes. Full method names and counts are in `TEST-RESULTS.md` and the archived Surefire reports.

| ID | Scenario and inputs | Expected result | Actual result | Automated evidence |
| --- | --- | --- | --- | --- |
| T01 | Create Studio event, Catering + Photography, capacity 25, Group; open; book 10 seats; export HTML; complete; reopen JSON | 8,700/guest, 87,000 subtotal, 8,700 discount, 78,300 total; completed state and 10 seats survive restart | Pass: 78,300.00 KZT, guest in HTML, completed event and 10 seats reloaded | `completeJourneySavesConfiguredEventBooksExportsAndReloads` |
| T02 | Sample open event, 12 seats; switch Group to Standard and back | Standard 104,400; Group 93,960 | Pass: both exact totals match | `switchingStrategiesChangesPersistedQuote` |
| T03 | Subscribe, save event Observed, unsubscribe, delete draft | One committed save notification; no delete notification after unsubscribe | Pass: one message and expected clock timestamp; saved event present | `committedChangesNotifyObserversAndUnsubscribeWorks` |
| T04 | Save participant Санжар Қараулов, close repository, reload | Unicode and all record relationships are preserved | Pass: complete saved state equals reloaded state | `jsonRoundTripPreservesUnicodeAndAllRelationships` |
| T05 | Blank event title/venue and date 2026-02-30 | Clear validation error, no invalid record | Pass: ValidationException; GUI shows Event title is required and real-date error without saving | `rejectBlankFieldsAndMalformedDates`, GUI `validationStaysVisibleAndDoesNotSaveBadForm` |
| T06 | Capacity 0 or 501; past date 2026-11-15 against fixed 2026-11-16 clock | Reject bounds and past date; state unchanged | Pass: all rejected; snapshot unchanged | `rejectCapacityBoundsAndDuplicateExtras`, `rejectPastDateWithoutChangingState` |
| T07 | Invalid email broken and case-variant DANA@EXAMPLE.COM | Reject malformed email and duplicate normalized email | Pass: ValidationException; new valid email normalized to lowercase | `rejectInvalidAndDuplicateEmails` |
| T08 | Capacity 10 with 10 booked seats; add 1 more or reduce capacity to 9 | Reject both actions; preserve original state and seats | Pass: both rejected and snapshot equal to before | `rejectOverbookingAndReducingCapacityBelowOccupiedSeats` |
| T09 | Group pricing with Essential at 9, 10 and 0 guests | Totals 27,000; 27,000 with 3,000 discount at ten; zero | Pass: exact BigDecimal values match | `groupDiscountStartsAtExactlyTenGuests` |
| T10 | Fake disk failure on save | IOException, no in-memory mutation and no success event | Pass: initial snapshot preserved, subscriber receives zero changes | `failedDiskWriteDoesNotMutateStateOrPublishEvent` |
| T11 | Cancel sample event with two confirmed bookings | Cancel event and both bookings together; release 12 seats; retain two history rows | Pass: zero confirmed seats and two cancelled rows | `cancellationCascadesAtomicallyAndPreservesHistory` |
| T12 | GUI: add Taylor; Essential + Catering, Group, 10 seats; export CSV/HTML; complete and reload | 49,500 total, both files created, activity updated, completed state persists | Pass: 49,500.00 KZT, expected file contents, at least seven activity entries, 10 seats after reload | GUI `userCreatesConfiguresBooksAndExportsThroughActualControls` |
| T13 | Malformed/empty/null/unsupported JSON and missing references | Load error; never overwrite bad data with samples | Pass: IOException and original bad content remains | Repository corruption, unsupported-version and relationship tests |
| T14 | Export title with =SUM, comma, quotes, script markup; guest with HTML markup | Quote CSV cells, neutralize spreadsheet formula prefix, escape HTML | Pass: escaped markup and doubled CSV quotes; formula prefix protected | `exportEscapesMarkupQuotesAndSpreadsheetFormulas` |

Additional automated coverage includes seat bounds, duplicate active bookings, edit/cancel/rebook, lifecycle restrictions, relationship-safe deletion, immutable snapshots, independent observer failure, file lock exclusion, existing empty-workspace preservation, and temporary-file cleanup.
