package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record UpdateRow(
    String tableKey,
    int rowId,
    List<ColumnValue<?>> valuesToUpdate
) {
    public UpdateRow(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        this.tableKey = Objects.requireNonNull(tableKey);
        this.rowId = rowId;
        this.valuesToUpdate = Collections.unmodifiableList(valuesToUpdate);
    }

    public static UpdateRow of(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        return new UpdateRow(tableKey, rowId, valuesToUpdate);
    }
}
