package kz.aitu.eventstudio;

import kz.aitu.eventstudio.events.EventBus;
import kz.aitu.eventstudio.service.StudioFacade;
import kz.aitu.eventstudio.storage.JsonStudioRepository;
import kz.aitu.eventstudio.ui.StudioFrame;
import javax.swing.*;
import kz.aitu.eventstudio.ui.UiTheme;
import java.nio.file.Path;
import java.time.Clock;

public final class App {
    private App() {}

    public static void main(String[] args) {
        if (args.length > 1 || args.length == 1 && args[0].equals("--help")) {
            System.out.println("Usage: java -jar target/event-studio.jar [path/to/studio.json]");
            return;
        }
        Path data = args.length == 1 ? Path.of(args[0]) : Path.of("data", "studio.json");
        SwingUtilities.invokeLater(() -> {
            JsonStudioRepository repository = null;
            try {
                UiTheme.install();
                EventBus bus = new EventBus();
                repository = new JsonStudioRepository(data);
                StudioFacade studio = new StudioFacade(repository, bus, Clock.systemDefaultZone());
                StudioFrame frame = new StudioFrame(studio, bus);
                frame.setVisible(true);
            } catch (Exception exception) {
                if (repository != null) {
                    try { repository.close(); } catch (Exception ignored) {}
                }
                String message = "Event Studio could not open this workspace.\n" + exception.getMessage();
                System.err.println(message);
                JOptionPane.showMessageDialog(null, message, "Workspace error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
