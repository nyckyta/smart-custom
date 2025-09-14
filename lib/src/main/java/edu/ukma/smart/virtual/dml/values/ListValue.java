package edu.ukma.smart.virtual.dml.values;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ListValue<V> implements ColumnValue<List<V>> {
    private final String key;
    private final List<V> value;
    private final ListType elementsType;
    private final Type type;

    private ListValue(String key, List<V> value, ListType elementsType) {
        this.key = key;
        this.value = value;
        this.elementsType = elementsType;
        this.type = Type.LIST;
    }

    public static ListValue<String> ofStrings(String key, List<String> value) {
        return new ListValue<>(key, value, ListType.STRING);
    }

    public static ListValue<BigDecimal> ofDecimals(String key, List<BigDecimal> value) {
        return new ListValue<>(key, value == null ? List.of() : Collections.unmodifiableList(value), ListType.DECIMAL);
    }

    public static ListValue<Integer> ofReference(String key, List<Integer> value) {
        return new ListValue<>(key, value == null ? List.of() : Collections.unmodifiableList(value), ListType.REFERENCE);
    }

    public static ListValue<Long> ofInts(String key, List<Long> value) {
        return new ListValue<>(key, value == null ? List.of() : Collections.unmodifiableList(value), ListType.INTEGER);
    }

    public static ListValue<Boolean> ofBooleans(String key, List<Boolean> value) {
        return new ListValue<>(key, value == null ? List.of() : Collections.unmodifiableList(value), ListType.BOOLEAN);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public List<V> value() {
        return value;
    }

    public ListType elementsType() {
        return elementsType;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ListValue) obj;
        return Objects.equals(this.key, that.key)
               && Objects.equals(this.value, that.value)
               && Objects.equals(this.elementsType, that.elementsType)
               && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, elementsType, type);
    }

    @Override
    public String toString() {
        return "ListValue[key=%s, value=%s, elementsType=%s, type=%s]".formatted(key, value, elementsType, type);
    }


    public enum ListType {
        BOOLEAN,
        INTEGER,
        DECIMAL,
        REFERENCE,
        STRING,
    }
}
