package edu.ukma.smart.virtual.values;

public record BooleanValue(String key, Boolean value) implements ColumnValue<Boolean> {

    public static BooleanValue of(String key, Boolean value) {
        return new BooleanValue(key, value);
    }
}
