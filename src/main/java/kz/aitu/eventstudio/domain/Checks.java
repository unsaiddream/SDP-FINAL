package kz.aitu.eventstudio.domain;

import java.util.UUID;

public final class Checks {
    private Checks() {}

    public static String text(String value, String field, int maximum) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " is required.");
        String clean = value.strip();
        if (clean.length() > maximum) throw new ValidationException(field + " must have at most " + maximum + " characters.");
        if (clean.chars().anyMatch(Character::isISOControl)) throw new ValidationException(field + " must not contain control characters.");
        return clean;
    }

    public static String id(String value) {
        try {
            if (value == null || !UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException();
            return value;
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Invalid record identifier.");
        }
    }

    public static int range(int value, String field, int min, int max) {
        if (value < min || value > max) throw new ValidationException(field + " must be between " + min + " and " + max + ".");
        return value;
    }

    public static <T> T required(T value, String field) {
        if (value == null) throw new ValidationException(field + " is required.");
        return value;
    }
}
