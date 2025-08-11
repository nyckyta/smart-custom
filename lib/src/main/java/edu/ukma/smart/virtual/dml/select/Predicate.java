package edu.ukma.smart.virtual.dml.select;

public interface Predicate {

    Type type();

    enum Type {
        COMPOUND,
        BOOLEAN,
        DECIMAL,
        NULL,
        INTEGER,
        STRING,
        REFERENCE,
    }
}
