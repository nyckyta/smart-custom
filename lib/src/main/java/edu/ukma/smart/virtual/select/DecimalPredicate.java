package edu.ukma.smart.virtual.select;

import java.math.BigDecimal;
import java.util.Objects;

public record DecimalPredicate(
    String columnKey,
    Operator op,
    BigDecimal value
) implements RawPredicate<BigDecimal> {

    public DecimalPredicate(String columnKey, Operator op, BigDecimal value) {
        this.columnKey = Objects.requireNonNull(columnKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
    }

    public static DecimalPredicate eq(String columnKey, BigDecimal value) {
        return new DecimalPredicate(columnKey, Operator.EQUAL, value);
    }

    public static DecimalPredicate ne(String columnKey, BigDecimal value) {
        return new DecimalPredicate(columnKey, Operator.NOT_EQUAL, value);
    }

    public static DecimalPredicate ls(String columnKey, BigDecimal value) {
        return new DecimalPredicate(columnKey, Operator.LESS, value);
    }

    public static DecimalPredicate gt(String columnKey, BigDecimal value) {
        return new DecimalPredicate(columnKey, Operator.GREATER, value);
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
