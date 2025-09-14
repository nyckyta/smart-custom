package edu.ukma.smart.virtual.dml.values;

import java.util.Objects;

public final class IntegerValue
    implements ColumnValue<Long> {
    private final String key;
    private final Long value;
    private final Type type;

    private IntegerValue(String key, Long value) {
        this.key = key;
        this.value = value;
        this.type = Type.INTEGER;
    }

    public static IntegerValue of(String key, Long value) {
        return new IntegerValue(key, value);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Long value() {
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
        var that = (IntegerValue) obj;
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
        return "IntegerValue[key=%s, value=%d, type=%s]".formatted(key, value, type);
    }

}
