package edu.ukma.smart.virtual.delete;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record DeleteTable(String tableKey) implements Validated {

    public static DeleteTable of(String tableKey) {
        return new DeleteTable(tableKey);
    }

    @Override
    public Optional<InputValidationErr> validate() {
        if (tableKey == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
