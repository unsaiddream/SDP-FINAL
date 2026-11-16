package kz.aitu.eventstudio;

import kz.aitu.eventstudio.domain.*;
import kz.aitu.eventstudio.events.EventBus;
import kz.aitu.eventstudio.service.*;
import kz.aitu.eventstudio.storage.JsonStudioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JsonStudioRepositoryTest {
    @TempDir Path directory;

    @Test
    void jsonRoundTripPreservesUnicodeAndAllRelationships() throws Exception {
        Path path = directory.resolve("nested/studio.json");
        StudioState saved;
        try (StudioFacade studio = new StudioFacade(new JsonStudioRepository(path), new EventBus(), Clock.systemUTC())) {
            studio.saveParticipant(null, "Санжар Қараулов", "sanzhar@example.com");
            saved = studio.snapshot();
        }
        try (JsonStudioRepository repository = new JsonStudioRepository(path)) {
            assertEquals(saved, repository.load().orElseThrow());
        }
    }

    @Test
    void corruptDataIsReportedAndNeverOverwrittenWithSamples() throws Exception {
        Path path = directory.resolve("broken.json");
        String corrupt = "{\"version\":1,\"events\":[";
        Files.writeString(path, corrupt);
        try (JsonStudioRepository repository = new JsonStudioRepository(path)) {
            assertThrows(IOException.class, () -> new StudioFacade(repository, new EventBus(), Clock.systemUTC()));
        }
        assertEquals(corrupt, Files.readString(path));
    }

    @Test
    void emptyNullAndUnsupportedVersionsAreRejected() throws Exception {
        for (String json : List.of("", "null", "{}", "{\"version\":2,\"events\":[],\"participants\":[],\"bookings\":[]}")) {
            Path path = directory.resolve(UUID.randomUUID() + ".json");
            Files.writeString(path, json);
            try (JsonStudioRepository repository = new JsonStudioRepository(path)) {
                assertThrows(IOException.class, repository::load);
            }
            assertEquals(json, Files.readString(path));
        }
    }

    @Test
    void secondWriterIsRejectedUntilFirstWindowCloses() throws Exception {
        Path path = directory.resolve("studio.json");
        try (JsonStudioRepository first = new JsonStudioRepository(path)) {
            assertThrows(IOException.class, () -> new JsonStudioRepository(path));
            first.save(SampleData.create(Clock.systemUTC()));
        }
        try (JsonStudioRepository second = new JsonStudioRepository(path)) {
            assertTrue(second.load().isPresent());
        }
    }

    @Test
    void existingEmptyWorkspaceDoesNotReseedAndTemporaryFilesAreCleaned() throws Exception {
        Path path = directory.resolve("studio.json");
        try (JsonStudioRepository repository = new JsonStudioRepository(path)) {
            repository.save(new StudioState(1, List.of(), List.of(), List.of()));
        }
        try (StudioFacade studio = new StudioFacade(new JsonStudioRepository(path), new EventBus(), Clock.systemUTC())) {
            assertTrue(studio.snapshot().events().isEmpty());
        }
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(p -> p.toString().endsWith(".tmp")));
        }
    }

    @Test
    void orphanedAndOverbookedPersistedDataAreRejected() throws Exception {
        Path path = directory.resolve("studio.json");
        try (JsonStudioRepository repository = new JsonStudioRepository(path)) {
            repository.save(SampleData.create(Clock.systemUTC()));
        }
        String original = Files.readString(path);
        String orphan = original.replace("10000000-0000-0000-0000-000000000001", "99999999-0000-0000-0000-000000000001");
        orphan = orphan.replaceFirst("99999999-0000-0000-0000-000000000001", "10000000-0000-0000-0000-000000000001");
        Files.writeString(path, orphan);
        try (JsonStudioRepository repository = new JsonStudioRepository(path)) { assertThrows(IOException.class, repository::load); }
        Files.writeString(path, original.replace("\"capacity\": 30", "\"capacity\": 1"));
        try (JsonStudioRepository repository = new JsonStudioRepository(path)) { assertThrows(IOException.class, repository::load); }
    }
}
