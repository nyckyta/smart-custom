package edu.ukma.smart.virtual.ddl.drop;

public record DropTable(String tableKey) {

    public static DropTable of(String tableKey) {
        return new DropTable(tableKey);
    }
}
