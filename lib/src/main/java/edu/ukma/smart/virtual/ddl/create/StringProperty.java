package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class StringProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final String defaultValue;
    private final boolean notNull;
    private final Set<Constraint> constraints;
    private final Type type;


    public StringProperty(
        String key,
        String name,
        String description,
        String defaultValue,
        boolean required,
        Set<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = required;
        this.constraints = constraints == null ? Set.of() : Collections.unmodifiableSet(constraints);
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
    public Set<Constraint> constraints() {
        return constraints;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StringProperty that)) {
            return false;
        }
        return notNull == that.notNull && Objects.equals(key, that.key) && Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) && Objects.equals(defaultValue, that.defaultValue) &&
               Objects.equals(constraints, that.constraints) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, defaultValue, notNull, constraints, type);
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


    public static final class PropertyBuilder implements Builder<StringProperty> {
        private String key;
        private String name;
        private String description;
        private String defaultValue;
        private boolean notNull;
        private Set<Constraint> constraints;

        private PropertyBuilder() {
            constraints = new HashSet<>();
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

        public PropertyBuilder notNull(boolean notNull) {
            this.notNull = notNull;
            return this;
        }


        public PropertyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public PropertyBuilder constraints(Set<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        @Override
        public Builder<StringProperty> addConstraint(Constraint constraint) {
            this.constraints.add(constraint);
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
