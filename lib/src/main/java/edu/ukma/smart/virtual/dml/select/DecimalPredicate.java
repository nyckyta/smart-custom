package edu.ukma.smart.virtual.dml.select;

import java.math.BigDecimal;
import java.util.Objects;

public record DecimalPredicate(
    String propertyKey,
    Operator op,
    BigDecimal value
) implements RawPredicate<BigDecimal> {

    public DecimalPredicate(String propertyKey, Operator op, BigDecimal value) {
        this.propertyKey = Objects.requireNonNull(propertyKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
    }

    public static DecimalPredicate eq(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.EQUAL, value);
    }

    public static DecimalPredicate ne(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.NOT_EQUAL, value);
    }

    public static DecimalPredicate ls(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.LESS, value);
    }

    public static DecimalPredicate gt(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.GREATER, value);
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
