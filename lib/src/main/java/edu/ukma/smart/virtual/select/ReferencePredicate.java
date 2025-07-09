package edu.ukma.smart.virtual.select;

import java.util.List;
import java.util.Objects;

public record ReferencePredicate(
    String columnKey, Operator op, List<Integer> value
) implements RawPredicate<List<Integer>> {

    public ReferencePredicate(String columnKey, Operator op, List<Integer> value) {
        this.columnKey = Objects.requireNonNull(columnKey);
        this.op = Objects.requireNonNull(op);
        this.value = Objects.requireNonNull(value);
    }

    public static ReferencePredicate in(String columnKey, List<Integer> value) {
        return new ReferencePredicate(columnKey, Operator.IN, value);
    }

    public static ReferencePredicate notIn(String columnKey, List<Integer> value) {
        return new ReferencePredicate(columnKey, Operator.NOT_IN, value);
    }

    public enum Operator {
        IN,
        NOT_IN
    }
}
