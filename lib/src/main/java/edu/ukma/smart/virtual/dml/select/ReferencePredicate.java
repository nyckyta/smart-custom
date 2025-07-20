package edu.ukma.smart.virtual.dml.select;

import java.util.List;
import java.util.Objects;

public record ReferencePredicate(
    String propertyKey, Operator op, List<Integer> value
) implements RawPredicate<List<Integer>> {

    public ReferencePredicate(String propertyKey, Operator op, List<Integer> value) {
        this.propertyKey = Objects.requireNonNull(propertyKey);
        this.op = Objects.requireNonNull(op);
        this.value = Objects.requireNonNull(value);
    }

    public static ReferencePredicate in(String propertyKey, List<Integer> value) {
        return new ReferencePredicate(propertyKey, Operator.IN, value);
    }

    public static ReferencePredicate notIn(String propertyKey, List<Integer> value) {
        return new ReferencePredicate(propertyKey, Operator.NOT_IN, value);
    }

    public enum Operator {
        IN,
        NOT_IN
    }
}
