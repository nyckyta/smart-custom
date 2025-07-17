package edu.ukma.smart.virtual.delete;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record DeleteRow(String tableKey, int rowId) implements Validated {

    public static DeleteRow of(String tableKey, int rowId) {
        return new DeleteRow(tableKey, rowId);
    }

    @Override
    public Optional<InputValidationErr> validate() {
        if (tableKey == null) {
            return Optional.of(InputValidationErr.of(InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
