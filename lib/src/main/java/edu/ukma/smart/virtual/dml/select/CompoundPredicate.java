package edu.ukma.smart.virtual.dml.select;

public record CompoundPredicate(
    Predicate left, Predicate right, Operator op
) implements Predicate {

    public CompoundPredicate(Predicate left, Predicate right, Operator op) {
        this.left = left;
        this.right = right;
        this.op = op;
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
