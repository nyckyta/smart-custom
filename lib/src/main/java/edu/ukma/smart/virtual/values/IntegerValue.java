package edu.ukma.smart.virtual.values;

public record IntegerValue(String key, Long value)
    implements ColumnValue{

    public static IntegerValue of(String key, Long value) {
        return new IntegerValue(key, value);
    }
}
