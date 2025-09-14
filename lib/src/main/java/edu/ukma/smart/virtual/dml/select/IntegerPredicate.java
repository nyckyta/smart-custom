package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public final class IntegerPredicate implements RawPredicate<Long> {
    private final String propertyKey;
    private final Operator op;
    private final Long value;
    private final Type type;

    private IntegerPredicate(String propertyKey, Operator op, Long value) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.INTEGER;
    }

    public static IntegerPredicate eq(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.EQUAL, value);
    }

    public static IntegerPredicate ne(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.NOT_EQUAL, value);
    }

    public static IntegerPredicate ls(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.LESS, value);
    }

    public static IntegerPredicate gt(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.GREATER, value);
    }

    public static IntegerPredicate lse(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.LESS_OR_EQUAL, value);
    }

    public static IntegerPredicate gre(String propertyKey, Long value) {
        return new IntegerPredicate(propertyKey, Operator.GREATER_OR_EQUAL, value);
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    public Operator op() {
        return op;
    }

    @Override
    public Long value() {
        return value;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IntegerPredicate) obj;
        return Objects.equals(this.propertyKey, that.propertyKey)
               && Objects.equals(this.op, that.op)
               && Objects.equals(this.value, that.value)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyKey, op, value, type);
    }

    @Override
    public String toString() {
        return "IntegerPredicate[propertyKey=%s, op=%s, value=%d, type=%s]".formatted(propertyKey, op, value, type);
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
