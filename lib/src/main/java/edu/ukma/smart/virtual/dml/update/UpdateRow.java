package edu.ukma.smart.virtual.dml.update;

import edu.ukma.smart.virtual.dml.values.ColumnValue;
import java.util.List;
import java.util.Objects;

public final class UpdateRow {
    private final String tableKey;
    private final int rowId;
    private final List<ColumnValue<?>> valuesToUpdate;

    private UpdateRow(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        this.tableKey = tableKey;
        this.rowId = rowId;
        this.valuesToUpdate = valuesToUpdate;
    }

    public static UpdateRow of(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        return new UpdateRow(tableKey, rowId, valuesToUpdate);
    }

    public String tableKey() {
        return tableKey;
    }

    public int rowId() {
        return rowId;
    }

    public List<ColumnValue<?>> valuesToUpdate() {
        return valuesToUpdate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UpdateRow) obj;
        return Objects.equals(this.tableKey, that.tableKey)
               && this.rowId == that.rowId
               && Objects.equals(this.valuesToUpdate, that.valuesToUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey, rowId, valuesToUpdate);
    }

    @Override
    public String toString() {
        return "UpdateRow[tableKey=%s, rowId=%d, valuesToUpdate=%s]".formatted(tableKey, rowId, valuesToUpdate);
    }

}
