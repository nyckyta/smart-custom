package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public final class BooleanPredicate implements RawPredicate<Boolean> {
    private final String propertyKey;
    private final Operator op;
    private final Boolean value;
    private final Type type;

    private BooleanPredicate(String propertyKey, Operator op, Boolean value) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.BOOLEAN;
    }

    public static BooleanPredicate eq(String propertyName, Boolean value) {
        return new BooleanPredicate(propertyName, Operator.EQUAL, value);
    }

    public static BooleanPredicate ne(String propertyName, Boolean value) {
        return new BooleanPredicate(Objects.requireNonNull(propertyName), Operator.NOT_EQUAL, value);
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    public Operator op() {
        return op;
    }

    @Override
    public Boolean value() {
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
        var that = (BooleanPredicate) obj;
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
        return "BooleanPredicate[propertyKey=%s, op=%s, value=%s, type=%s]".formatted(propertyKey, op, value, type);
    }

    public enum Operator {
        EQUAL,
        NOT_EQUAL,
    }
}
