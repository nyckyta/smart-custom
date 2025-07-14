package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.create.Property.STATIC_FIELDS;
import static edu.ukma.smart.virtual.create.Property.SYSTEM_EXCLUDED_FIELDS;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record SelectProperty(String propertyName) implements Validated {

    public static SelectProperty of(String propertyName) {
        return new SelectProperty(propertyName);
    }

    @Override
    public Optional<Err> validate() {
        if (propertyName == null) {
            return Optional.of(InputValidationErr.error("Property name must be specified"));
        }

        if (!STATIC_FIELDS.contains(propertyName) && !KEY_REGEXP.matcher(propertyName).matches()) {
            return Optional.of(InputValidationErr.error("Property name has invalid format %s".formatted(propertyName)));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(propertyName)) {
            return Optional.of(InputValidationErr.error("Forbidden property %s".formatted(propertyName)));
        }

        return Optional.empty();
    }
}
