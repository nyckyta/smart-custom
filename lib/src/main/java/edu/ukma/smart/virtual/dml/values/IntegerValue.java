package edu.ukma.smart.virtual.dml.values;

public record IntegerValue(String key, Long value)
    implements ColumnValue<Long> {

    public static IntegerValue of(String key, Long value) {
        return new IntegerValue(key, value);
    }
}
