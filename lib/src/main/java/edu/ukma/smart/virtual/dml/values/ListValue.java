package edu.ukma.smart.virtual.dml.values;

import java.util.Collections;
import java.util.List;

public record ListValue<V>(
    String key,
    List<V> value,
    Type type
) implements ColumnValue<List<V>> {

    public ListValue(String key, List<V> value, Type type) {
        this.key = key;
        this.value = value == null ? List.of() : Collections.unmodifiableList(value);
        this.type = type;
    }

    public static <V> ListValue<V> of(String key, Type type) {
        return new ListValue<>(key, List.of(), type);
    }

    public static <V> ListValue<V> of(String key, List<V> value, Type type) {
        return new ListValue<>(key, value, type);
    }

}
