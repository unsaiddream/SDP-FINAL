package kz.aitu.eventstudio.events;

import java.time.Instant;

public record StudioEvent(Instant occurredAt, String message) {}
