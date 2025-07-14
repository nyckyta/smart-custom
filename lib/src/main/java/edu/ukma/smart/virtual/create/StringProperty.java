package edu.ukma.smart.virtual.create;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record StringProperty(
    String key,
    String name,
    String description,
    String defaultValue,
    boolean required,
    boolean unique,
    Integer maxLength,
    Integer minLength
) implements Property<String> {

    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    public static final class PropertyBuilder {
        private String key;
        private String name;
        private String description;
        private String defaultValue;
        private boolean isRequired;
        private boolean isUnique;
        private Integer maxLength;
        private Integer minLength;

        private PropertyBuilder() {
        }

        public PropertyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PropertyBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PropertyBuilder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public PropertyBuilder required(boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public PropertyBuilder unique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public PropertyBuilder key(String key) {
            this.key = key;
            return this;
        }


        public PropertyBuilder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public PropertyBuilder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public StringProperty build() {
            return new StringProperty(
                key,
                name,
                description,
                defaultValue,
                isRequired,
                isUnique,
                maxLength,
                minLength
            );
        }
    }

    @Override
    public Optional<Err> validate() {
        boolean maxLengthSet = maxLength != null;
        boolean minLengthSet = minLength != null;

        if (maxLengthSet && maxLength < 1) {
            return Optional.of(InputValidationErr.error(
                "Property key '%s' max length can not be less than one".formatted(key()))
            );
        }

        if (minLengthSet && minLength < 1) {
            return Optional.of(
                InputValidationErr.error(
                    "Property key '%s' min length can not be less than one".formatted(key()))
            );
        }

        if (minLengthSet && maxLengthSet) {
            if (maxLength() < minLength()) {
                return Optional.of(
                    InputValidationErr.error("Property key '%s' max length can not be less than min length".formatted(key()))
                );
            }
        }

        return Property.super.validate();
    }
}
