package edu.ukma.smart.virtual.dml.select;

import java.math.BigDecimal;
import java.util.Objects;

public final class DecimalPredicate implements RawPredicate<BigDecimal> {
    private final String propertyKey;
    private final Operator op;
    private final BigDecimal value;
    private final Type type;

    private DecimalPredicate(String propertyKey, Operator op, BigDecimal value) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.value = value;
        this.type = Type.DECIMAL;
    }

    public static DecimalPredicate eq(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.EQUAL, value);
    }

    public static DecimalPredicate ne(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.NOT_EQUAL, value);
    }

    public static DecimalPredicate ls(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.LESS, value);
    }

    public static DecimalPredicate gt(String propertyKey, BigDecimal value) {
        return new DecimalPredicate(propertyKey, Operator.GREATER, value);
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    public Operator op() {
        return op;
    }

    @Override
    public BigDecimal value() {
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
        var that = (DecimalPredicate) obj;
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
        return "DecimalPredicate[propertyKey=%s, op=%s, value=%s, type=%s]".formatted(propertyKey, op, value, type);
    }

    public enum Operator {
        NOT_EQUAL,
        EQUAL,
        LESS,
        GREATER,
        GREATER_OR_EQUAL,
        LESS_OR_EQUAL,
    }
}
