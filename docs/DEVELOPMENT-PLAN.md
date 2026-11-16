# Prospective three-week development plan

This plan follows the assignment's milestones. It does not claim that three weeks elapsed, that weekly presentations occurred, or that the implementation has historic commits.

| Stage | Work | Evidence to collect |
| --- | --- | --- |
| Week 1: idea and architecture | Confirm team, campus event problem, scenarios, UI sketch, pattern allocation and runnable skeleton | Real commits, initial diagrams and instructor presentation |
| Week 2: working version | Independently develop and integrate event configuration, bookings, pricing, persistence and six patterns | Incremental commits, working demo and instructor presentation |
| Week 3: completion | Review assisted code, add individual changes, test edge cases, refine UI, update UML/report and prepare defense | Test evidence, final screenshots, individual contribution record and presentation |

According to the provided assignment, final materials are due by the end of course Week 9 and defense is in Week 10. Exact calendar dates were not provided. The team must confirm them with the instructor and submit through Moodle. No schedule or deadline has been inferred from the local computer date.

## Interface sketch

```text
Event Studio
[Events] [People] [Activity]
+-------------------------+------------------------------------------+
| Your events [New event] | Selected title, date, venue, lifecycle    |
| event / date / status   | [Edit] [Open] [Complete] [Cancel] [Delete] |
|                         | Package, pricing, total and free seats   |
|                         | Guest list [Book seats]                  |
|                         | [Edit booking] [Cancel booking]          |
|                         | [Export CSV] [Export HTML]               |
+-------------------------+------------------------------------------+
Saved locally / actionable validation and storage errors
```
