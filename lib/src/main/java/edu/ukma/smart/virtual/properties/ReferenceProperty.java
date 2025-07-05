package edu.ukma.smart.virtual.properties;

import java.util.Objects;

public record ReferenceProperty(
    String key,
    String name,
    String description,
    boolean required,
    boolean unique,
    Void defaultValue,
    String refTableKey
) implements Property<Void> {

    public static ReferencePropertyBuilder builder() {
        return new ReferencePropertyBuilder();
    }

    @Override
    public Void defaultValue() {
        throw new UnsupportedOperationException(
            "Reference properties do not support default value");
    }

    public static final class ReferencePropertyBuilder {
        private String refTableKey;
        private boolean unique;
        private boolean required;
        private String key;
        private String description;
        private String name;

        private ReferencePropertyBuilder() {
        }

        public static ReferencePropertyBuilder aReferenceProperty() {
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

        public ReferenceProperty build() {
            return new ReferenceProperty(
                Objects.requireNonNull(key),
                Objects.requireNonNull(name),
                description,
                required,
                unique,
                null,
                Objects.requireNonNull(refTableKey)
            );
        }
    }
}
