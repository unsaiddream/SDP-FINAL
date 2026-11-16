package kz.aitu.eventstudio.domain;

import java.util.Locale;

public record Participant(String id, String name, String email) {
    public Participant {
        id = Checks.id(id);
        name = Checks.text(name, "Participant name", 100);
        email = Checks.text(email, "Email", 160).toLowerCase(Locale.ROOT);
        if (!email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+")) {
            throw new ValidationException("Enter a valid email address, such as name@example.com.");
        }
    }

    @Override
    public String toString() { return name + " <" + email + ">"; }
}
