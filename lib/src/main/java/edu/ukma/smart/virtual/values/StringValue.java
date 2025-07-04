package edu.ukma.smart.virtual.values;

public record StringValue(String key, String value) implements ColumnValue {

    public static StringValue of(String key, String value) {
        return new StringValue(key, value);
    }
}
