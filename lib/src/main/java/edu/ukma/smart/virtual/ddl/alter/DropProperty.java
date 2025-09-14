package edu.ukma.smart.virtual.ddl.alter;

import java.util.Objects;

public final class DropProperty {
    private final String tableKey;
    private final String columnKey;

    private DropProperty(String tableKey, String columnKey) {
        this.tableKey = tableKey;
        this.columnKey = columnKey;
    }

    public static DropProperty of(String tableKey, String columnKey) {
        return new DropProperty(tableKey, columnKey);
    }

    public String tableKey() {
        return tableKey;
    }

    public String columnKey() {
        return columnKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (DropProperty) obj;
        return Objects.equals(this.tableKey, that.tableKey)
               && Objects.equals(this.columnKey, that.columnKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey, columnKey);
    }

    @Override
    public String toString() {
        return "DropProperty[tableKey=%s, columnKey=%s]".formatted(tableKey, columnKey);
    }

}
