package edu.ukma.smart.virtual.create;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record ReferenceProperty(
    String key,
    String name,
    String description,
    boolean required,
    boolean unique,
    Integer defaultValue,
    String refTableKey
) implements Property<Integer> {

    public static ReferencePropertyBuilder builder() {
        return new ReferencePropertyBuilder();
    }

    public static final class ReferencePropertyBuilder {
        private String refTableKey;
        private boolean unique;
        private boolean required;
        private String key;
        private String description;
        private String name;
        private Integer defaultValue;

        private ReferencePropertyBuilder() {
        }

        public static ReferencePropertyBuilder referencePropertyBuilder() {
            return new ReferencePropertyBuilder();
        }

        public ReferencePropertyBuilder refTableKey(String refTableKey) {
            this.refTableKey = refTableKey;
            return this;
        }

        public ReferencePropertyBuilder unique(boolean isUnique) {
            this.unique = isUnique;
            return this;
        }

        public ReferencePropertyBuilder required(boolean isRequired) {
            this.required = isRequired;
            return this;
        }

        public ReferencePropertyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public ReferencePropertyBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ReferencePropertyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ReferencePropertyBuilder defaultValue(Integer defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public ReferenceProperty build() {
            return new ReferenceProperty(
                key,
                name,
                description,
                required,
                unique,
                defaultValue,
                refTableKey
            );
        }
    }

    @Override
    public Optional<Err> validate() {
        if (refTableKey == null) {
            return Optional.of(InputValidationErr.error("Reference table must be specified"));
        }

        if (!KEY_REGEXP.matcher(refTableKey).matches()) {
            return Optional.of(InputValidationErr.error("Invalid table reference %s".formatted(refTableKey)));
        }

        return Property.super.validate();
    }
}
