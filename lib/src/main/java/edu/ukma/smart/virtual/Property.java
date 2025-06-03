package edu.ukma.smart.virtual;

import java.util.Objects;

// TODO: strict validation
public record Property(
    String name,
    String description,
    Type type,
    String defaultValue,
    boolean isRequired,
    boolean isUnique
) {

    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    public enum Type {
        STRING("VARCHAR");

        public final String sqlType;

        Type(String sqlType) {
            this.sqlType = sqlType;
        }
    }

    public static final class PropertyBuilder {
        private String name;
        private String description;
        private Type type;
        private String defaultValue;
        private boolean isRequired;
        private boolean isUnique;

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

        public PropertyBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public PropertyBuilder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public PropertyBuilder isRequired(boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public PropertyBuilder isUnique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public Property build() {
            return new Property(
                Objects.requireNonNull(name),
                Objects.requireNonNull(description),
                Objects.requireNonNull(type),
                defaultValue,
                isRequired,
                isUnique
            );
        }
    }
}
