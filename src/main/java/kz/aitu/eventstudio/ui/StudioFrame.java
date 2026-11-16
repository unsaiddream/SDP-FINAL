package kz.aitu.eventstudio.ui;

import kz.aitu.eventstudio.domain.*;
import kz.aitu.eventstudio.domain.Event;
import kz.aitu.eventstudio.events.EventBus;
import kz.aitu.eventstudio.export.CsvExportCreator;
import kz.aitu.eventstudio.export.ExportCreator;
import kz.aitu.eventstudio.export.HtmlExportCreator;
import kz.aitu.eventstudio.service.StudioFacade;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public final class StudioFrame extends JFrame {
    private static final Color INK = new Color(25, 48, 73);
    private static final Color BLUE = new Color(48, 83, 184);
    private static final Color BACKGROUND = new Color(240, 245, 251);
    private final StudioFacade studio;
    private final AutoCloseable subscription;
    private final DefaultTableModel eventModel = model("Event", "Date", "Status");
    private final DefaultTableModel peopleModel = model("Name", "Email");
    private final DefaultTableModel bookingModel = model("Participant", "Seats", "Status");
    private final JTable eventTable = table(eventModel, "eventTable");
    private final JTable peopleTable = table(peopleModel, "peopleTable");
    private final JTable bookingTable = table(bookingModel, "bookingTable");
    private final JLabel selectedTitle = new JLabel("Select an event");
    private final JLabel eventMeta = new JLabel("Create an event to get started.");
    private final JLabel total = new JLabel("0.00 KZT");
    private final JLabel occupancy = new JLabel("0 guests");
    private final JLabel status = new JLabel("Sample workspace ready. Changes are saved automatically.");
    private final JTextArea breakdown = new JTextArea();
    private final DefaultListModel<String> activity = new DefaultListModel<>();
    private final List<JButton> eventActions = new ArrayList<>();
    private List<Event> eventRows = List.of();
    private List<Participant> peopleRows = List.of();
    private List<Booking> bookingRows = List.of();
    private String selectedId;
    private boolean refreshing;

    public StudioFrame(StudioFacade studio, EventBus bus) {
        super("Event Studio | Campus events");
        this.studio = studio;
        setName("studioFrame");
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(210);
        eventTable.getColumnModel().getColumn(1).setMinWidth(92);
        eventTable.getColumnModel().getColumn(1).setMaxWidth(98);
        eventTable.getColumnModel().getColumn(2).setMinWidth(74);
        eventTable.getColumnModel().getColumn(2).setMaxWidth(80);
        eventTable.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                cell.setToolTipText(String.valueOf(value));
                return cell;
            }
        });
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 740));
        setSize(1280, 850);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(24, 28, 16, 28));
        JPanel header = panel(new BorderLayout());
        JLabel brand = new JLabel("Event Studio");
        brand.setFont(new Font("Avenir Next", Font.BOLD, 32));
        brand.setForeground(INK);
        header.add(brand, BorderLayout.WEST);
        JLabel tagline = new JLabel("Plan a gathering. Bring people together.");
        tagline.setForeground(INK);
        header.add(tagline, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("workspaceTabs");
        tabs.addTab("Events", eventsPanel());
        tabs.addTab("People", peoplePanel());
        JList<String> activityList = new JList<>(activity);
        activityList.setName("activityList");
        activityList.setFixedCellHeight(32);
        JPanel activityPanel = panel(new BorderLayout(0, 12));
        activityPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
        activityPanel.add(new JLabel("Session activity updates after a change is saved. Exported files are recorded here too."), BorderLayout.NORTH);
        activityPanel.add(new JScrollPane(activityList));
        tabs.addTab("Activity", activityPanel);
        root.add(tabs, BorderLayout.CENTER);
        status.setName("statusMessage");
        status.setForeground(INK);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        eventTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshing) {
                int row = eventTable.getSelectedRow();
                selectedId = row < 0 ? null : eventRows.get(row).id();
                refreshDetail();
            }
        });
        subscription = bus.subscribe(event -> {
            Runnable refresh = () -> {
                activity.add(0, event.message());
                if (activity.size() > 100) activity.remove(100);
                message(event.message() + " | Saved locally", false);
                refreshFromState();
            };
            if (SwingUtilities.isEventDispatchThread()) refresh.run(); else SwingUtilities.invokeLater(refresh);
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                try {
                    subscription.close();
                    studio.close();
                } catch (Exception exception) {
                    System.err.println("Could not release the workspace: " + exception.getMessage());
                }
            }
        });
        refreshFromState();
    }

    private JPanel eventsPanel() {
        JPanel left = panel(new BorderLayout(0, 12));
        left.setBorder(new EmptyBorder(18, 16, 18, 16));
        JPanel leftHeader = panel(new BorderLayout());
        leftHeader.add(new JLabel("Your events"), BorderLayout.WEST);
        leftHeader.add(button("New event", "newEvent", () -> eventDialog(null)), BorderLayout.EAST);
        left.add(leftHeader, BorderLayout.NORTH);
        left.add(new JScrollPane(eventTable));
        JPanel detail = panel(new BorderLayout(0, 14));
        detail.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel heading = panel(new GridLayout(0, 1, 0, 6));
        selectedTitle.setName("selectedEventTitle");
        selectedTitle.setFont(new Font("Avenir Next", Font.BOLD, 24));
        selectedTitle.setForeground(INK);
        heading.add(selectedTitle);
        heading.add(eventMeta);
        JPanel actions = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(eventButton("Edit", "editEvent", () -> eventDialog(selectedEvent())));
        actions.add(eventButton("Open bookings", "openEvent", () -> changeStatus(EventStatus.OPEN)));
        actions.add(eventButton("Complete", "completeEvent", () -> changeStatus(EventStatus.COMPLETED)));
        actions.add(eventButton("Cancel event", "cancelEvent", () -> {
            if (confirm("Cancel this event and all its bookings?")) attempt(() -> studio.changeStatus(selectedId, EventStatus.CANCELLED));
        }));
        actions.add(eventButton("Delete draft", "deleteEvent", () -> {
            if (confirm("Delete this draft event?")) attempt(() -> studio.deleteEvent(selectedId));
        }));
        JPanel top = panel(new BorderLayout(0, 12));
        top.add(heading, BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);
        detail.add(top, BorderLayout.NORTH);
        JPanel body = panel(new BorderLayout(0, 18));
        JPanel quote = new JPanel(new BorderLayout(16, 12));
        quote.setBackground(new Color(225, 234, 251));
        quote.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel numbers = panel(new BorderLayout());
        numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
        numbers.add(new JLabel("Estimated total"));
        total.setName("quoteTotal");
        total.setForeground(BLUE);
        total.setFont(new Font("Avenir Next", Font.BOLD, 30));
        numbers.add(total);
        occupancy.setName("occupancy");
        numbers.add(occupancy);
        quote.add(numbers, BorderLayout.NORTH);
        breakdown.setName("quoteBreakdown");
        breakdown.setEditable(false);
        breakdown.setOpaque(false);
        breakdown.setLineWrap(true);
        breakdown.setWrapStyleWord(true);
        breakdown.setRows(6);
        breakdown.setForeground(INK);
        quote.add(breakdown, BorderLayout.CENTER);
        body.add(quote, BorderLayout.NORTH);
        JPanel guests = panel(new BorderLayout(0, 10));
        JPanel guestHeading = panel(new BorderLayout());
        guestHeading.add(new JLabel("Guest list"), BorderLayout.WEST);
        guestHeading.add(eventButton("Book seats", "bookSeats", () -> bookingDialog(null)), BorderLayout.EAST);
        guests.add(guestHeading, BorderLayout.NORTH);
        guests.add(new JScrollPane(bookingTable));
        JPanel guestActions = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        guestActions.add(eventButton("Edit booking", "editBooking", () -> bookingDialog(selectedBooking())));
        guestActions.add(eventButton("Cancel booking", "cancelBooking", () -> {
            Booking booking = selectedBooking();
            if (confirm("Cancel the selected booking and release its seats?")) attempt(() -> studio.cancelBooking(booking.id()));
        }));
        guests.add(guestActions, BorderLayout.SOUTH);
        body.add(guests);
        detail.add(body);
        JPanel exports = panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        exports.add(eventButton("Export CSV", "exportCsv", () -> export(new CsvExportCreator())));
        exports.add(eventButton("Export HTML", "exportHtml", () -> export(new HtmlExportCreator())));
        detail.add(exports, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detail);
        split.setResizeWeight(0.33);
        split.setDividerLocation(385);
        split.setBorder(null);
        JPanel result = panel(new BorderLayout());
        result.add(split);
        return result;
    }

    private JPanel peoplePanel() {
        JPanel result = panel(new BorderLayout(0, 16));
        result.setBorder(new EmptyBorder(18, 18, 18, 18));
        result.add(new JLabel("Keep participant details here, then book their seats from an open event."), BorderLayout.NORTH);
        result.add(new JScrollPane(peopleTable));
        JPanel actions = panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(button("Add participant", "newParticipant", () -> participantDialog(null)));
        actions.add(button("Edit participant", "editParticipant", () -> participantDialog(selectedParticipant())));
        actions.add(button("Delete participant", "deleteParticipant", () -> {
            Participant participant = selectedParticipant();
            if (confirm("Delete this participant? People with booking history are kept.")) attempt(() -> studio.deleteParticipant(participant.id()));
        }));
        result.add(actions, BorderLayout.SOUTH);
        return result;
    }

    private void refreshFromState() {
        refreshing = true;
        try {
            eventRows = studio.snapshot().events().stream().sorted(Comparator.comparing(Event::date).thenComparing(Event::title)).toList();
            eventModel.setRowCount(0);
            int selection = -1;
            for (int i = 0; i < eventRows.size(); i++) {
                Event event = eventRows.get(i);
                eventModel.addRow(new Object[]{event.title(), event.date(), event.status()});
                if (event.id().equals(selectedId)) selection = i;
            }
            if (selection < 0 && !eventRows.isEmpty()) selection = 0;
            selectedId = selection < 0 ? null : eventRows.get(selection).id();
            if (selection >= 0) eventTable.setRowSelectionInterval(selection, selection);
            String personId = peopleTable.getSelectedRow() < 0 || peopleTable.getSelectedRow() >= peopleRows.size()
                    ? null : peopleRows.get(peopleTable.getSelectedRow()).id();
            peopleRows = studio.snapshot().participants().stream().sorted(Comparator.comparing(Participant::name)).toList();
            peopleModel.setRowCount(0);
            for (int i = 0; i < peopleRows.size(); i++) {
                Participant person = peopleRows.get(i);
                peopleModel.addRow(new Object[]{person.name(), person.email()});
                if (person.id().equals(personId)) peopleTable.setRowSelectionInterval(i, i);
            }
        } finally {
            refreshing = false;
        }
        refreshDetail();
    }

    private void refreshDetail() {
        for (JButton action : eventActions) action.setEnabled(selectedId != null);
        bookingModel.setRowCount(0);
        bookingRows = List.of();
        if (selectedId == null) {
            selectedTitle.setText("Plan your next gathering");
            eventMeta.setText("Use New event to create a draft.");
            total.setText("0.00 KZT");
            occupancy.setText("No event selected");
            breakdown.setText("");
            return;
        }
        Event event = studio.event(selectedId);
        var quote = studio.quote(selectedId);
        selectedTitle.setText(event.title());
        selectedTitle.setToolTipText(event.title());
        eventMeta.setText(event.date() + " | " + event.venue() + " | " + event.status());
        eventMeta.setToolTipText(eventMeta.getText());
        total.setText(money(quote.total()));
        occupancy.setText(quote.guests() + " of " + event.capacity() + " seats booked | " + (event.capacity() - quote.guests()) + " available");
        breakdown.setText(String.join("\n", quote.lines()) + "\n" + quote.strategy() + "\nSubtotal: " + money(quote.subtotal()) + " | Discount: " + money(quote.discount()));
        bookingRows = studio.snapshot().bookings().stream().filter(b -> b.eventId().equals(selectedId)).toList();
        for (Booking booking : bookingRows) bookingModel.addRow(new Object[]{studio.participant(booking.participantId()).name(), booking.seats(), booking.status()});
        boolean editable = event.status() == EventStatus.DRAFT || event.status() == EventStatus.OPEN;
        for (JButton action : eventActions) {
            switch (action.getName()) {
                case "editEvent", "cancelEvent" -> action.setEnabled(editable);
                case "openEvent", "deleteEvent" -> action.setEnabled(event.status() == EventStatus.DRAFT);
                case "completeEvent", "bookSeats", "editBooking", "cancelBooking" -> action.setEnabled(event.status() == EventStatus.OPEN);
                default -> action.setEnabled(true);
            }
        }
    }

    private void eventDialog(Event existing) {
        JDialog dialog = dialog(existing == null ? "New event" : "Edit event", "eventDialog");
        JPanel fields = fields();
        JTextField title = text("eventTitle", existing == null ? "" : existing.title());
        JTextField date = text("eventDate", existing == null ? LocalDate.now().plusDays(1).toString() : existing.date());
        JTextField venue = text("eventVenue", existing == null ? "" : existing.venue());
        JSpinner capacity = spinner("eventCapacity", existing == null ? 20 : existing.capacity());
        JComboBox<PackageTier> tier = combo("packageTier", PackageTier.values());
        tier.setSelectedItem(existing == null ? PackageTier.ESSENTIAL : existing.packageTier());
        JComboBox<PricingMode> mode = combo("pricingMode", PricingMode.values());
        mode.setSelectedItem(existing == null ? PricingMode.STANDARD : existing.pricingMode());
        addField(fields, "Event title", title);
        addField(fields, "Date (YYYY-MM-DD)", date);
        addField(fields, "Venue", venue);
        addField(fields, "Capacity (1-500)", capacity);
        addField(fields, "Package", tier);
        EnumMap<Extra, JCheckBox> extras = new EnumMap<>(Extra.class);
        JPanel extraOptions = panel(new GridLayout(0, 1));
        for (Extra extra : Extra.values()) {
            JCheckBox box = new JCheckBox(extra.toString(), existing != null && existing.extras().contains(extra));
            box.setName("extra" + extra.name());
            extraOptions.add(box);
            extras.put(extra, box);
        }
        addField(fields, "Optional services", extraOptions);
        addField(fields, "Pricing", mode);
        JLabel hint = new JLabel("Totals use confirmed guests. The group discount starts at 10 guests.");
        hint.setFont(hint.getFont().deriveFont(12f));
        fields.add(hint);
        JLabel error = errorLabel();
        finishDialog(dialog, fields, error, "Save event", "saveEvent", () -> {
            capacity.commitEdit();
            Event.Builder builder = existing == null ? new Event.Builder() : existing.toBuilder();
            Event candidate = builder.title(title.getText()).date(date.getText()).venue(venue.getText()).capacity((Integer) capacity.getValue())
                    .packageTier((PackageTier) tier.getSelectedItem()).pricingMode((PricingMode) mode.getSelectedItem())
                    .extras(extras.entrySet().stream().filter(e -> e.getValue().isSelected()).map(java.util.Map.Entry::getKey).toList()).build();
            String previous = selectedId;
            selectedId = candidate.id();
            try {
                studio.saveEvent(candidate);
            } catch (Exception exception) {
                selectedId = previous;
                throw exception;
            }
        });
    }

    private void participantDialog(Participant existing) {
        JDialog dialog = dialog(existing == null ? "Add participant" : "Edit participant", "participantDialog");
        JPanel fields = fields();
        JTextField name = text("participantName", existing == null ? "" : existing.name());
        JTextField email = text("participantEmail", existing == null ? "" : existing.email());
        addField(fields, "Full name", name);
        addField(fields, "Email", email);
        finishDialog(dialog, fields, errorLabel(), "Save participant", "saveParticipant",
                () -> studio.saveParticipant(existing == null ? null : existing.id(), name.getText(), email.getText()));
    }

    private void bookingDialog(Booking existing) {
        Event event = selectedEvent();
        if (event.status() != EventStatus.OPEN) throw new ValidationException("Open bookings for this event first.");
        if (existing != null && existing.status() != BookingStatus.CONFIRMED) throw new ValidationException("Cancelled bookings cannot be edited. Create a new booking.");
        if (peopleRows.isEmpty()) throw new ValidationException("Add a participant in the People tab first.");
        JDialog dialog = dialog(existing == null ? "Book seats" : "Edit booking", "bookingDialog");
        JPanel fields = fields();
        JComboBox<Participant> person = combo("bookingParticipant", peopleRows.toArray(Participant[]::new));
        if (existing != null) person.setSelectedItem(studio.participant(existing.participantId()));
        JSpinner seats = spinner("bookingSeats", existing == null ? 1 : existing.seats());
        addField(fields, "Participant", person);
        addField(fields, "Seats", seats);
        fields.add(new JLabel((event.capacity() - studio.confirmedSeats(event.id())) + " seats currently available."));
        finishDialog(dialog, fields, errorLabel(), "Save booking", "saveBooking", () -> {
            seats.commitEdit();
            Participant selected = (Participant) person.getSelectedItem();
            studio.saveBooking(existing == null ? null : existing.id(), event.id(), selected.id(), (Integer) seats.getValue());
        });
    }

    private void export(ExportCreator creator) throws IOException {
        Event event = selectedEvent();
        JFileChooser chooser = new JFileChooser();
        chooser.setName("exportChooser");
        chooser.setDialogTitle("Save event report");
        chooser.setFileFilter(new FileNameExtensionFilter(creator.extension().toUpperCase(java.util.Locale.ROOT) + " report", creator.extension()));
        chooser.setSelectedFile(new java.io.File("event-" + event.id().substring(0, 8) + "." + creator.extension()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (!target.toString().toLowerCase(java.util.Locale.ROOT).endsWith("." + creator.extension())) target = Path.of(target + "." + creator.extension());
        if (Files.exists(target) && !confirm("Replace the existing report file?")) return;
        studio.export(event.id(), creator, target);
        message("Report saved: " + target.toAbsolutePath(), false);
    }

    private void changeStatus(EventStatus target) { attempt(() -> studio.changeStatus(selectedEvent().id(), target)); }

    private Event selectedEvent() {
        if (selectedId == null) throw new ValidationException("Select an event first.");
        return studio.event(selectedId);
    }

    private Participant selectedParticipant() {
        int row = peopleTable.getSelectedRow();
        if (row < 0) throw new ValidationException("Select a participant first.");
        return peopleRows.get(row);
    }

    private Booking selectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) throw new ValidationException("Select a booking first.");
        return bookingRows.get(row);
    }

    private JDialog dialog(String title, String name) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setName(name);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        return dialog;
    }

    private void finishDialog(JDialog dialog, JPanel fields, JLabel error, String label, String name, Action action) {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(new EmptyBorder(20, 24, 20, 24));
        body.add(fields);
        JPanel footer = panel(new BorderLayout(0, 12));
        footer.add(error, BorderLayout.NORTH);
        JPanel actions = panel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(button("Cancel", "dismissDialog", dialog::dispose));
        JButton save = button(label, name, () -> {
            try {
                action.run();
                dialog.dispose();
            } catch (Exception exception) {
                error.setText(exception instanceof java.text.ParseException ? "Enter a whole number from 1 to 500." : friendly(exception));
                dialog.pack();
            }
        });
        actions.add(save);
        footer.add(actions, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(save);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(580, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton eventButton(String label, String name, Action action) {
        JButton button = button(label, name, action);
        eventActions.add(button);
        return button;
    }

    private JButton button(String label, String name, Action action) {
        JButton button = new JButton(label);
        button.setName(name);
        button.getAccessibleContext().setAccessibleName(label);
        button.addActionListener(e -> attempt(action));
        return button;
    }

    private void attempt(Action action) {
        try { action.run(); } catch (Exception exception) { message(friendly(exception), true); }
    }

    private static String friendly(Exception exception) {
        if (exception instanceof IOException) return "Could not save. Check the folder permissions and free disk space. " + exception.getMessage();
        return exception.getMessage() == null ? "The action could not be completed. Check your input." : exception.getMessage();
    }

    private void message(String text, boolean error) {
        status.setForeground(error ? new Color(165, 35, 53) : INK);
        status.setText(text);
        status.setToolTipText(text);
        status.getAccessibleContext().setAccessibleDescription(error ? "Error: " + text : text);
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirm action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel fields() {
        JPanel panel = panel(new BorderLayout());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static void addField(JPanel fields, String label, JComponent component) {
        JPanel row = panel(new BorderLayout(0, 5));
        JLabel title = new JLabel(label);
        title.setLabelFor(component);
        row.add(title, BorderLayout.NORTH);
        row.add(component);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        fields.add(row);
        fields.add(Box.createVerticalStrut(10));
    }

    private static JTextField text(String name, String value) {
        JTextField field = new JTextField(value, 30);
        field.setName(name);
        return field;
    }

    private static JSpinner spinner(String name, int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 1, 500, 1));
        spinner.setName(name);
        ((JSpinner.NumberEditor) spinner.getEditor()).getTextField().setColumns(6);
        return spinner;
    }

    private static <T> JComboBox<T> combo(String name, T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setName(name);
        return combo;
    }

    private static JLabel errorLabel() {
        JLabel label = new JLabel(" ");
        label.setName("formError");
        label.setForeground(new Color(165, 35, 53));
        return label;
    }

    private static JTable table(DefaultTableModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(10, 0));
        table.setSelectionBackground(new Color(215, 228, 250));
        table.setSelectionForeground(INK);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static String money(BigDecimal amount) { return String.format(java.util.Locale.US, "%,.2f KZT", amount); }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }
}
