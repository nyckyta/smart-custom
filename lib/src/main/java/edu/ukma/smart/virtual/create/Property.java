package edu.ukma.smart.virtual.create;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public interface Property<T> extends Validated {

    String key();

    String name();

    String description();

    boolean unique();

    boolean required();

    T defaultValue();

    @Override
    default Optional<InputValidationErr> validate() {
        if (key() == null) {
            return Optional.of(InputValidationErr.of(EMPTY_PROPERTY_KEY));
        }

        if (name() == null || name().isBlank()) {
            return Optional.of(InputValidationErr.of(CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY));
        }

        return Optional.empty();
    }
}
