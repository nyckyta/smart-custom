package edu.ukma.smart.virtual.dml.select;

import java.util.List;
import java.util.Objects;

public final class ReferencePredicate implements RawPredicate<List<Integer>> {
    private final String propertyKey;
    private final Operator op;
    private final List<Integer> value;
    private final Type type;

    private ReferencePredicate(String propertyKey, Operator op, List<Integer> value) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.REFERENCE;
    }

    public static ReferencePredicate in(String propertyKey, List<Integer> value) {
        return new ReferencePredicate(propertyKey, Operator.IN, value);
    }

    public static ReferencePredicate notIn(String propertyKey, List<Integer> value) {
        return new ReferencePredicate(propertyKey, Operator.NOT_IN, value);
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    public Operator op() {
        return op;
    }

    @Override
    public List<Integer> value() {
        return value;
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
        var that = (ReferencePredicate) obj;
        return Objects.equals(this.propertyKey, that.propertyKey)
               && Objects.equals(this.op, that.op)
               && Objects.equals(this.value, that.value)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyKey, op, value, type);
    }

    @Override
    public String toString() {
        return "ReferencePredicate[propertyKey=%s, op=%s, value=%s, type=%s]".formatted(propertyKey, op, value, type);
    }


    public enum Operator {
        IN,
        NOT_IN
    }
}
