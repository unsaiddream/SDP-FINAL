package kz.aitu.eventstudio.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class ExportCreator {
    protected abstract ReportExporter createExporter();
    public abstract String extension();

    public final Path export(EventReport report, Path destination) throws IOException {
        Path file = destination.toAbsolutePath().normalize();
        if (!file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith("." + extension())) {
            throw new IllegalArgumentException("Choose a ." + extension() + " file.");
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "report-", ".tmp");
        try {
            Files.writeString(temporary, createExporter().render(report), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return file;
    }
}
