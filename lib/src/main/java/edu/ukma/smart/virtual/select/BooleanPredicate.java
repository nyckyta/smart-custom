package edu.ukma.smart.virtual.select;

import java.util.Objects;

public record BooleanPredicate(
    String columnKey,
    Operator op,
    Boolean value
) implements RawPredicate<Boolean> {

    public BooleanPredicate(String columnKey, Operator op, Boolean value) {
        this.columnKey = Objects.requireNonNull(columnKey);
        this.op = Objects.requireNonNull(op);
        this.value = value;
    }

    public static BooleanPredicate eq(String columnName, Boolean value) {
        return new BooleanPredicate(Objects.requireNonNull(columnName), Operator.EQUAL, value);
    }

    public static BooleanPredicate ne(String columnName, Boolean value) {
        return new BooleanPredicate(Objects.requireNonNull(columnName), Operator.NOT_EQUAL, value);
    }

    public enum Operator {
        EQUAL,
        NOT_EQUAL,
    }
}
