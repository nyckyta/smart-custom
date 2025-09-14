package edu.ukma.smart.virtual.dml.values;

import java.util.Objects;

public final class StringValue implements ColumnValue<String> {
    private final String key;
    private final String value;
    private final Type type;


    private StringValue(String key, String value) {
        this.key = key;
        this.value = value;
        this.type = Type.STRING;
    }

    public static StringValue of(String key, String value) {
        return new StringValue(key, value);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String value() {
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
        var that = (StringValue) obj;
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
        return "StringValue[key=%s, value=%s, type=%s]".formatted(key, value, type);
    }

}
