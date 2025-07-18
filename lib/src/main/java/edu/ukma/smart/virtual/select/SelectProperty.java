package edu.ukma.smart.virtual.select;

public record SelectProperty(String propertyKey) {

    public static SelectProperty of(String propertyName) {
        return new SelectProperty(propertyName);
    }

}
