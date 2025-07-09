package edu.ukma.smart.virtual.select;

import java.util.Objects;

public record IntegerPredicate(
    String columnKey,
    Operator op,
    Long value
) implements RawPredicate<Long> {

    public IntegerPredicate(String columnKey, Operator op, Long value) {
        this.columnKey = Objects.requireNonNull(columnKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
    }

    public static IntegerPredicate eq(String columnKey, Long value) {
        return new IntegerPredicate(columnKey, Operator.EQUAL, value);
    }

    public static IntegerPredicate ne(String columnKey, Long value) {
        return new IntegerPredicate(columnKey, Operator.NOT_EQUAL, value);
    }

    public static IntegerPredicate ls(String columnKey, Long value) {
        return new IntegerPredicate(columnKey, Operator.LESS, value);
    }

    public static IntegerPredicate gt(String columnKey, Long value) {
        return new IntegerPredicate(columnKey, Operator.GREATER, value);
    }

    public static IntegerPredicate lse(String columnKey, Long value) {
        return new IntegerPredicate(columnKey, Operator.LESS_OR_EQUAL, value);
    }

    public static IntegerPredicate gre(String columnKey, Long value) {
        return new IntegerPredicate(columnKey, Operator.GREATER_OR_EQUAL, value);
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
