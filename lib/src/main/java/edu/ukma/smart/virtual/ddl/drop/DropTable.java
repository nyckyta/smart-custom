package edu.ukma.smart.virtual.ddl.drop;

import java.util.Objects;

public final class DropTable {
    private final String tableKey;

    private DropTable(String tableKey) {
        this.tableKey = tableKey;
    }

    public static DropTable of(String tableKey) {
        return new DropTable(tableKey);
    }

    public String tableKey() {
        return tableKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (DropTable) obj;
        return Objects.equals(this.tableKey, that.tableKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey);
    }

    @Override
    public String toString() {
        return "DropTable[tableKey=%s]".formatted(tableKey);
    }

}
