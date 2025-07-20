package edu.ukma.smart.virtual.dml.values;

public record BooleanValue(String key, Boolean value, Type type) implements ColumnValue<Boolean> {

    public BooleanValue(String key, Boolean value, Type type) {
        this.key = key;
        this.value = value;
        this.type = Type.BOOLEAN;
    }

    public static BooleanValue of(String key, Boolean value) {
        return new BooleanValue(key, value, Type.BOOLEAN);
    }
}
