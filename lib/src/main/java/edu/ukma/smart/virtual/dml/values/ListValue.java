package edu.ukma.smart.virtual.dml.values;

import java.util.Collections;
import java.util.List;

public record ListValue<V>(
    String key,
    List<V> value,
    ListType elementsType,
    Type type
) implements ColumnValue<List<V>> {

    public ListValue(String key, List<V> value, ListType elementsType, Type type) {
        this.key = key;
        this.value = value == null ? List.of() : Collections.unmodifiableList(value);
        this.elementsType = elementsType;
        this.type = Type.LIST;
    }

    public static <V> ListValue<V> of(String key, ListType elementsType) {
        return new ListValue<>(key, List.of(), elementsType, Type.LIST);
    }

    public static <V> ListValue<V> of(String key, List<V> value, ListType elementsType) {
        return new ListValue<>(key, value, elementsType, Type.LIST);
    }

    public enum ListType {
        BOOLEAN,
        INTEGER,
        DECIMAL,
        REFERENCE,
        STRING,
    }
}
