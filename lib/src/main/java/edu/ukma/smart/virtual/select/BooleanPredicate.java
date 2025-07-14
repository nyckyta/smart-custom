package edu.ukma.smart.virtual.select;

import java.util.Objects;

public record BooleanPredicate(
    String propertyKey,
    Operator op,
    Boolean value
) implements RawPredicate<Boolean> {

    public BooleanPredicate(String propertyKey, Operator op, Boolean value) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
    }

    public static BooleanPredicate eq(String propertyName, Boolean value) {
        return new BooleanPredicate(propertyName, Operator.EQUAL, value);
    }

    public static BooleanPredicate ne(String propertyName, Boolean value) {
        return new BooleanPredicate(Objects.requireNonNull(propertyName), Operator.NOT_EQUAL, value);
    }

    public enum Operator {
        EQUAL,
        NOT_EQUAL,
    }
}
