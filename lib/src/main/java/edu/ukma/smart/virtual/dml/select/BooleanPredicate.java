package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public record BooleanPredicate(
    String propertyKey,
    Operator op,
    Boolean value,
    Type type
) implements RawPredicate<Boolean> {

    public BooleanPredicate(String propertyKey, Operator op, Boolean value, Type type) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.BOOLEAN;
    }

    public static BooleanPredicate eq(String propertyName, Boolean value) {
        return new BooleanPredicate(propertyName, Operator.EQUAL, value, Type.BOOLEAN);
    }

    public static BooleanPredicate ne(String propertyName, Boolean value) {
        return new BooleanPredicate(Objects.requireNonNull(propertyName), Operator.NOT_EQUAL, value, Type.BOOLEAN);
    }

    public enum Operator {
        EQUAL,
        NOT_EQUAL,
    }
}
