package edu.ukma.smart.virtual.dml.select;

public interface RawPredicate<V> extends Predicate {

    String propertyKey();

    V value();
}
