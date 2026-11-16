package kz.aitu.eventstudio.domain;

public final class ValidationException extends IllegalArgumentException {
    public ValidationException(String message) {
        super(message);
    }
}
