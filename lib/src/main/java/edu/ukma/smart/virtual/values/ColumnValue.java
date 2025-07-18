package edu.ukma.smart.virtual.values;

public interface ColumnValue<T> {
    String key();

    T value();
}
