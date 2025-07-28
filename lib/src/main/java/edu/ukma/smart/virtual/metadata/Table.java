package edu.ukma.smart.virtual.metadata;

public record Table(String id, String name, String description) {

    public static Table of(String id, String name, String description) {
        return new Table(id, name, description);
    }
}
