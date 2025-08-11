package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public final class DecimalProperty implements Property {
    private final String key;
    private final String name;
    private final String description;
    private final BigDecimal defaultValue;
    private final boolean notNull;
    private final int precision;
    private final int scale;
    private final List<Constraint> constraints;
    private final Type type;

    private DecimalProperty(
        String key,
        String name,
        String description,
        BigDecimal defaultValue,
        boolean notNull,
        int precision,
        int scale,
        List<Constraint> constraints
    ) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
        this.precision = precision;
        this.scale = scale;
        this.constraints = constraints == null ? List.of() : Collections.unmodifiableList(constraints);
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
    public List<Constraint> constraints() {
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


    public static final class DecimalPropertyBuilder {
        private String key;
        private String name;
        private String description;
        private BigDecimal defaultValue;
        private boolean required;
        private int precision;
        private int scale;
        private List<Constraint> constraint;

        private DecimalPropertyBuilder() {
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

        public DecimalPropertyBuilder required(boolean isRequired) {
            this.required = isRequired;
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

        public DecimalPropertyBuilder constraints(List<Constraint> constraint) {
            this.constraint = constraint;
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
