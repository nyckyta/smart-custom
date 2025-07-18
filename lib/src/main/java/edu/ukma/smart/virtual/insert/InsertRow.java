package edu.ukma.smart.virtual.insert;

import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.List;

public record InsertRow(String tableKey, List<? extends ColumnValue<?>> columnValues) {

    public InsertRow(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        this.tableKey = tableKey;
        this.columnValues = columnValues == null ? List.of() : columnValues;
    }

    public static InsertRow of(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        return new InsertRow(tableKey, columnValues);
    }
}
