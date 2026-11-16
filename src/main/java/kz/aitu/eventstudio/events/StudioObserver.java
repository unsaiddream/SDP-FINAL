package kz.aitu.eventstudio.events;

@FunctionalInterface
public interface StudioObserver {
    void onChange(StudioEvent event);
}
