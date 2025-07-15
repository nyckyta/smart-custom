package edu.ukma.smart.virtual.errors;

import java.util.Optional;

public record Return<T>(T value, Optional<Err> error) {

    public static <T> Return<T> of(T value) {
        if (value == null) {
            throw new NullPointerException("value is null");
        }

        return new Return<>(value, Optional.empty());
    }

    public static <T> Return<T> error(Err err) {
        if (err == null) {
            throw new NullPointerException("Error must not be null");
        }
        return new Return<>(null, Optional.of(err));
    }
}
