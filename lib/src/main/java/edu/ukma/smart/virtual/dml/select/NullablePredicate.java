package edu.ukma.smart.virtual.dml.select;

public record NullablePredicate(String propertyKey, Operator op, Type type) implements RawPredicate<Object> {

    private static final Object NULL = new Object();

    public NullablePredicate(String propertyKey, Operator op, Type type) {
        this.propertyKey = propertyKey;
        this.op = op;
        this.type = Type.NULL;
    }

    public static NullablePredicate isNull(String propertyName) {
        return new NullablePredicate(propertyName, Operator.IS_NULL, Type.NULL);
    }

    public static NullablePredicate isNotNull(String propertyName) {
        return new NullablePredicate(propertyName, Operator.IS_NOT_NULL, Type.NULL);
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
