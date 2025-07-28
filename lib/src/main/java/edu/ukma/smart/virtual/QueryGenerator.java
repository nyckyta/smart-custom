package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.errors.Return;

interface QueryGenerator {

    Return<String> createTable(NewTable newTable);

    Return<String> dropTable(DropTable tableKey);

    Return<String> getTables();

    Return<String> addProperty(AddProperty addProperty);

    Return<String> dropProperty(DropProperty dropProperty);

    Return<String> insertIntoTable(InsertRow insertRow);

    Return<SelectStatement> select(SelectQuery selectQuery);

    Return<String> updateRow(UpdateRow updateRow);

    Return<String> deleteFromTable(DeleteRow deleteRow);
    // TODO: extend
}
