package kz.aitu.eventstudio.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kz.aitu.eventstudio.domain.StudioState;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class JsonStudioRepository implements StudioRepository {
    private final Path path;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final Gson gson = new GsonBuilder().setStrictness(com.google.gson.Strictness.STRICT).setPrettyPrinting().create();

    public JsonStudioRepository(Path file) throws IOException {
        path = file.toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        lockChannel = FileChannel.open(path.resolveSibling(path.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
        } catch (OverlappingFileLockException exception) {
            acquired = null;
        } catch (IOException exception) {
            lockChannel.close();
            throw exception;
        }
        if (acquired == null) {
            lockChannel.close();
            throw new IOException("This data file is already open in another Event Studio window.");
        }
        lock = acquired;
    }

    @Override
    public Optional<StudioState> load() throws IOException {
        if (!Files.exists(path)) return Optional.empty();
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            StudioState state = gson.fromJson(json, StudioState.class);
            if (state == null) throw new IllegalArgumentException("Empty data file");
            return Optional.of(state);
        } catch (RuntimeException exception) {
            throw new IOException("Cannot read " + path + ". Keep a copy and repair or select a new data file. " + exception.getMessage(), exception);
        }
    }

    @Override
    public void save(StudioState state) throws IOException {
        Path temp = Files.createTempFile(path.getParent(), "studio-", ".tmp");
        try {
            byte[] bytes = gson.toJson(state).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            lockChannel.close();
        }
    }
}
