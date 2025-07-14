package edu.ukma.smart.virtual.values;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.create.Property.SYSTEM_EXCLUDED_FIELDS;

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
            return Optional.of(InputValidationErr.error("Value must be specify the key."));
        }

        if (!KEY_REGEXP.matcher(key()).matches()) {
            return Optional.of(InputValidationErr.error("Wrong property key %s".formatted(key())));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(key())) {
            return Optional.of(InputValidationErr.error("Forbidden property %s".formatted(key())));
        }

        return Optional.empty();
    }
}
