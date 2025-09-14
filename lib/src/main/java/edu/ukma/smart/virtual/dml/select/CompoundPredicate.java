package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public final class CompoundPredicate implements Predicate {
    private final Predicate left;
    private final Predicate right;
    private final Operator op;
    private final Type type;

    private CompoundPredicate(Predicate left, Predicate right, Operator op) {
        this.left = left;
        this.right = right;
        this.op = op;
        this.type = Type.COMPOUND;
    }

    public static CompoundPredicate and(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.AND);
    }

    public static CompoundPredicate or(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.OR);
    }

    public Predicate left() {
        return left;
    }

    public Predicate right() {
        return right;
    }

    public Operator op() {
        return op;
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
        var that = (CompoundPredicate) obj;
        return Objects.equals(this.left, that.left)
               && Objects.equals(this.right, that.right)
               && Objects.equals(this.op, that.op)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, op, type);
    }

    @Override
    public String toString() {
        return "CompoundPredicate[left=%s, right=%s, op=%s, type=%s]".formatted(left, right, op, type);
    }


    public enum Operator {
        AND,
        OR
    }
}
