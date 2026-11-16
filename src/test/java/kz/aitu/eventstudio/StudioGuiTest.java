package kz.aitu.eventstudio;

import kz.aitu.eventstudio.domain.*;
import kz.aitu.eventstudio.events.EventBus;
import kz.aitu.eventstudio.service.StudioFacade;
import kz.aitu.eventstudio.storage.JsonStudioRepository;
import kz.aitu.eventstudio.ui.StudioFrame;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudioGuiTest {
    @TempDir Path directory;
    private StudioFrame frame;
    private StudioFacade studio;

    @BeforeEach
    void openWindow() throws Exception {
        EventBus bus = new EventBus();
        studio = new StudioFacade(new JsonStudioRepository(directory.resolve("studio.json")), bus, Clock.systemDefaultZone());
        edt(() -> {
            kz.aitu.eventstudio.ui.UiTheme.install();
            frame = new StudioFrame(studio, bus);
            frame.setVisible(true);
            return null;
        });
    }

    @AfterEach
    void closeWindows() throws Exception {
        edt(() -> {
            for (Window window : Window.getWindows()) window.dispose();
            return null;
        });
        edt(() -> null);
    }

    @Test
    @Order(1)
    void userCreatesConfiguresBooksAndExportsThroughActualControls() throws Exception {
        screenshot(frame, "events.png");
        edt(() -> { find(frame, "workspaceTabs", JTabbedPane.class).setSelectedIndex(1); return null; });
        openDialog("newParticipant", "participantDialog");
        JDialog personDialog = visibleDialog("participantDialog");
        fill(personDialog, "participantName", "Taylor Demo");
        fill(personDialog, "participantEmail", "taylor@example.com");
        click(personDialog, "saveParticipant");
        assertEquals(4, studio.snapshot().participants().size());
        screenshot(frame, "people.png");
        edt(() -> { find(frame, "workspaceTabs", JTabbedPane.class).setSelectedIndex(0); return null; });
        openDialog("newEvent", "eventDialog");
        JDialog eventDialog = visibleDialog("eventDialog");
        fill(eventDialog, "eventTitle", "Student makers evening");
        fill(eventDialog, "eventVenue", "Campus workshop");
        fill(eventDialog, "eventDate", LocalDate.now().plusDays(10).toString());
        edt(() -> {
            find(eventDialog, "pricingMode", JComboBox.class).setSelectedItem(PricingMode.GROUP);
            find(eventDialog, "extraCATERING", JCheckBox.class).setSelected(true);
            return null;
        });
        screenshot(eventDialog, "configuration.png");
        click(eventDialog, "saveEvent");
        Event created = studio.snapshot().events().stream().filter(e -> e.title().equals("Student makers evening")).findFirst().orElseThrow();
        assertEquals(created.title(), edt(() -> find(frame, "selectedEventTitle", JLabel.class).getText()));
        click(frame, "openEvent");
        assertEquals(EventStatus.OPEN, studio.event(created.id()).status());
        openDialog("bookSeats", "bookingDialog");
        JDialog bookingDialog = visibleDialog("bookingDialog");
        edt(() -> {
            var choices = find(bookingDialog, "bookingParticipant", JComboBox.class);
            choices.setSelectedItem(studio.snapshot().participants().stream().filter(p -> p.email().equals("taylor@example.com")).findFirst().orElseThrow());
            find(bookingDialog, "bookingSeats", JSpinner.class).setValue(10);
            return null;
        });
        click(bookingDialog, "saveBooking");
        assertEquals("49,500.00 KZT", edt(() -> find(frame, "quoteTotal", JLabel.class).getText()));
        assertEquals(10, studio.confirmedSeats(created.id()));
        screenshot(frame, "booked-event.png");
        Path csv = directory.resolve("ui-result.csv");
        Path html = directory.resolve("ui-result.html");
        exportThroughChooser("exportCsv", csv);
        exportThroughChooser("exportHtml", html);
        assertTrue(Files.readString(csv).contains("49500.00"));
        assertTrue(Files.readString(html).contains("Taylor Demo"));
        click(frame, "completeEvent");
        assertEquals(EventStatus.COMPLETED, studio.event(created.id()).status());
        assertFalse(edt(() -> find(frame, "bookSeats", JButton.class).isEnabled()));
        edt(() -> { find(frame, "workspaceTabs", JTabbedPane.class).setSelectedIndex(2); return null; });
        assertTrue(edt(() -> find(frame, "activityList", JList.class).getModel().getSize()) >= 7);
        screenshot(frame, "activity.png");
        edt(() -> { frame.dispose(); return null; });
        edt(() -> null);
        try (StudioFacade reloaded = new StudioFacade(new JsonStudioRepository(directory.resolve("studio.json")), new EventBus(), Clock.systemDefaultZone())) {
            assertEquals(EventStatus.COMPLETED, reloaded.event(created.id()).status());
            assertEquals(10, reloaded.confirmedSeats(created.id()));
        }
    }

    @Test
    @Order(2)
    void validationStaysVisibleAndDoesNotSaveBadForm() throws Exception {
        int before = studio.snapshot().events().size();
        openDialog("newEvent", "eventDialog");
        JDialog dialog = visibleDialog("eventDialog");
        click(dialog, "saveEvent");
        assertTrue(edt(() -> find(dialog, "formError", JLabel.class).getText()).contains("Event title is required"));
        assertEquals(before, studio.snapshot().events().size());
        screenshot(dialog, "validation.png");
        fill(dialog, "eventTitle", "Date validation");
        fill(dialog, "eventVenue", "Campus hall");
        fill(dialog, "eventDate", "2026-02-30");
        click(dialog, "saveEvent");
        assertTrue(edt(() -> find(dialog, "formError", JLabel.class).getText()).contains("real date"));
        fill(dialog, "eventDate", LocalDate.now().plusDays(1).toString());
        edt(() -> {
            JSpinner spinner = find(dialog, "eventCapacity", JSpinner.class);
            ((JSpinner.NumberEditor) spinner.getEditor()).getTextField().setText("0");
            return null;
        });
        click(dialog, "saveEvent");
        assertTrue(edt(() -> find(dialog, "formError", JLabel.class).getText()).contains("whole number"));
        assertEquals(before, studio.snapshot().events().size());
        click(dialog, "dismissDialog");
    }

    @Test
    @Order(3)
    void observerRefreshesSelectedQuoteWhenDataChanges() throws Exception {
        Event selected = studio.snapshot().events().get(0);
        assertEquals("93,960.00 KZT", edt(() -> find(frame, "quoteTotal", JLabel.class).getText()));
        studio.saveEvent(selected.toBuilder().pricingMode(PricingMode.STANDARD).build());
        edt(() -> null);
        assertEquals("104,400.00 KZT", edt(() -> find(frame, "quoteTotal", JLabel.class).getText()));
        assertTrue(edt(() -> find(frame, "statusMessage", JLabel.class).getText()).contains("Saved locally"));
        assertEquals(1, edt(() -> find(frame, "activityList", JList.class).getModel().getSize()));
    }

    private void exportThroughChooser(String button, Path target) throws Exception {
        SwingUtilities.invokeLater(() -> find(frame, button, JButton.class).doClick());
        JFileChooser chooser = null;
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (chooser == null && System.nanoTime() < deadline) {
            chooser = edt(() -> {
                for (Window window : Window.getWindows()) {
                    if (window.isShowing()) {
                        JFileChooser found = findOrNull(window, "exportChooser", JFileChooser.class);
                        if (found != null) return found;
                    }
                }
                return null;
            });
            if (chooser == null) Thread.sleep(30);
        }
        assertNotNull(chooser, "Export chooser opened");
        JFileChooser opened = chooser;
        edt(() -> { opened.setSelectedFile(target.toFile()); opened.approveSelection(); return null; });
        edt(() -> null);
        assertTrue(Files.exists(target), "Report was created by the GUI action");
    }

    private void openDialog(String button, String name) throws Exception {
        SwingUtilities.invokeLater(() -> find(frame, button, JButton.class).doClick());
        visibleDialog(name);
    }

    private JDialog visibleDialog(String name) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            JDialog found = edt(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog && name.equals(dialog.getName()) && dialog.isShowing()) return dialog;
                }
                return null;
            });
            if (found != null) return found;
            Thread.sleep(30);
        }
        throw new AssertionError("Dialog did not open: " + name);
    }

    private static void fill(Container root, String name, String value) throws Exception {
        edt(() -> { find(root, name, JTextField.class).setText(value); return null; });
    }

    private static void click(Container root, String name) throws Exception {
        edt(() -> {
            JButton button = find(root, name, JButton.class);
            assertTrue(button.isEnabled(), name + " is enabled");
            button.doClick();
            return null;
        });
    }

    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        T result = findOrNull(root, name, type);
        if (result == null) throw new AssertionError("Missing control: " + name);
        return result;
    }

    private static <T extends Component> T findOrNull(Component root, String name, Class<T> type) {
        if (type.isInstance(root) && name.equals(root.getName())) return type.cast(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findOrNull(child, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { result.set(action.call()); } catch (Throwable exception) { failure.set(exception); }
        });
        if (failure.get() != null) {
            if (failure.get() instanceof Error error) throw error;
            throw new Exception(failure.get());
        }
        return result.get();
    }

    private static void screenshot(Window window, String name) throws Exception {
        Path folder = Path.of(System.getProperty("studio.screenshotDir", "target/ui-evidence"));
        Files.createDirectories(folder);
        BufferedImage image = edt(() -> {
            Container content = ((RootPaneContainer) window).getContentPane();
            BufferedImage captured = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = captured.createGraphics();
            content.printAll(graphics);
            graphics.dispose();
            return captured;
        });
        ImageIO.write(image, "png", folder.resolve(name).toFile());
    }
}
