package edu.ukma.smart.virtual.create;

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
            return Optional.of(InputValidationErr.error("Key must be specified"));
        }

        if (name() == null) {
            return Optional.of(InputValidationErr.error("Name must be specified"));
        }

        if (!KEY_REGEXP.matcher(key()).matches()) {
            return Optional.of(InputValidationErr.error("Wrong table key %s".formatted(key())));
        }

        if (STATIC_FIELDS.contains(key()) || SYSTEM_EXCLUDED_FIELDS.contains(key())) {
            return Optional.of(InputValidationErr.error("Property key %s is forbidden".formatted(key())));
        }

        return Optional.empty();
    }
}
