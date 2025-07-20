package edu.ukma.smart.virtual.dml.values;

import static edu.ukma.smart.virtual.dml.values.Type.REFERENCE;

public record ReferenceValue(String key, Integer value, Type type)
    implements ColumnValue<Integer> {

    public ReferenceValue(String key, Integer value, Type type) {
        this.key = key;
        this.value = value;
        this.type = REFERENCE;
    }

    public static ReferenceValue of(String key, Integer value) {
        return new ReferenceValue(key, value, REFERENCE);
    }
}
