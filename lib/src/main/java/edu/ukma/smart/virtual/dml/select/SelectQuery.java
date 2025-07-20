package edu.ukma.smart.virtual.dml.select;

import java.util.Collections;
import java.util.List;

public record SelectQuery(
    String tableKey,
    List<SelectProperty> propertyKeysToReturn,
    Predicate predicate
) {

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
}
