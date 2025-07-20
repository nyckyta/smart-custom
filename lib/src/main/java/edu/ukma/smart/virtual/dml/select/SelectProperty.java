package edu.ukma.smart.virtual.dml.select;

public record SelectProperty(String propertyKey) {

    public static SelectProperty of(String propertyName) {
        return new SelectProperty(propertyName);
    }

}
