# Assignment traceability

Source: supplied FINAL PROJECT.docx, Software Design Patterns, reviewed in full on 16 November 2026. This matrix distinguishes implemented application requirements from personal or historical course requirements. No score is claimed: the instructor determines credit, and 30 of 100 points are for the mandatory individual defense.

| Requirement | Status and evidence |
| --- | --- |
| Complete create/select, configure, act, view/save journey | Implemented; README walkthrough, GUI journey test, screenshots and exported reports |
| At least three related entities | Event, Participant and Booking with validated foreign references |
| At least five scenarios | Event creation/configuration, participant management, booking/edit/cancel, strategy switch, event lifecycle, export, reload |
| Core CRUD including appropriate deletion/cancellation | Events and participants create/read/update/delete; bookings create/read/update/cancel with history |
| Two interchangeable operation algorithms | Standard and Group pricing, selected through the event form and persisted |
| Meaningful event reaction | Custom EventBus/StudioObserver refreshes quote, guest list, free seats and session activity after commits |
| Persistence across restart | JSON repository, next-state validation, atomic replacement where supported, writer lock, restart tests |
| Meaningful final output | CSV summary/guest list and standalone printable HTML with cost breakdown |
| Input validation and error handling | Required text, real dates, future date on event save/open, capacity/seat limits, email uniqueness, overbooking, transitions, storage errors |
| Immediate sample data | First-run SampleData: two events, three people and two bookings, dates relative to first launch |
| Six allowed patterns across all categories | Builder, Factory Method, Decorator, Facade, Strategy, Observer; concrete classes and demonstrations in PATTERNS.md |
| Integrated application-owned patterns | All six participate in actual Swing workflows; no library-internal patterns counted |
| Java or Go core logic | Java 17-compatible source and JAR; Swing desktop interface |
| Browse, create/edit, configure, finish journey, results/errors in GUI | Events, People and Activity tabs, forms, lifecycle actions, quote, guest list and export choosers |
| UI/business/storage separated, readable code | domain/pricing/service/storage/events/export/ui packages; source contains no comments as requested |
| At least eight recorded test scenarios | Fourteen scenarios with concrete input, expected and actual result in TEST-SCENARIOS.md |
| At least three automated business tests | 27 business/persistence tests plus 3 GUI integration tests |
| README setup, launch, sample data and checking instructions | README.md; standalone executable JAR and source/distribution ZIPs |
| PDF introduction, main body, conclusion and future work | docs/report.pdf |
| Pattern table with problem, participants, demo and responsible student | Included; actual student responsibility remains unassigned pending real contribution evidence |
| Class and sequence diagrams | Editable PlantUML and rendered PNG views, embedded in PDF |
| Screenshots, code excerpts and test results in report | Real rendered Swing screenshots, excerpts from current source and archived Surefire evidence |
| Individual contributions in report | Honest contribution status and proposed allocation; no independent student authorship asserted |
| Team size 2-4 and independently implemented 2+ patterns/student | Not verified; teammate names and genuine personal implementation evidence required |
| Three-week history and weekly instructor presentations | Cannot be reconstructed; prospective plan supplied, no historical claims |
| GitHub/GitLab development history | Initial repo had no commits. Remote configured; nothing pushed, no commits/history invented |
| Moodle source ZIP, report and repository link submission | Local packages prepared; user/team must review and submit. No Moodle upload performed |
| Mandatory individual defense and live changes | Defense guide and extension exercises prepared; each student must demonstrate competence live |
| AI/external materials disclosure and independent work | Codex assistance disclosed in report/README. Course-policy compliance and independent development require team/instructor review |

## Practical limits

The app is local and single-user, with a fixed service catalog, session-only activity, calculated estimates rather than payment transactions, and no email/cloud service. Java source targets 17; actual testing used macOS, Java 25/27. Native usability on Windows/Linux and direct Java 17 runtime execution have not been verified. The pure Swing content screenshots are captured from the visible integration test windows, without simulated application states.
