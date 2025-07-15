package edu.ukma.smart.virtual.create;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.FORBIDDEN_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public interface Property<T> extends Validated {

    Set<String> STATIC_FIELDS = Set.of("_id", "_created");
    Set<String> SYSTEM_EXCLUDED_FIELDS = Set.of(
        "tableoid",
        "xmin",
        "cmin",
        "xmax",
        "cmax",
        "ctid"
    );
    Pattern KEY_REGEXP = Pattern.compile("^[a-z][a-z_]{1,62}$");

    String key();

    String name();

    String description();

    boolean unique();

    boolean required();

    T defaultValue();

    @Override
    default Optional<Err> validate() {
        if (key() == null) {
            return Optional.of(InputValidationErr.error(EMPTY_PROPERTY_KEY));
        }

        if (name() == null || name().isBlank()) {
            return Optional.of(InputValidationErr.error(CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY));
        }

        if (!KEY_REGEXP.matcher(key()).matches()) {
            return Optional.of(InputValidationErr.error(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (STATIC_FIELDS.contains(key()) || SYSTEM_EXCLUDED_FIELDS.contains(key())) {
            return Optional.of(InputValidationErr.error(FORBIDDEN_PROPERTY_KEY));
        }

        return Optional.empty();
    }
}
