package edu.ukma.smart.virtual.dml.delete;

import java.util.Objects;

public final class DeleteRow {
    private final String tableKey;
    private final int rowId;

    private DeleteRow(String tableKey, int rowId) {
        this.tableKey = tableKey;
        this.rowId = rowId;
    }

    public static DeleteRow of(String tableKey, int rowId) {
        return new DeleteRow(tableKey, rowId);
    }

    public String tableKey() {
        return tableKey;
    }

    public int rowId() {
        return rowId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (DeleteRow) obj;
        return Objects.equals(this.tableKey, that.tableKey) &&
               this.rowId == that.rowId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey, rowId);
    }

    @Override
    public String toString() {
        return "DeleteRow[tableKey=%s, rowId=%d]".formatted(tableKey, rowId);
    }


}
