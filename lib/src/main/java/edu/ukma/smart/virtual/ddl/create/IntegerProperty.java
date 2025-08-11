package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.List;

public final class IntegerProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final Long defaultValue;
    private final boolean notNull;
    private final List<Constraint> constraints;
    private final Type type;

    private IntegerProperty(
        String key,
        String name,
        String description,
        Long defaultValue,
        boolean notNull,
        List<Constraint> constraints,
        Type type
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.constraints = constraints == null ? List.of() : Collections.unmodifiableList(constraints);
        this.type = type;
    }

    public IntegerProperty(
        String key,
        String name,
        String description,
        Long defaultValue,
        boolean notNull,
        List<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.constraints = constraints == null ? List.of() : Collections.unmodifiableList(constraints);
        this.type = Type.INTEGER;
    }

    public static IntegerPropertyBuilder builder() {
        return new IntegerPropertyBuilder();
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

    public Long defaultValue() {
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
        return "IntegerProperty[" +
               "key=" + key + ", " +
               "name=" + name + ", " +
               "description=" + description + ", " +
               "defaultValue=" + defaultValue + ", " +
               "notNull=" + notNull + ", " +
               "notNull=" + constraints + ", " +
               "type=" + type + ']';
    }


    public static final class IntegerPropertyBuilder {
        private String key;
        private String name;
        private String description;
        private Long defaultValue;
        private boolean notNull;
        private List<Constraint> constraints;

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
            this.notNull = isRequired;
            return this;
        }

        public IntegerPropertyBuilder constraints(List<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public IntegerProperty build() {
            return new IntegerProperty(
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
