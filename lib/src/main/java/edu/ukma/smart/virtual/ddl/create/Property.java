package edu.ukma.smart.virtual.ddl.create;

public interface Property {

    String key();

    String name();

    String description();

    boolean unique();

    boolean required();

    Type type();

    enum Type {
        STRING,
        INTEGER,
        DECIMAL,
        REFERENCE,
        BOOLEAN
    }
}
