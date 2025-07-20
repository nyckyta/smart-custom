package edu.ukma.smart.virtual.dml.values;

public interface ColumnValue<T> {
    String key();

    T value();

    Type type();
}
