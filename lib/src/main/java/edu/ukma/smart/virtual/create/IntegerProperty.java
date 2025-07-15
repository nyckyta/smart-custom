package edu.ukma.smart.virtual.create;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Objects;
import java.util.Optional;

public record IntegerProperty(
    String key,
    String name,
    String description,
    Long defaultValue,
    boolean required,
    boolean unique,
    Long max,
    Long min
) implements Property<Long> {

    public static IntegerPropertyBuilder builder() {
        return new IntegerPropertyBuilder();
    }

    public static final class IntegerPropertyBuilder {
        private String key;
        private String name;
        private String description;
        private Long defaultValue;
        private boolean required;
        private boolean unique;
        private Long max;
        private Long min;

        private IntegerPropertyBuilder() {
        }

        public static IntegerPropertyBuilder builder() {
            return new IntegerPropertyBuilder();
        }

        public IntegerPropertyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public IntegerPropertyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public IntegerPropertyBuilder description(String description) {
            this.description = description;
            return this;
        }

        public IntegerPropertyBuilder defaultValue(Long defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public IntegerPropertyBuilder required(boolean isRequired) {
            this.required = isRequired;
            return this;
        }

        public IntegerPropertyBuilder unique(boolean isUnique) {
            this.unique = isUnique;
            return this;
        }

        public IntegerPropertyBuilder max(Long max) {
            this.max = max;
            return this;
        }

        public IntegerPropertyBuilder min(Long min) {
            this.min = min;
            return this;
        }

        public IntegerProperty build() {
            return new IntegerProperty(
                Objects.requireNonNull(key),
                Objects.requireNonNull(name),
                description,
                defaultValue,
                required,
                unique,
                max,
                min);
        }
    }

    @Override
    public Optional<Err> validate() {
        boolean isMaxSet = max != null;
        boolean isMinSet = min != null;
        boolean defaultValueSet = defaultValue != null;

        if (defaultValueSet) {
            if (isMinSet && defaultValue < min) {
                return Optional.of(
                    InputValidationErr.error("Property key '%s' default value can not be less that minimum".formatted(key))
                );
            }

            if (isMaxSet && defaultValue > max) {
                return Optional.of(
                    InputValidationErr.error("Property key '%s' default value can not be less than maximum".formatted(key))
                );
            }
        }


        if (isMinSet && isMaxSet && max < min) {
            return Optional.of(
                InputValidationErr.error("Property key '%s' max can not be less than min".formatted(key))
            );
        }

        return Property.super.validate();
    }
}
