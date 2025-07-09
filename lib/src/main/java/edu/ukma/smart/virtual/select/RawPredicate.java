package edu.ukma.smart.virtual.select;

public interface RawPredicate<V> extends Predicate {

    String columnKey();

    V value();
}
