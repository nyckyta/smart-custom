package edu.ukma.smart.virtual.dml.values;

public record IntegerValue(String key, Long value, Type type)
    implements ColumnValue<Long> {

    public IntegerValue(String key, Long value, Type type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public static IntegerValue of(String key, Long value) {
        return new IntegerValue(key, value, Type.INTEGER);
    }
}
