package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.create.Property.KEY_REGEXP;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_TABLE_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record SelectQuery(
    String tableKey,
    List<SelectProperty> propertyKeysToReturn,
    Predicate predicate
) implements Validated {

    public SelectQuery(String tableKey, List<SelectProperty> propertyKeysToReturn, Predicate predicate) {
        this.tableKey = tableKey;
        this.propertyKeysToReturn = propertyKeysToReturn == null ? List.of() : Collections.unmodifiableList(propertyKeysToReturn);
        this.predicate = predicate;
    }

    public static SelectQuery of(
        String tableKey,
        List<SelectProperty> columnsToReturn
    ) {
        return new SelectQuery(
            tableKey,
            columnsToReturn,
            null
        );
    }

    public static SelectQuery of(
        String tableKey,
        List<SelectProperty> columnsToReturn,
        Predicate predicate
    ) {
        return new SelectQuery(
            tableKey,
            columnsToReturn,
            predicate
        );
    }

    public static SelectQuery wildcard(String tableKey) {
        return new SelectQuery(tableKey, List.of(), null);
    }

    public static SelectQuery wildcard(String tableKey, Predicate predicate) {
        return new SelectQuery(tableKey, List.of(), predicate);
    }

    @Override
    public Optional<Err> validate() {
        if (tableKey == null) {
            return Optional.of(InputValidationErr.error(EMPTY_TABLE_KEY));
        }

        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            return Optional.of(InputValidationErr.error(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
