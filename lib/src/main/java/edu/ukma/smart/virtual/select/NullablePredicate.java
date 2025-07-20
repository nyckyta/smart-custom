package edu.ukma.smart.virtual.select;

public record NullablePredicate(String propertyKey, Operator op) implements RawPredicate<Object> {

    private static final Object NULL = new Object();

    public static NullablePredicate isNull(String propertyName) {
        return new NullablePredicate(propertyName, Operator.IS_NULL);
    }

    public static NullablePredicate isNotNull(String propertyName) {
        return new NullablePredicate(propertyName, Operator.IS_NOT_NULL);
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    // Nullable predicate does not use value
    @Override
    public Object value() {
        return NULL;
    }

    public enum Operator {
        IS_NULL,
        IS_NOT_NULL
    }
}
