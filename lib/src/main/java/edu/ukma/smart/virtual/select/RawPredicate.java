package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.create.Property.SYSTEM_EXCLUDED_FIELDS;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public interface RawPredicate<V> extends Predicate {

    String propertyKey();

    V value();

    @Override
    default Optional<Err> validate() {
        if (propertyKey() == null) {
            return Optional.of(InputValidationErr.error("Property key must be specified"));
        }

        if (!KEY_REGEXP.matcher(propertyKey()).matches()) {
            return Optional.of(InputValidationErr.error("Wrong property key %s".formatted(propertyKey())));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(propertyKey())) {
            return Optional.of(
                InputValidationErr.error("Can not make predicate against forbidden property %s".formatted(propertyKey()))
            );
        }

        if (value() == null) {
            return Optional.of(InputValidationErr.error("Property value must be specified"));
        }

        return Optional.empty();
    }
}
