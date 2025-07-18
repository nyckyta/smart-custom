package edu.ukma.smart.virtual.create;

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

        public static ReferencePropertyBuilder builder() {
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
}
