package io.github.ashhdroid.dispatch.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum Priority {
    HIGH, MEDIUM, LOW;

    @JsonCreator
    public static Priority parse(String input) {
        if (input == null) return null;
        try {
            return Priority.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("priority must be one of HIGH, MEDIUM, LOW");
        }
    }
}
