package kz.aitu.eventstudio.events;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {
    private final CopyOnWriteArrayList<StudioObserver> observers = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(StudioObserver observer) {
        observers.add(Objects.requireNonNull(observer));
        return () -> observers.remove(observer);
    }

    public void publish(StudioEvent event) {
        for (StudioObserver observer : observers) {
            try {
                observer.onChange(event);
            } catch (RuntimeException exception) {
                System.err.println("A subscriber could not refresh: " + exception.getMessage());
            }
        }
    }
}
