package edu.ukma.smart.virtual.dml.values;

public record StringValue(String key, String value, Type type) implements ColumnValue<String> {

    public StringValue(String key, String value, Type type) {
        this.key = key;
        this.value = value;
        this.type = Type.STRING;
    }

    public static StringValue of(String key, String value) {
        return new StringValue(key, value, Type.STRING);
    }
}
