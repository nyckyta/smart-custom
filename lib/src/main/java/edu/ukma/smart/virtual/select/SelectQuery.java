package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.Validated;
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
        this.tableKey = tableKey == null ? "" : tableKey;
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
    public Optional<InputValidationErr> validate() {
        if (tableKey == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
