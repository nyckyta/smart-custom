package edu.ukma.smart.virtual.values;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public interface ColumnValue<T> extends Validated {
    String key();

    T value();

    @Override
    default Optional<InputValidationErr> validate() {
        if (key() == null) {
            return Optional.of(InputValidationErr.of(EMPTY_PROPERTY_KEY));
        }

        return Optional.empty();
    }
}
