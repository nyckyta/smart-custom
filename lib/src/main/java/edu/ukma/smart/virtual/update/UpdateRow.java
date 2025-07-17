package edu.ukma.smart.virtual.update;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.UPDATE_ROW_NO_PROPERTIES;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.List;
import java.util.Optional;

public record UpdateRow(
    String tableKey,
    int rowId,
    List<ColumnValue<?>> valuesToUpdate
) implements Validated {
    public UpdateRow(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        this.tableKey = tableKey;
        this.rowId = rowId;
        this.valuesToUpdate = valuesToUpdate;
    }

    public static UpdateRow of(String tableKey, int rowId, List<ColumnValue<?>> valuesToUpdate) {
        return new UpdateRow(tableKey, rowId, valuesToUpdate);
    }

    @Override
    public Optional<InputValidationErr> validate() {
        if (tableKey == null || tableKey.isEmpty()) {
            return Optional.of(new InputValidationErr(WRONG_TABLE_KEY_FORMAT));
        }

        if (valuesToUpdate == null || valuesToUpdate.isEmpty()) {
            return Optional.of(new InputValidationErr(UPDATE_ROW_NO_PROPERTIES));
        }
        return Optional.empty();
    }
}
