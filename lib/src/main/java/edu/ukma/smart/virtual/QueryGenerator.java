package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.create.NewTable;
import edu.ukma.smart.virtual.delete.DeleteRow;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.update.UpdateRow;
import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.List;

interface QueryGenerator {

    Return<String> createTable(NewTable newTable);

    Return<String> deleteTable(String tableKey);

    Return<String> insertIntoTable(String tableKey, List<? extends ColumnValue<?>> columnValues);

    Return<SelectStatement> select(SelectQuery selectQuery);

    Return<String> updateRow(UpdateRow updateRow);

    Return<String> deleteFromTable(DeleteRow deleteRow);
    // TODO: extend
}
