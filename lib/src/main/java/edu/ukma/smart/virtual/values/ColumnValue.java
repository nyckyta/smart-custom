package edu.ukma.smart.virtual.values;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.create.Property.SYSTEM_EXCLUDED_FIELDS;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.FORBIDDEN_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public interface ColumnValue<T> extends Validated {
    String key();

    T value();

    @Override
    default Optional<Err> validate() {
        if (key() == null) {
            return Optional.of(InputValidationErr.error(EMPTY_PROPERTY_KEY));
        }

        if (!KEY_REGEXP.matcher(key()).matches()) {
            return Optional.of(InputValidationErr.error(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(key())) {
            return Optional.of(InputValidationErr.error(FORBIDDEN_PROPERTY_KEY));
        }

        return Optional.empty();
    }
}
