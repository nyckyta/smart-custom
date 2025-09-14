package edu.ukma.smart.virtual.dml.insert;

import edu.ukma.smart.virtual.dml.values.ColumnValue;
import java.util.List;
import java.util.Objects;

public final class InsertRow {
    private final String tableKey;
    private final List<? extends ColumnValue<?>> columnValues;

    private InsertRow(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        this.tableKey = tableKey;
        this.columnValues = columnValues == null ? List.of() : columnValues;
    }

    public static InsertRow of(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        return new InsertRow(tableKey, columnValues);
    }

    public String tableKey() {
        return tableKey;
    }

    public List<? extends ColumnValue<?>> columnValues() {
        return columnValues;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (InsertRow) obj;
        return Objects.equals(this.tableKey, that.tableKey)
               && Objects.equals(this.columnValues, that.columnValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey, columnValues);
    }

    @Override
    public String toString() {
        return "InsertRow[tableKey=%s, columnValues=%s]".formatted(tableKey, columnValues);
    }

}
