package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.List;

public final class BooleanProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final Boolean defaultValue;
    private final boolean notNull;
    private final List<Constraint> constraints;
    private final Type type;


    private BooleanProperty(
        String key,
        String name,
        String description,
        Boolean defaultValue,
        boolean notNull,
        List<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.constraints = constraints == null ? List.of() : Collections.unmodifiableList(constraints);
        this.type = Type.BOOLEAN;
    }

    public static BooleanPropertyBuilder builder() {
        return new BooleanPropertyBuilder();
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

    @Override
    public boolean notNull() {
        return notNull;
    }

    public Boolean defaultValue() {
        return defaultValue;
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
        return "BooleanProperty[" +
               "key=" + key + ", " +
               "name=" + name + ", " +
               "description=" + description + ", " +
               "defaultValue=" + defaultValue + ", " +
               "notNull=" + notNull + ", " +
               "notNull=" + constraints + ", " +
               "type=" + type + ']';
    }


    public static final class BooleanPropertyBuilder {
        private String key;
        private String name;
        private String description;
        private Boolean defaultValue;
        private boolean isRequired;
        private List<Constraint> constraints;

        private BooleanPropertyBuilder() {
        }

        public static BooleanPropertyBuilder builder() {
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

        public BooleanPropertyBuilder constraints(List<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public BooleanProperty build() {
            return new BooleanProperty(
                key,
                name,
                description,
                defaultValue,
                isRequired,
                constraints
            );
        }
    }
}
