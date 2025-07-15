package edu.ukma.smart.virtual.delete;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_ROW_ID_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record DeleteRow(String tableKey, int rowId) implements Validated {

    public static DeleteRow of(String tableKey, int rowId) {
        return new DeleteRow(tableKey, rowId);
    }

    @Override
    public Optional<Err> validate() {
        if (tableKey == null) {
            return Optional.of(InputValidationErr.error(InputValidationErr.ErrorCode.EMPTY_TABLE_KEY));
        }

        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            return Optional.of(InputValidationErr.error(InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT));
        }

        if (rowId < 1) {
            return Optional.of(InputValidationErr.error(WRONG_ROW_ID_FORMAT));
        }

        return Optional.empty();
    }
}
