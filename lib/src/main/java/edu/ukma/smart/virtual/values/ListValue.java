package edu.ukma.smart.virtual.values;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ListValue<V>(
    String key,
    List<V> value,
    Type type
) implements ColumnValue<List<V>> {

    public ListValue(String key, List<V> value, Type type) {
        this.key = Objects.requireNonNull(key);
        this.value = Collections.unmodifiableList(value);
        this.type = Objects.requireNonNull(type);
    }

    public static <V> ListValue<V> of(String key, Type type) {
        return new ListValue<>(key, List.of(), type);
    }

    public static <V> ListValue<V> of(String key, List<V> value, Type type) {
        return new ListValue<>(key, value, type);
    }
}
