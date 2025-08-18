package edu.ukma.smart.virtual.ddl.constraints;


import java.util.Arrays;
import java.util.Objects;

public class PropertyConstraint implements Constraint {

    public final Type type;
    public final Object value;

    private PropertyConstraint(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static PropertyConstraint maxLength(Integer value) {
        return new PropertyConstraint(Type.STRING_MAX_LENGTH, value);
    }

    public static PropertyConstraint minLength(Integer value) {
        return new PropertyConstraint(Type.STRING_MIN_LENGTH, value);
    }

    public static PropertyConstraint greaterThan(Object value) {
        return new PropertyConstraint(Type.GREATER_THAN_VALUE, value);
    }

    public static PropertyConstraint lessThan(Object value) {
        return new PropertyConstraint(Type.GREATER_THAN_VALUE, value);
    }

    public static PropertyConstraint greaterOrEqual(Object value) {
        return new PropertyConstraint(Type.GREATER_OR_EQUAL_THAN_VALUE, value);
    }

    public static PropertyConstraint lessOrEqual(Object value) {
        return new PropertyConstraint(Type.LESS_OR_EQUAL_THAN_VALUE, value);
    }

    public static <T> PropertyConstraint in(T[] values) {
        return new PropertyConstraint(Type.IN, values);
    }

    public static <T> PropertyConstraint notIn(T[] values) {
        return new PropertyConstraint(Type.NOT_IN, values);
    }

    public <T> T castValue(Class<T> clazz) {
        return clazz.cast(value);
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyConstraint that)) {
            return false;
        }

        return type == that.type && Objects.deepEquals(value, that.value);
    }

    @Override
    public int hashCode() {
        if (value instanceof Object[] arr) {
            return Objects.hash(type) + Arrays.deepHashCode(arr);
        }

        return Objects.hash(type, value);
    }
}
