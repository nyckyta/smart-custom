package edu.ukma.smart.virtual.properties;

import java.util.Objects;

public record BooleanProperty(
    String key,
    String name,
    String description,
    Boolean defaultValue,
    boolean required,
    boolean unique
) implements Property<Boolean> {

    public static BooleanPropertyBuilder builder() {
        return new BooleanPropertyBuilder();
    }

    public static final class BooleanPropertyBuilder {
        private String key;
        private String name;
        private String description;
        private Boolean defaultValue;
        private boolean isRequired;
        private boolean isUnique;

        private BooleanPropertyBuilder() {
        }

        public static BooleanPropertyBuilder aBooleanProperty() {
            return new BooleanPropertyBuilder();
        }

        public BooleanPropertyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public BooleanPropertyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BooleanPropertyBuilder description(String description) {
            this.description = description;
            return this;
        }

        public BooleanPropertyBuilder defaultValue(Boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public BooleanPropertyBuilder required(boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public BooleanPropertyBuilder unique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public BooleanProperty build() {
            return new BooleanProperty(
                Objects.requireNonNull(key),
                Objects.requireNonNull(name),
                description,
                defaultValue,
                isRequired,
                isUnique
            );
        }
    }
}
