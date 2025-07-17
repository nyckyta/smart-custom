package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record SelectProperty(String propertyKey) implements Validated {

    public static SelectProperty of(String propertyName) {
        return new SelectProperty(propertyName);
    }

    @Override
    public Optional<InputValidationErr> validate() {
        if (propertyKey == null) {
            return Optional.of(InputValidationErr.of(EMPTY_PROPERTY_KEY));
        }

        return Optional.empty();
    }
}
