package edu.ukma.smart.virtual.values;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ListValue<V>(
    String key,
    List<V> value,
    Type type
) implements ColumnValue<List<V>> {

    public ListValue(String key, List<V> value, Type type) {
        this.key = key;
        this.value = Collections.unmodifiableList(value);
        this.type = type;
    }

    public static <V> ListValue<V> of(String key, Type type) {
        return new ListValue<>(key, List.of(), type);
    }

    public static <V> ListValue<V> of(String key, List<V> value, Type type) {
        return new ListValue<>(key, value, type);
    }

    @Override
    public Optional<Err> validate() {
        if (value == null) {
            return Optional.of(InputValidationErr.error("List value can not be null"));
        }

        if (type == null) {
            return Optional.of(InputValidationErr.error("Type can not be null"));
        }

        return ColumnValue.super.validate();
    }
}
