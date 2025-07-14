package edu.ukma.smart.virtual.delete;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;

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
            return Optional.of(InputValidationErr.error("Table key must be specified"));
        }

        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            return Optional.of(InputValidationErr.error("Invalid table key format: %s".formatted(tableKey)));
        }

        if (rowId < 1) {
            return Optional.of(InputValidationErr.error("Row id cannot be less than 1"));
        }

        return Optional.empty();
    }
}
