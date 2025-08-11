package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.List;

public final class StringProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final String defaultValue;
    private final boolean notNull;
    private final List<Constraint> constraints;
    private final Type type;


    public StringProperty(
        String key,
        String name,
        String description,
        String defaultValue,
        boolean required,
        List<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = required;
        this.constraints = constraints == null ? List.of() : Collections.unmodifiableList(constraints);
        this.type = Type.STRING;
    }

    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean notNull() {
        return notNull;
    }

    @Override
    public List<Constraint> constraints() {
        return constraints;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return "StringProperty[" +
               "key=" + key + ", " +
               "name=" + name + ", " +
               "description=" + description + ", " +
               "defaultValue=" + defaultValue + ", " +
               "notNull=" + notNull + ", " +
               "constraints=" + constraints + ", " +
               "type=" + type + ']';
    }


    public static final class PropertyBuilder {
        private String key;
        private String name;
        private String description;
        private String defaultValue;
        private boolean notNull;
        private List<Constraint> constraints;

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
            this.notNull = isRequired;
            return this;
        }


        public PropertyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public PropertyBuilder constraints(List<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public StringProperty build() {
            return new StringProperty(
                key,
                name,
                description,
                defaultValue,
                notNull,
                constraints
            );
        }
    }
}
