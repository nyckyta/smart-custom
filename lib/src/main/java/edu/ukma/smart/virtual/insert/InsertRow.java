package edu.ukma.smart.virtual.insert;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.List;
import java.util.Optional;

public record InsertRow(String tableKey, List<? extends ColumnValue<?>> columnValues) implements Validated {

    public InsertRow(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        this.tableKey = tableKey;
        this.columnValues = columnValues == null ? List.of() : columnValues;
    }

    public static InsertRow of(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        return new InsertRow(tableKey, columnValues);
    }

    @Override
    public Optional<InputValidationErr> validate() {
        if (tableKey == null || tableKey.isBlank()) {
            return Optional.of(InputValidationErr.of(InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
