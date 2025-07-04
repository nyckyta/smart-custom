package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.List;

interface QueryBuilder {

    Return<String> createTable(NewTable newTable);

    Return<String> deleteTable(String tableKey);

    Return<String> insertIntoTable(String tableKey, List<? extends ColumnValue> columnValues);

    Return<String> deleteFromTable(String tableKey, int rowId);
    // TODO: extend
}
