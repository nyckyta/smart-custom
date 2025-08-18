package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class DecimalProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final BigDecimal defaultValue;
    private final boolean notNull;
    private final int precision;
    private final int scale;
    private final Set<Constraint> constraints;
    private final Type type;

    private DecimalProperty(
        String key,
        String name,
        String description,
        BigDecimal defaultValue,
        boolean notNull,
        int precision,
        int scale,
        Set<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.precision = precision;
        this.scale = scale;
        this.constraints = constraints == null ? Set.of() : Collections.unmodifiableSet(constraints);
        this.type = Type.DECIMAL;
    }

    public static DecimalPropertyBuilder builder() {
        return new DecimalPropertyBuilder();
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

    public BigDecimal defaultValue() {
        return defaultValue;
    }

    public boolean notNull() {
        return notNull;
    }

    public int precision() {
        return precision;
    }

    public int scale() {
        return scale;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Set<Constraint> constraints() {
        return constraints;
    }

    @Override
    public String toString() {
        return "DecimalProperty[" +
               "key=" + key + ", " +
               "name=" + name + ", " +
               "description=" + description + ", " +
               "defaultValue=" + defaultValue + ", " +
               "required=" + notNull + ", " +
               "precision=" + precision + ", " +
               "scale=" + scale + ", " +
               "type=" + type + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DecimalProperty that)) {
            return false;
        }
        return notNull == that.notNull && precision == that.precision && scale == that.scale &&
               Objects.equals(key, that.key) && Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) && Objects.equals(defaultValue, that.defaultValue) &&
               Objects.equals(constraints, that.constraints) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, defaultValue, notNull, precision, scale, constraints, type);
    }

    public static final class DecimalPropertyBuilder implements Builder<DecimalProperty> {
        private String key;
        private String name;
        private String description;
        private BigDecimal defaultValue;
        private boolean required;
        private int precision;
        private int scale;
        private Set<Constraint> constraint;

        private DecimalPropertyBuilder() {
            constraint = new HashSet<>();
        }

        public static DecimalPropertyBuilder builder() {
            return new DecimalPropertyBuilder();
        }

        public DecimalPropertyBuilder key(String key) {
            this.key = key;
            return this;
        }

        public DecimalPropertyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DecimalPropertyBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DecimalPropertyBuilder defaultValue(BigDecimal defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public DecimalPropertyBuilder notNull(boolean notNull) {
            this.required = notNull;
            return this;
        }

        public DecimalPropertyBuilder precision(int precision) {
            this.precision = precision;
            return this;
        }

        public DecimalPropertyBuilder scale(int scale) {
            this.scale = scale;
            return this;
        }

        public DecimalPropertyBuilder constraints(Set<Constraint> constraint) {
            this.constraint = constraint;
            return this;
        }

        @Override
        public Builder<DecimalProperty> addConstraint(Constraint constraint) {
            this.constraint.add(constraint);
            return this;
        }

        public DecimalProperty build() {
            return new DecimalProperty(
                key,
                name,
                description,
                defaultValue,
                required,
                precision,
                scale,
                constraint
            );
        }
    }
}
