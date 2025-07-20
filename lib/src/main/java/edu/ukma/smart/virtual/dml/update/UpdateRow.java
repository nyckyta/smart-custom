package edu.ukma.smart.virtual.dml.update;

import edu.ukma.smart.virtual.dml.values.ColumnValue;
import java.util.List;

public record UpdateRow(
    String tableKey,
    int rowId,
    List<ColumnValue<?>> valuesToUpdate
) {
    public UpdateRow(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        this.tableKey = tableKey;
        this.rowId = rowId;
        this.valuesToUpdate = valuesToUpdate;
    }

    public static UpdateRow of(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        return new UpdateRow(tableKey, rowId, valuesToUpdate);
    }
}
