package edu.ukma.smart.virtual.properties;

import java.util.Objects;

// TODO: strict validation
public record StringProperty(
    String key,
    String name,
    String description,
    String defaultValue,
    boolean isRequired,
    boolean isUnique,
    Integer maxLength,
    Integer minLength

) implements Property {

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

        public PropertyBuilder isRequired(boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public PropertyBuilder isUnique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public PropertyBuilder key(String key) {
            this.key = key;
            return this;
        }


        public PropertyBuilder maxLength(int maxLength) {
            if (maxLength < 0 && (minLength == null || maxLength > minLength)) {
                throw new IllegalArgumentException("maxLength must be >= 0");
            }
            this.maxLength = maxLength;
            return this;
        }

        public PropertyBuilder minLength(int minLength) {
            if (minLength < 0 && (maxLength == null || minLength <= maxLength)) {
                throw new IllegalArgumentException("minLength must be >= 0");
            }
            this.minLength = minLength;
            return this;
        }

        public StringProperty build() {
            return new StringProperty(
                Objects.requireNonNull(key),
                Objects.requireNonNull(name),
                Objects.requireNonNull(description),
                defaultValue,
                isRequired,
                isUnique,
                maxLength,
                minLength
            );
        }
    }
}
