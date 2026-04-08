package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class IntegerProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final Long defaultValue;
    private final boolean notNull;
    private final Set<Constraint> constraints;
    private final Type type;

    private IntegerProperty(
        String key,
        String name,
        String description,
        Long defaultValue,
        boolean notNull,
        Set<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.constraints = constraints == null ? Set.of() : Collections.unmodifiableSet(constraints);
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
    public Set<Constraint> constraints() {
        return constraints;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return "IntegerProperty[key=%s, name=%s, description=%s, defaultValue=%d, notNull=%s, constraints=%s, type=%s]".formatted(key, name,
            description, defaultValue, notNull, constraints, type);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntegerProperty that)) {
            return false;
        }
        return notNull == that.notNull && Objects.equals(key, that.key) && Objects.equals(name, that.name)
               && Objects.equals(description, that.description) && Objects.equals(defaultValue, that.defaultValue)
               && type == that.type && constraints.equals(that.constraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, defaultValue, notNull, constraints, type);
    }

    public static final class IntegerPropertyBuilder implements Builder<IntegerProperty> {
        private String key;
        private String name;
        private String description;
        private Long defaultValue;
        private boolean notNull;
        private Set<Constraint> constraints;

        private IntegerPropertyBuilder() {
            this.constraints = new HashSet<>();
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

        public IntegerPropertyBuilder notNull(boolean notNull) {
            this.notNull = notNull;
            return this;
        }

        public IntegerPropertyBuilder constraints(Set<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        @Override
        public Builder<IntegerProperty> addConstraint(Constraint constraint) {
            this.constraints.add(constraint);
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
