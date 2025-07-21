package edu.ukma.smart.virtual.dml.select;

import java.math.BigDecimal;
import java.util.Objects;

public record DecimalPredicate(
    String propertyKey,
    Operator op,
    BigDecimal value,
    Type type
) implements RawPredicate<BigDecimal> {

    public DecimalPredicate(String propertyKey, Operator op, BigDecimal value, Type type) {
        this.propertyKey = Objects.requireNonNull(propertyKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
        this.type = Type.DECIMAL;
    }

    public static DecimalPredicate eq(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.EQUAL, value, Type.DECIMAL);
    }

    public static DecimalPredicate ne(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.NOT_EQUAL, value, Type.DECIMAL);
    }

    public static DecimalPredicate ls(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.LESS, value, Type.DECIMAL);
    }

    public static DecimalPredicate gt(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.GREATER, value, Type.DECIMAL);
    }

    public enum Operator {
        NOT_EQUAL,
        EQUAL,
        LESS,
        GREATER,
        GREATER_OR_EQUAL,
        LESS_OR_EQUAL,
    }
}
