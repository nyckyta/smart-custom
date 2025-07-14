package edu.ukma.smart.virtual.create;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// TODO: strict validation
public record NewTable(
    String key,
    String name,
    String description,
    List<Property<?>> properties
) implements Validated {

    public NewTable(String key, String name, String description, List<Property<?>> properties) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.properties = Collections.unmodifiableList(properties);
    }

    public static NewTableBuilder builder() {
        return new NewTableBuilder();
    }

    public static final class NewTableBuilder {
        private String key;
        private String name;
        private String description;
        private List<Property<?>> properties;

        private NewTableBuilder() {
        }

        public NewTableBuilder key(String key) {
            this.key = key;
            return this;
        }

        public NewTableBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NewTableBuilder description(String description) {
            this.description = description;
            return this;
        }

        public NewTableBuilder properties(List<Property<?>> properties) {
            this.properties = properties;
            return this;
        }

        public NewTable build() {
            return new NewTable(
                key,
                name,
                description,
                properties
            );
        }
    }

    @Override
    public Optional<Err> validate() {
        if (key == null) {
            return Optional.of(InputValidationErr.error("Table key must be specified"));
        }

        if (!KEY_REGEXP.matcher(key).matches()) {
            return Optional.of(InputValidationErr.error("Invalid table key format: %s".formatted(key)));
        }

        if (name == null || name.isBlank()) {
            return Optional.of(InputValidationErr.error("Table name must be specified"));
        }

        if (properties == null) {
            return Optional.of(InputValidationErr.error("Table properties must be specified"));
        }

        return Optional.empty();
    }
}
