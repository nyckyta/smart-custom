package edu.ukma.smart.virtual.create;

import java.math.BigDecimal;

public record DecimalProperty(
    String key,
    String name,
    String description,
    BigDecimal defaultValue,
    boolean required,
    boolean unique,
    int precision,
    int scale,
    BigDecimal min,
    BigDecimal max
) implements Property<BigDecimal> {

    public static DecimalPropertyBuilder builder() {
        return new DecimalPropertyBuilder();
    }

    public static final class DecimalPropertyBuilder {
        private String key;
        private String name;
        private String description;
        private BigDecimal defaultValue;
        private boolean required;
        private boolean unique;
        private int precision;
        private int scale;
        private BigDecimal max;
        private BigDecimal min;

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

        public DecimalPropertyBuilder unique(boolean isUnique) {
            this.unique = isUnique;
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

        public DecimalPropertyBuilder max(BigDecimal max) {
            this.max = max;
            return this;
        }

        public DecimalPropertyBuilder min(BigDecimal min) {
            this.min = min;
            return this;
        }

        public DecimalProperty build() {
            return new DecimalProperty(
                key,
                name,
                description,
                defaultValue,
                required,
                unique,
                precision,
                scale,
                min,
                max
            );
        }
    }
}
