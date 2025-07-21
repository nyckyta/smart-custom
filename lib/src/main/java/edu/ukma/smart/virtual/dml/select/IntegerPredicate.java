package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public record IntegerPredicate(
    String propertyKey,
    Operator op,
    Long value,
    Type type
) implements RawPredicate<Long> {

    public IntegerPredicate(String propertyKey, Operator op, Long value, Type type) {
        this.propertyKey = Objects.requireNonNull(propertyKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
        this.type = Type.INTEGER;
    }

    public static IntegerPredicate eq(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.EQUAL, value, Type.INTEGER);
    }

    public static IntegerPredicate ne(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.NOT_EQUAL, value, Type.INTEGER);
    }

    public static IntegerPredicate ls(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.LESS, value, Type.INTEGER);
    }

    public static IntegerPredicate gt(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.GREATER, value, Type.INTEGER);
    }

    public static IntegerPredicate lse(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.LESS_OR_EQUAL, value, Type.INTEGER);
    }

    public static IntegerPredicate gre(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.GREATER_OR_EQUAL, value, Type.INTEGER);
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
