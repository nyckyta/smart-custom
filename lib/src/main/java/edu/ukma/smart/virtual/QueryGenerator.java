package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.create.NewTable;
import edu.ukma.smart.virtual.delete.DeleteRow;
import edu.ukma.smart.virtual.delete.DeleteTable;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.insert.InsertRow;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.update.UpdateRow;

interface QueryGenerator {

    Return<String> createTable(NewTable newTable);

    Return<String> deleteTable(DeleteTable tableKey);

    Return<String> insertIntoTable(InsertRow insertRow);

    Return<SelectStatement> select(SelectQuery selectQuery);

    Return<String> updateRow(UpdateRow updateRow);

    Return<String> deleteFromTable(DeleteRow deleteRow);
    // TODO: extend
}
