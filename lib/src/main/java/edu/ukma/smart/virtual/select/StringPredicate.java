package edu.ukma.smart.virtual.select;

import java.util.Objects;

public record StringPredicate(
    String columnKey,
    Operator op,
    String value
) implements RawPredicate<String> {

    public StringPredicate(String columnKey, Operator op, String value) {
        this.columnKey = Objects.requireNonNull(columnKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
    }

    public static StringPredicate eq(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.EQUAL, value);
    }

    public static StringPredicate ne(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.NOT_EQUAL, value);
    }

    public static StringPredicate gt(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.GREATER, value);
    }

    public static StringPredicate ls(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.LESS, value);
    }

    public static StringPredicate like(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.LIKE, value);
    }

    public static StringPredicate notLike(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.NOT_LIKE, value);
    }

    public static StringPredicate lse(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.LESS_OR_EQUAL, value);
    }

    public static StringPredicate gte(String columnKey, String value) {
        return new StringPredicate(columnKey, Operator.GREATER_OR_EQUAL, value);
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
