package edu.ukma.smart.virtual.properties;

public record ReferenceProperty(
    String key,
    String name,
    String description,
    boolean isRequired,
    boolean isUnique,
    Void defaultValue,
    String refTableKey
) implements Property<Void> {

    @Override
    public Void defaultValue() {
        throw new UnsupportedOperationException("Reference properties do not support default value");
    }

    public static ReferencePropertyBuilder builder() {
        return new ReferencePropertyBuilder();
    }

    public static final class ReferencePropertyBuilder {
        private String refTableKey;
        private boolean isUnique;
        private boolean isRequired;
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

        public ReferencePropertyBuilder isUnique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public ReferencePropertyBuilder isRequired(boolean isRequired) {
            this.isRequired = isRequired;
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
            return new ReferenceProperty(key, name, description, isRequired, isUnique, null, refTableKey);
        }
    }
}
