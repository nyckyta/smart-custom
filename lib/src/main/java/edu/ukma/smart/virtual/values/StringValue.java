package edu.ukma.smart.virtual.values;

public class StringValue implements ColumnValue {

    public final String key;
    public final String value;

    private StringValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static StringValue of(String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        return new StringValue(key, value);
    }

    @Override
    public String key() {
        return key;
    }
}
