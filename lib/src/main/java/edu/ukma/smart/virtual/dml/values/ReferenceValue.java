package edu.ukma.smart.virtual.dml.values;

import static edu.ukma.smart.virtual.dml.values.Type.REFERENCE;

import java.util.Objects;

public final class ReferenceValue
    implements ColumnValue<Integer> {
    private final String key;
    private final Integer value;
    private final Type type;

    private ReferenceValue(String key, Integer value) {
        this.key = key;
        this.value = value;
        this.type = REFERENCE;
    }

    public static ReferenceValue of(String key, Integer value) {
        return new ReferenceValue(key, value);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Integer value() {
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
        var that = (ReferenceValue) obj;
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
        return "ReferenceValue[key=%s, value=%d, type=%s]".formatted(key, value, type);
    }

}
