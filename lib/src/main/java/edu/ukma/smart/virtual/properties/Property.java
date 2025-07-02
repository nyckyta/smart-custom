package edu.ukma.smart.virtual.properties;

public interface Property<T> {

    String key();
    String name();
    String description();
    boolean isUnique();
    boolean isRequired();
    T defaultValue();
}
