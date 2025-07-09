package edu.ukma.smart.virtual.select;

import java.util.List;
import java.util.Objects;

public record SelectQuery(
    String tableKey,
    List<String> columnKeysToReturn,
    Predicate predicate
) {

    public SelectQuery(String tableKey, List<String> columnKeysToReturn, Predicate predicate) {
        this.tableKey = Objects.requireNonNull(tableKey);
        this.columnKeysToReturn = Objects.requireNonNull(columnKeysToReturn);
        this.predicate = predicate;
    }

    public static SelectQuery of(
        String tableKey,
        List<String> columnsToReturn
    ) {
        return new SelectQuery(
            Objects.requireNonNull(tableKey),
            Objects.requireNonNull(columnsToReturn),
            null
        );
    }

    public static SelectQuery of(
        String tableKey,
        List<String> columnsToReturn,
        Predicate predicate
    ) {
        return new SelectQuery(
            Objects.requireNonNull(tableKey),
            Objects.requireNonNull(columnsToReturn),
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
