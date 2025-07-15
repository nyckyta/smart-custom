package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.create.Property.STATIC_FIELDS;
import static edu.ukma.smart.virtual.create.Property.SYSTEM_EXCLUDED_FIELDS;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.FORBIDDEN_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record SelectProperty(String propertyKey) implements Validated {

    public static SelectProperty of(String propertyName) {
        return new SelectProperty(propertyName);
    }

    @Override
    public Optional<Err> validate() {
        if (propertyKey == null) {
            return Optional.of(InputValidationErr.error(EMPTY_PROPERTY_KEY));
        }

        if (!STATIC_FIELDS.contains(propertyKey) && !KEY_REGEXP.matcher(propertyKey).matches()) {
            return Optional.of(InputValidationErr.error(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(propertyKey)) {
            return Optional.of(InputValidationErr.error(FORBIDDEN_PROPERTY_KEY));
        }

        return Optional.empty();
    }
}
