package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class BooleanProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final Boolean defaultValue;
    private final boolean notNull;
    private final Set<Constraint> constraints;
    private final Type type;


    private BooleanProperty(
        String key,
        String name,
        String description,
        Boolean defaultValue,
        boolean notNull,
        Set<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.constraints = constraints == null ? Set.of() : Collections.unmodifiableSet(constraints);
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
    public Set<Constraint> constraints() {
        return constraints;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return "BooleanProperty[key=%s, name=%s, description=%s, defaultValue=%s, notNull=%s, notNull=%s, type=%s]".formatted(key, name,
            description, defaultValue, notNull, constraints, type);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BooleanProperty that)) {
            return false;
        }
        return notNull == that.notNull && Objects.equals(key, that.key) && Objects.equals(name, that.name)
               && Objects.equals(description, that.description) && Objects.equals(defaultValue, that.defaultValue)
               && Objects.equals(constraints, that.constraints) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, defaultValue, notNull, constraints, type);
    }

    public static final class BooleanPropertyBuilder implements Builder<BooleanProperty> {
        private String key;
        private String name;
        private String description;
        private Boolean defaultValue;
        private boolean notNull;
        private Set<Constraint> constraints;

        private BooleanPropertyBuilder() {
            constraints = new HashSet<>();
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

        public BooleanPropertyBuilder notNull(boolean isRequired) {
            this.notNull = isRequired;
            return this;
        }

        public BooleanPropertyBuilder constraints(Set<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        @Override
        public Builder<BooleanProperty> addConstraint(Constraint constraint) {
            this.constraints.add(constraint);
            return this;
        }


        public BooleanProperty build() {
            return new BooleanProperty(
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
