package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public final class StringPredicate implements RawPredicate<String> {
    private final String propertyKey;
    private final Operator op;
    private final String value;
    private final Type type;


    StringPredicate(String propertyKey, Operator op, String value) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.STRING;
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

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    public Operator op() {
        return op;
    }

    @Override
    public String value() {
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
        var that = (StringPredicate) obj;
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
        return "StringPredicate[propertyKey=%s, op=%s, value=%s, type=%s]".formatted(propertyKey, op, value, type);
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
