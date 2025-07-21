package edu.ukma.smart.virtual.dml.select;

import java.util.List;

public record ReferencePredicate(
    String propertyKey, Operator op, List<Integer> value, Type type
) implements RawPredicate<List<Integer>> {

    public ReferencePredicate(String propertyKey, Operator op, List<Integer> value, Type type) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.REFERENCE;
    }

    public static ReferencePredicate in(String propertyKey, List<Integer> value) {
        return new ReferencePredicate(propertyKey, Operator.IN, value, Type.REFERENCE);
    }

    public static ReferencePredicate notIn(String propertyKey, List<Integer> value) {
        return new ReferencePredicate(propertyKey, Operator.NOT_IN, value, Type.REFERENCE);
    }

    public enum Operator {
        IN,
        NOT_IN
    }
}
