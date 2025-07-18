package edu.ukma.smart.virtual.create;

public interface Property<T> {

    String key();

    String name();

    String description();

    boolean unique();

    boolean required();

    T defaultValue();
}
