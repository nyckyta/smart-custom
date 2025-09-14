package edu.ukma.smart.virtual.dml.values;

import java.math.BigDecimal;
import java.util.Objects;

public final class DecimalValue implements ColumnValue<BigDecimal> {
    private final String key;
    private final BigDecimal value;
    private final Type type;

    private DecimalValue(String key, BigDecimal value) {
        this.key = key;
        this.value = value;
        this.type = Type.DECIMAL;
    }

    public static DecimalValue of(String key, BigDecimal value) {
        return new DecimalValue(key, value);
    }

    @Override
    public String key() {
        return key;
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
        var that = (DecimalValue) obj;
        return Objects.equals(this.key, that.key)
               && Objects.equals(this.value, that.value)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, type);
    }

    @Override
    public String toString() {
        return "DecimalValue[key=%s, value=%s, type=%s]".formatted(key, value, type);
    }

}
