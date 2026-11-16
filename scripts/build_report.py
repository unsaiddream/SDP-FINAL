from pathlib import Path
from html import escape
import json
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle, Image, PageBreak, NextPageTemplate, Preformatted
from PIL import Image as PillowImage

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
evidence = json.loads((DOCS / 'test-evidence' / 'summary.json').read_text())
OUT = DOCS / 'report.pdf'
W, H = A4
MARGIN = 46
WIDTH = W - 2 * MARGIN
BLUE = colors.HexColor('#3053B8')
INK = colors.HexColor('#193049')
LIGHT = colors.HexColor('#F0F5FB')
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='ReportTitle', fontName='Helvetica-Bold', fontSize=32, leading=37, textColor=INK, spaceAfter=18))
styles.add(ParagraphStyle(name='SectionTitle', fontName='Helvetica-Bold', fontSize=21, leading=26, textColor=INK, spaceAfter=15))
styles.add(ParagraphStyle(name='ReportBody', fontName='Helvetica', fontSize=10.4, leading=15.5, textColor=INK, spaceAfter=10))
styles.add(ParagraphStyle(name='ReportSmall', fontName='Helvetica', fontSize=8.5, leading=11.5, textColor=INK, spaceAfter=7))
styles.add(ParagraphStyle(name='Cell', fontName='Helvetica', fontSize=8.4, leading=11.6, textColor=INK))
styles.add(ParagraphStyle(name='CodeBlock', fontName='Courier', fontSize=7.0, leading=10, textColor=INK, backColor=LIGHT, borderPadding=8, spaceAfter=12))
styles.add(ParagraphStyle(name='Caption', fontName='Helvetica-Oblique', fontSize=8.5, leading=11, textColor=INK, spaceAfter=12))
styles.add(ParagraphStyle(name='Subhead', fontName='Helvetica-Bold', fontSize=12, leading=16, textColor=BLUE, spaceAfter=7, spaceBefore=8))
story = []


def p(text, style='ReportBody'):
    story.append(Paragraph(text, styles[style]))


def heading(text):
    p(text, 'SectionTitle')


def sub(text):
    p(text, 'Subhead')


def page(title, template='portrait'):
    story.append(NextPageTemplate(template))
    story.append(PageBreak())
    heading(title)


def table(rows, widths):
    cells = [[Paragraph(escape(str(value)).replace('\n', '<br/>'), styles['Cell']) for value in row] for row in rows]
    result = Table(cells, colWidths=widths, repeatRows=1, hAlign='LEFT')
    result.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), LIGHT), ('TEXTCOLOR', (0, 0), (-1, -1), INK),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8), ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8), ('LINEBELOW', (0, 0), (-1, 0), 0.7, BLUE),
        ('LINEBELOW', (0, 1), (-1, -1), 0.3, colors.HexColor('#D6E0EC'))]))
    story.append(result)
    story.append(Spacer(1, 12))


def image(path, width, height=None):
    path = DOCS / path
    with PillowImage.open(path) as original:
        iw, ih = original.size
    scale = min(width / iw, height / ih if height else width / iw)
    result = Image(str(path), width=iw * scale, height=ih * scale)
    result.hAlign = 'LEFT'
    story.append(result)
    story.append(Spacer(1, 7))


def code(relative, start, end, limit=80):
    source = (ROOT / 'src' / 'main' / 'java' / 'kz' / 'aitu' / 'eventstudio' / relative).read_text()
    first = source.index(start)
    last = source.index(end, first) + len(end)
    lines = source[first:last].splitlines()
    minimum = min((len(line) - len(line.lstrip()) for line in lines[1:] if line.strip()), default=0)
    normalized = [lines[0]] + [line[minimum:] if line.startswith(' ' * minimum) else line for line in lines[1:]]
    for line in normalized:
        if len(line) > limit:
            raise ValueError(f'Code line needs a narrower excerpt: {relative}: {line}')
    story.append(Preformatted('\n'.join(normalized), styles['CodeBlock']))


def footer(canvas, doc):
    width, height = canvas._pagesize
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(INK)
    canvas.drawString(MARGIN, 25, 'Event Studio | Software Design Patterns')
    canvas.drawRightString(width - MARGIN, 25, str(doc.page))


portrait_frame = Frame(MARGIN, 43, WIDTH, H - 92, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
LW, LH = landscape(A4)
landscape_frame = Frame(MARGIN, 43, LW - 2 * MARGIN, LH - 92, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
doc = BaseDocTemplate(str(OUT), pagesize=A4, title='Event Studio Software Design Patterns Project Report', author='Prepared with Codex assistance for Sanzhar Karaulov')
doc.addPageTemplates([
    PageTemplate(id='portrait', frames=[portrait_frame], onPage=footer, pagesize=A4),
    PageTemplate(id='landscape', frames=[landscape_frame], onPage=footer, pagesize=landscape(A4))])

p('Event Studio', 'ReportTitle')
p('Software Design Patterns final project report', 'SectionTitle')
p('Implementation evidence recorded on ' + evidence['recorded_date'] + '. Requested by Sanzhar Karaulov.', 'ReportSmall')
story.append(Spacer(1, 17))
sub('Introduction')
p('Event Studio helps a campus organizer turn an event idea into a configured gathering with a guest list and a saved cost summary. Spreadsheets often separate package options, participant details and seat counts; this application keeps those relationships consistent in one local workspace.')
p('The implemented journey is: create an event, configure services and pricing, open bookings, reserve seats, review the calculated result, and export a report. A Java Swing interface connects to Java business logic and a JSON repository. Six application-owned design patterns structure those operations.')
sub('Implementation result')
p('The application has three related domain entities, complete event and participant CRUD, booking editing and cancellation, two runtime-selectable pricing rules, committed-change notifications, sample data, validation, persistence, CSV and HTML exports. ' + str(evidence['total_tests']) + ' automated tests passed across business, persistence and GUI suites.')
sub('Authorship and course requirements')
p('The repository was empty and had no commits at the start of this session. Codex assisted with the implementation, tests and documentation. This report does not assert independent student authorship, a three-week development history, weekly instructor presentations or a completed defense. The team must confirm its members, record genuine individual work, review the course AI policy, submit materials, and complete the mandatory individual defense.')
p('The supplied assignment allocates 70 points to the shared project and 30 to individual defense. This report provides implementation evidence, not a guaranteed assessment score.', 'ReportSmall')

page('User scenarios')
table([
    ['Scenario', 'Organizer action', 'Result'],
    ['Create and configure', 'New event: enter title, date, venue, capacity, package, extras and pricing rule.', 'A validated immutable draft is stored.'],
    ['Manage people', 'Create or edit a participant with a unique valid email; delete unused records.', 'Reusable participant data is saved.'],
    ['Open and book', 'Open a draft, select a participant and reserve seats.', 'Booking is saved; guest count, available seats and total update.'],
    ['Compare pricing', 'Switch Standard and Group for an event with at least ten confirmed guests.', 'The chosen behavior persists; the total reflects the algorithm.'],
    ['Edit or cancel', 'Change booking seats or cancel a booking/event.', 'Capacity and price update; cancelled rows remain available.'],
    ['View and export', 'Export the selected result in CSV and HTML.', 'Real saved options, totals and guest rows are written to a chosen file.'],
    ['Complete and restart', 'Complete an event, close the app, reopen the same workspace.', 'Completed data is read-only and survives restart.']
], [98, 227, WIDTH - 325])
p('Target users are students or campus organizers planning small gatherings with at most 500 seats per event. No online registration, payments or administrator account are needed. Sample data makes the application immediately demonstrable.')
sub('Worked result')
p('Essential costs 3,000 KZT per guest and Catering adds 2,500. Ten confirmed guests create a 55,000 KZT subtotal. Group pricing applies a 5,500 KZT discount, producing 49,500 KZT. Standard pricing produces 55,000 KZT. Cancelled seats are excluded from both calculations.')

page('Architecture and application rules')
sub('Separated responsibilities')
p('The Swing interface gathers input and displays errors. StudioFacade exposes complete use cases and coordinates immutable domain state, pricing, persistence, exports and notifications. Domain records validate fields and StudioState validates cross-record relationships. The storage interface isolates JSON and file operations. Exporters depend on an EventReport rather than UI widgets.')
table([
    ['Layer', 'Main classes', 'Responsibility'],
    ['UI', 'StudioFrame, UiTheme', 'Forms, lists, selection, dialogs and user feedback'],
    ['Application', 'StudioFacade, SampleData', 'CRUD, lifecycle, relationships and commit order'],
    ['Domain', 'Event, Participant, Booking, StudioState', 'Immutable records and validation'],
    ['Pricing', 'PriceCalculator, components, strategies, Quote', 'Composable per-guest price and discount rules'],
    ['Infrastructure', 'StudioRepository, JsonStudioRepository', 'UTF-8 JSON, writer lock and file replacement'],
    ['Events and output', 'EventBus, StudioObserver, ExportCreator, exporters', 'Post-commit refresh and saved final results']
], [85, 205, WIDTH - 290])
sub('Lifecycle and integrity')
p('Draft events may be edited, deleted, opened or cancelled. Open events may be edited, completed or cancelled. Completion requires confirmed attendees. Completed and cancelled events are read-only. A participant may have one confirmed booking per event. Booking cancellation releases seats but preserves history. Participants with any booking history cannot be deleted.')
sub('Persistence and failures')
p('The facade constructs and validates a next state, writes it to a temporary JSON file, flushes and replaces the stored file, installs the in-memory snapshot, then publishes an event. A failed write does not change state or publish success. Atomic replacement is used when supported, with a replacement fallback. An exclusive writer lock prevents two running instances from writing the same workspace. Corrupt or unsupported data is reported and never silently replaced with samples.')

page('Six integrated design patterns')
p('All six patterns below are in the application code and exercised by working GUI scenarios. Framework-internal implementations are not counted.', 'ReportSmall')
table([
    ['Pattern', 'Problem and participants', 'Demonstration', 'Student'],
    ['Builder', 'Validated event construction: Event, Event.Builder.', 'Create/edit a configured event.', 'Unassigned'],
    ['Factory Method', 'Vary export product within a shared workflow: ExportCreator, concrete CSV/HTML creators, ReportExporter and products.', 'Save both export formats.', 'Unassigned'],
    ['Decorator', 'Compose optional services: PriceComponent, PackagePrice, ExtraDecorator.', 'Add several extras; show price and lines.', 'Unassigned'],
    ['Facade', 'Coordinate complete operations: StudioFacade, repository, calculator, bus and creator.', 'Book or cancel; show stored result and refresh.', 'Unassigned'],
    ['Strategy', 'Replace pricing algorithm at runtime: PricingStrategy, StandardPricing, GroupPricing, PriceCalculator.', 'Switch rule for 12 confirmed guests.', 'Unassigned'],
    ['Observer', 'React to committed changes: EventBus, StudioObserver, StudioEvent and frame callback.', 'Book seats; show quote, guest list and Activity.', 'Unassigned']
], [75, 215, 133, WIDTH - 423])
p('Category coverage: Builder and Factory Method are creational; Decorator and Facade are structural; Strategy and Observer are behavioral. Each addresses a different problem in the same application.')
sub('Responsibility status')
p('No member has been assigned independent ownership retrospectively. The assignment requires 2-4 students and at least two independently implemented patterns per student. Only the requester Sanzhar Karaulov is known. The proposed allocation in CONTRIBUTIONS.md is a planning aid and must be replaced with actual contribution evidence before submission.')

page('UML domain and pricing participants', 'landscape')
image('diagrams/class-domain-pricing.png', LW - 2 * MARGIN, 405)
p('Event and Participant are connected through Booking. Builder creates the immutable event. Pricing composes PriceComponent decorators and selects a PricingStrategy. This view continues into the application view through Event and PriceCalculator.', 'Caption')

page('UML application and output participants', 'landscape')
image('diagrams/class-diagram.png', LW - 2 * MARGIN, 395)
p('StudioFacade coordinates the state, repository and pricing. The custom Observer callback refreshes the frame. Concrete creators override the Factory Method to produce CSV or HTML exporters. Editable sources are included under docs/diagrams.', 'Caption')

page('UML main booking sequence')
image('diagrams/sequence-diagram.png', WIDTH, 620)
p('The sequence shows a successful booking, price refresh and post-commit notification, with explicit invalid-input and storage-failure branches. The current UI is synchronous; off-thread notifications are marshalled onto Swing\'s event-dispatch thread.', 'Caption')

page('Interface and saved result')
image('screenshots/events.png', WIDTH, 290)
p('Initial sample event: configured Studio package with Catering and Photography, 12 confirmed guests and a Group total of 93,960 KZT.', 'Caption')
image('screenshots/booked-event.png', WIDTH, 290)
p('Real GUI journey: Student makers evening, Essential plus Catering, ten guests, 49,500 KZT. Guest table, available seats and total reflect the saved booking.', 'Caption')

page('Forms and validation')
form_images = []
for name in ('configuration.png', 'validation.png'):
    path = DOCS / 'screenshots' / name
    with PillowImage.open(path) as source:
        iw, ih = source.size
    form_images.append(Image(str(path), width=240, height=ih * 240 / iw))
forms = Table([form_images], colWidths=[WIDTH / 2, WIDTH / 2], hAlign='LEFT')
forms.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LEFTPADDING', (0, 0), (-1, -1), 0)]))
story.append(forms)
p('Left: service and pricing configuration. Right: required-title validation leaves the form open and prevents a save. Impossible dates and invalid spinner values are tested through the same controls.', 'Caption')
image('screenshots/people.png', WIDTH, 275)
p('Participant data is edited in the People tab and reused in booking forms. Screenshots are rendered from visible Swing integration windows; no simulated application screen was used.', 'Caption')

page('Testing and observed outcomes')
p('Verification used ' + escape(evidence['environment']) + '. Commands: <font name="Courier">mvn clean verify</font> and <font name="Courier">mvn -Pgui test</font>. Source targets Java 17; direct Java 17 runtime and other desktop platforms were not tested.', 'ReportSmall')
table([['Suite', 'Passed', 'Failures / errors / skipped']] + [[s['suite'], str(s['tests']), '0 / 0 / 0'] for s in evidence['suites']], [225, 65, WIDTH - 290])
table([
    ['Input and scenario', 'Expected', 'Actual'],
    ['Studio + Catering + Photography, Group, ten seats; export and reload.', '78,300 KZT, completed, ten seats persist.', 'Pass: exact total, HTML guest and restored state.'],
    ['Sample 12 guests; Standard then Group.', '104,400 then 93,960 KZT.', 'Pass: both totals exact.'],
    ['Subscribe, save Observed, unsubscribe, delete.', 'One event; no event after unsubscribe.', 'Pass: one message and expected timestamp.'],
    ['Save Unicode participant; reload JSON.', 'All fields and relationships equal.', 'Pass: complete state equals saved state.'],
    ['Blank title and impossible 2026-02-30.', 'Reject without saving.', 'Pass: domain errors and visible form feedback.'],
    ['Capacity 0 / 501; past event date.', 'Reject bounds and past date.', 'Pass: unchanged state.'],
    ['Broken email / DANA@EXAMPLE.COM duplicate.', 'Reject invalid and duplicate email.', 'Pass: both rejected.'],
    ['Ten of ten seats taken; add one / lower capacity to nine.', 'Reject overcapacity; retain original.', 'Pass: no state change.']
], [222, 132, WIDTH - 354])
p('TEST-SCENARIOS.md records fourteen scenarios with inputs, expected results, actual results and automated method names. Additional tests cover failed writes, corrupted files, writer exclusion, exact discount boundary, cancellation cascade, escaped exports and observer failure. The three GUI tests cover both file choosers, lifecycle, validation and refresh.', 'ReportSmall')

page('Code excerpts construction and pricing')
p('These excerpts come from the implemented Java sources. The form builds an event, the calculator composes extras, and the saved pricing mode selects the interchangeable rule.', 'ReportSmall')
sub('Builder in domain/Event.java')
code('domain/Event.java', 'public Event build()', 'return new Event(id, title, date, venue, capacity, packageTier, extras, pricingMode, status);\n        }', 118)
p('Fluent builder fields are supplied by the actual event form; the record constructor validates every event, including objects loaded from JSON.', 'ReportSmall')
sub('Decorator in pricing/ExtraDecorator.java')
code('pricing/ExtraDecorator.java', 'public record ExtraDecorator', 'return List.copyOf(result);\n    }\n}', 118)
p('The same component interface allows nested wrappers to add unit prices and description lines without creating a subclass for each combination.', 'ReportSmall')
sub('Strategy in pricing/GroupPricing.java')
code('pricing/GroupPricing.java', 'public final class GroupPricing', 'return "Group rate: 10% off at 10 guests"; }\n}', 118)
p('PriceCalculator selects StandardPricing or GroupPricing from the saved event mode. Subtotal and discount are calculated with BigDecimal and rounded to two decimal places.', 'ReportSmall')

page('Code excerpts workflow and reactions')
sub('Factory Method in export/ExportCreator.java')
code('export/ExportCreator.java', 'protected abstract ReportExporter createExporter();', 'public abstract String extension();')
code('export/CsvExportCreator.java', 'public final class CsvExportCreator', 'public String extension() { return "csv"; }\n}', 118)
p('The shared final export workflow calls createExporter().render(report); each concrete creator returns its format-specific product. The UI invokes this workflow through the facade.', 'ReportSmall')
sub('Facade commit in service/StudioFacade.java')
code('service/StudioFacade.java', 'private void commit', 'eventBus.publish(new StudioEvent(Instant.now(clock), message));\n    }', 118)
p('Persistence precedes state installation and publication. This order makes a failed write observable without displaying a successful state that was never saved.', 'ReportSmall')
sub('Observer in events/EventBus.java')
code('events/EventBus.java', 'public AutoCloseable subscribe', 'return () -> observers.remove(observer);\n    }')
code('events/EventBus.java', 'public void publish', 'System.err.println("A subscriber could not refresh: " + exception.getMessage());\n            }\n        }\n    }', 118)
p('The frame supplies a custom observer that refreshes its models, quote and activity. It unsubscribes on close. Subscriber failures are isolated after the state has already been committed.', 'ReportSmall')

page('Contributions disclosure and delivery')
sub('Individual contributions')
p('Sanzhar Karaulov requested this implementation. No additional team members or genuine independent student changes were supplied. Codex assisted with all implementation areas in this session. Personal ownership remains unassigned. No student is credited here with having independently written the assisted code.')
table([
    ['Proposed allocation only', 'Patterns', 'Actual evidence'],
    ['Sanzhar Karaulov, if confirmed by team', 'Builder, Decorator, Strategy', 'Not yet recorded'],
    ['Teammate to be identified', 'Facade, Factory Method, Observer', 'Not yet recorded']
], [205, 150, WIDTH - 355])
p('This suggested two-person split meets the numerical allocation only if those students genuinely implement and can explain their contributions. More members require a revised allocation. Independent work and course-policy compliance are personal requirements that automated tests do not establish.')
sub('AI and external materials')
p('Tools used: Codex by OpenAI for code, tests, documentation and troubleshooting; Java standard library and Swing for the application; Gson 2.11.0 for JSON; JUnit Jupiter 5.11.4 for tests; Apache Maven plugins for build and packaging; PlantUML 1.2025.10 for diagrams; Python ReportLab and Pillow for this report. The supplied FINAL PROJECT.docx was the assignment authority. Third-party libraries and their license notices are retained in the executable distribution.')
p('Reference projects: Gson at github.com/google/gson, JUnit at junit.org/junit5, Apache Maven at maven.apache.org, Java API at docs.oracle.com/en/java, PlantUML at plantuml.com. These are tool references, not claims of independent authorship.', 'ReportSmall')
sub('Delivery and real history')
p('README contains setup, launch, sample-data and test commands. Source and executable ZIPs are generated by scripts/package_submission.py. Editable UML, screenshots, test evidence and this PDF are included. The configured repository is https://github.com/unsaiddream/SDP-FINAL. The repository began with no commits; this session creates no fabricated history and pushes nothing. The team must review the real remote state and submit the required ZIP, repository link and PDF through Moodle.')

page('Conclusion and future work')
sub('Conclusion')
p('Event Studio implements the assignment\'s feasible application requirements in one working Java desktop product. The organizer can configure an event, manage participants, reserve and cancel seats, choose a pricing behavior, see automatic reactions, reopen persisted data, and export a meaningful result. Builder, Factory Method, Decorator, Facade, Strategy and Observer all participate in those working operations.')
p('The verified engineering result is a successful executable build, 27 passing business/persistence tests and three passing visible GUI integration tests. Concrete input/expected/actual evidence, real interface captures, UML views and source excerpts accompany the implementation. These results support review of the shared application, while leaving individual course obligations explicit.')
sub('Future work')
p('A larger version could introduce editable service catalogs, event-wide fixed fees, time-based pricing strategies, a database-backed multi-user repository, persistent audit activity, calendar integration and accessible keyboard shortcuts. Repository and strategy interfaces provide extension points, but synchronization, payment collection and authentication would require additional domain and infrastructure work.')
sub('Remaining before course submission')
p('Confirm a team of 2-4 and actual independent pattern responsibilities. Review and adapt the assisted implementation under the instructor\'s AI policy. Collect genuine commits and contribution evidence. Perform native usability checks on the intended defense machine, rehearse live modifications, confirm the course Week 9 deadline, submit through Moodle and attend the mandatory Week 10 individual defense. Weekly presentations and historic development cannot be reconstructed by generated documents.')
p('The current implementation is local and single-user; its service catalog is fixed, activity is session-only, prices are estimates in KZT, and no real payments or messages are sent. Source targets Java 17, with runtime verification on Java 25/27 and macOS arm64. Those limits should be included in the team\'s defense explanation.', 'ReportSmall')

doc.build(story)
print(OUT)
