package edu.ukma.smart.virtual.delete;

public record DeleteRow(String tableKey, int rowId) {

    public static DeleteRow of(String tableKey, int rowId) {
        return new DeleteRow(tableKey, rowId);
    }

}
