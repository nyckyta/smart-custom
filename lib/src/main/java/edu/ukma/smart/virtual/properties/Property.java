package edu.ukma.smart.virtual.properties;

public interface Property {

    String key();
    String name();
    String description();

    boolean isUnique();
    boolean isRequired();
}
