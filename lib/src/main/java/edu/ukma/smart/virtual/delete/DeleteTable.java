package edu.ukma.smart.virtual.delete;

public record DeleteTable(String tableKey) {

    public static DeleteTable of(String tableKey) {
        return new DeleteTable(tableKey);
    }
}
