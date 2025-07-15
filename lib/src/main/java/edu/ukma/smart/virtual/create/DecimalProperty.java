package edu.ukma.smart.virtual.create;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_DEFAULT_GREATER_MAX_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_DEFAULT_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_MAX_VAL_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_PRECISION_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_SCALE_IS_INVALID;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

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

    private static final int MAX_PRECISION = 131072;
    private static final int MAX_SCALE = 16383;

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

    @Override
    public Optional<Err> validate() {
        if (precision < 1 || precision > MAX_PRECISION) {
            return Optional.of(InputValidationErr.error(DECIMAL_PRECISION_IS_INVALID));
        }

        if (scale < 1 || scale > MAX_SCALE) {
            return Optional.of(InputValidationErr.error(DECIMAL_SCALE_IS_INVALID));
        }

        boolean maxSet = max != null;
        boolean minSet = min != null;
        boolean defaultSet = defaultValue != null;

        if (defaultSet) {
            if (maxSet && defaultValue.compareTo(max) > 0) {
                return Optional.of(
                    InputValidationErr.error(DECIMAL_DEFAULT_GREATER_MAX_VAL)
                );
            }

            if (minSet && defaultValue.compareTo(min) < 0) {
                return Optional.of(InputValidationErr.error(DECIMAL_DEFAULT_LESS_MIN_VAL));
            }
        }

        if (minSet && maxSet) {
            if (max.compareTo(min) < 0) {
                return Optional.of(InputValidationErr.error(DECIMAL_MAX_VAL_LESS_MIN_VAL));
            }
        }

        return Property.super.validate();
    }
}
