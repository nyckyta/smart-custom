package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public record StringPredicate(
    String propertyKey,
    Operator op,
    String value
) implements RawPredicate<String> {

    public StringPredicate(String propertyKey, Operator op, String value) {
        this.propertyKey = propertyKey;
        this.op = Objects.requireNonNull(op);
        this.value = value;
    }

    public static StringPredicate eq(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.EQUAL, value);
    }

    public static StringPredicate ne(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.NOT_EQUAL, value);
    }

    public static StringPredicate gt(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.GREATER, value);
    }

    public static StringPredicate ls(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.LESS, value);
    }

    public static StringPredicate like(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.LIKE, value);
    }

    public static StringPredicate notLike(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.NOT_LIKE, value);
    }

    public static StringPredicate lse(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.LESS_OR_EQUAL, value);
    }

    public static StringPredicate gte(String propertyKey, String value) {
        return new StringPredicate(propertyKey, Operator.GREATER_OR_EQUAL, value);
    }

    public enum Operator {
        NOT_EQUAL,
        EQUAL,
        LESS,
        GREATER,
        LESS_OR_EQUAL,
        GREATER_OR_EQUAL,
        LIKE,
        NOT_LIKE,
    }
}
