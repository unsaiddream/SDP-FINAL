package kz.aitu.eventstudio.storage;

import kz.aitu.eventstudio.domain.StudioState;
import java.io.IOException;
import java.util.Optional;

public interface StudioRepository extends AutoCloseable {
    Optional<StudioState> load() throws IOException;
    void save(StudioState state) throws IOException;
    @Override
    void close() throws IOException;
}
