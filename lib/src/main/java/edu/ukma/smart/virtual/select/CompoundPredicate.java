package edu.ukma.smart.virtual.select;

import java.util.Objects;

public record CompoundPredicate(
    Predicate left, Predicate right, Operator op
) implements Predicate {

    public CompoundPredicate(Predicate left, Predicate right, Operator op) {
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
        this.op = Objects.requireNonNull(op);
    }

    public static CompoundPredicate and(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.AND);
    }

    public static CompoundPredicate or(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.OR);
    }

    public enum Operator {
        AND,
        OR
    }
}
