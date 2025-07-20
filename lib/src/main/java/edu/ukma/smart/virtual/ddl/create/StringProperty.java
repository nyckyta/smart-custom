package edu.ukma.smart.virtual.ddl.create;

public record StringProperty(
    String key,
    String name,
    String description,
    String defaultValue,
    boolean required,
    boolean unique,
    Integer maxLength,
    Integer minLength,
    Type type
) implements Property {

    public StringProperty(
        String key,
        String name,
        String description,
        String defaultValue,
        boolean required,
        boolean unique,
        Integer maxLength,
        Integer minLength,
        Type type
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.required = required;
        this.unique = unique;
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.type = Type.STRING;
    }

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
                minLength,
                Type.STRING
            );
        }
    }
}
