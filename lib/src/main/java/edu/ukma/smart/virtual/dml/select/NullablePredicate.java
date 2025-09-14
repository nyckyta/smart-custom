package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public final class NullablePredicate implements RawPredicate<Object> {

    private static final Object NULL = new Object();
    private final String propertyKey;
    private final Operator op;
    private final Type type;

    private NullablePredicate(String propertyKey, Operator op) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.type = Type.NULL;
    }

    public static NullablePredicate isNull(String propertyName) {
        return new NullablePredicate(propertyName, Operator.IS_NULL);
    }

    public static NullablePredicate isNotNull(String propertyName) {
        return new NullablePredicate(propertyName, Operator.IS_NOT_NULL);
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    // Nullable predicate does not use value
    @Override
    public Object value() {
        return NULL;
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
        var that = (NullablePredicate) obj;
        return Objects.equals(this.propertyKey, that.propertyKey)
               && Objects.equals(this.op, that.op)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyKey, op, type);
    }

    @Override
    public String toString() {
        return "NullablePredicate[propertyKey=%s, op=%s, type=%s]".formatted(propertyKey, op, type);
    }

    public enum Operator {
        IS_NULL,
        IS_NOT_NULL
    }
}
