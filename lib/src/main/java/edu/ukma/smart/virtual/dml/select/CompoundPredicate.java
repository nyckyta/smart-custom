package edu.ukma.smart.virtual.dml.select;

public record CompoundPredicate(
    Predicate left, Predicate right, Operator op, Type type
) implements Predicate {

    public CompoundPredicate(Predicate left, Predicate right, Operator op, Type type) {
        this.left = left;
        this.right = right;
        this.op = op;
        this.type = Type.COMPOUND;
    }

    public static CompoundPredicate and(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.AND, Type.COMPOUND);
    }

    public static CompoundPredicate or(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.OR, Type.COMPOUND);
    }

    public enum Operator {
        AND,
        OR
    }
}
