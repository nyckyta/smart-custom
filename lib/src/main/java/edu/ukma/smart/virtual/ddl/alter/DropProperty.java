package edu.ukma.smart.virtual.ddl.alter;

public record DropProperty(String tableKey, String columnKey) {

    public static DropProperty of(String tableKey, String columnKey) {
        return new DropProperty(tableKey, columnKey);
    }
}
