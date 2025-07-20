package edu.ukma.smart.virtual.dml.values;

public record ReferenceValue(String key, Integer value)
    implements ColumnValue<Integer> {

    public static ReferenceValue of(String key, Integer value) {
        return new ReferenceValue(key, value);
    }
}
