package org.superwindcloud.fkill;

import java.util.List;

public class FkillException extends Exception {
    private final List<String> errors;

    public FkillException(String message) {
        super(message);
        this.errors = List.of();
    }

    public FkillException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of();
    }

    public FkillException(String message, List<String> errors) {
        super(message + System.lineSeparator() + String.join(System.lineSeparator(), errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
