package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ReferenceProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final boolean notNull;
    private final Set<Constraint> constraints;
    private final Integer defaultValue;
    private final String refTableKey;
    private final Type type;


    private ReferenceProperty(
        String key,
        String name,
        String description,
        boolean notNull,
        Set<Constraint> constraints,
        Integer defaultValue,
        String refTableKey
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.constraints = constraints == null ? Set.of() : Collections.unmodifiableSet(constraints);
        this.defaultValue = defaultValue;
        this.refTableKey = refTableKey;
        this.notNull = notNull;
        this.type = Type.REFERENCE;
    }

    public static ReferencePropertyBuilder builder() {
        return new ReferencePropertyBuilder();
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

    @Override
    public Set<Constraint> constraints() {
        return constraints;
    }

    public Integer defaultValue() {
        return defaultValue;
    }

    public String refTableKey() {
        return refTableKey;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, notNull, constraints, defaultValue, refTableKey, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ReferenceProperty) obj;
        return Objects.equals(this.key, that.key) &&
               Objects.equals(this.name, that.name) &&
               Objects.equals(this.description, that.description) &&
               this.notNull == that.notNull &&
               Objects.equals(this.constraints, that.constraints) &&
               Objects.equals(this.defaultValue, that.defaultValue) &&
               Objects.equals(this.refTableKey, that.refTableKey) &&
               Objects.equals(this.type, that.type);
    }


    public static final class ReferencePropertyBuilder implements Builder<ReferenceProperty> {
        private String refTableKey;
        private boolean notNull;
        private String key;
        private String description;
        private String name;
        private Integer defaultValue;
        private Set<Constraint> constraints;

        private ReferencePropertyBuilder() {
            this.constraints = new HashSet<>();
        }

        public static ReferencePropertyBuilder builder() {
            return new ReferencePropertyBuilder();
        }

        public ReferencePropertyBuilder refTableKey(String refTableKey) {
            this.refTableKey = refTableKey;
            return this;
        }

        public ReferencePropertyBuilder notNull(boolean isRequired) {
            this.notNull = isRequired;
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

        public ReferencePropertyBuilder constraints(Set<Constraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        @Override
        public Builder<ReferenceProperty> addConstraint(Constraint constraint) {
            this.constraints.add(constraint);
            return this;
        }

        public ReferenceProperty build() {
            return new ReferenceProperty(
                key,
                name,
                description,
                notNull,
                constraints,
                defaultValue,
                refTableKey
            );
        }
    }
}
