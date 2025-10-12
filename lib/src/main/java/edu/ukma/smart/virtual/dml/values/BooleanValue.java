package edu.ukma.smart.virtual.dml.values;

import java.util.Objects;

public final class BooleanValue implements ColumnValue<Boolean> {
    private final String key;
    private final Boolean value;
    private final Type type;

    private BooleanValue(String key, Boolean value) {
        this.key = key;
        this.value = value;
        this.type = Type.BOOLEAN;
    }

    public static BooleanValue of(String key, Boolean value) {
        return new BooleanValue(key, value);
    }

    @Override
    public String key() {
        return key;
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
        var that = (BooleanValue) obj;
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
        return "BooleanValue[key=%s, value=%s, type=%s]".formatted(key, value, type);
    }

}
