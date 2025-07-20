package edu.ukma.smart.virtual.dml.values;

public record StringValue(String key, String value) implements ColumnValue<String> {

    public static StringValue of(String key, String value) {
        return new StringValue(key, value);
    }
}
