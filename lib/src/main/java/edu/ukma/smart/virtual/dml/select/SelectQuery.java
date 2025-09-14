package edu.ukma.smart.virtual.dml.select;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SelectQuery {
    private final String tableKey;
    private final List<SelectProperty> propertyKeysToReturn;
    private final Predicate predicate;

    private SelectQuery(String tableKey, List<SelectProperty> propertyKeysToReturn, Predicate predicate) {
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

    public String tableKey() {
        return tableKey;
    }

    public List<SelectProperty> propertyKeysToReturn() {
        return propertyKeysToReturn;
    }

    public Predicate predicate() {
        return predicate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SelectQuery) obj;
        return Objects.equals(this.tableKey, that.tableKey)
               && Objects.equals(this.propertyKeysToReturn, that.propertyKeysToReturn)
               && Objects.equals(this.predicate, that.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey, propertyKeysToReturn, predicate);
    }

    @Override
    public String toString() {
        return "SelectQuery[tableKey=%s, propertyKeysToReturn=%s, predicate=%s]".formatted(tableKey, propertyKeysToReturn, predicate);
    }

}
