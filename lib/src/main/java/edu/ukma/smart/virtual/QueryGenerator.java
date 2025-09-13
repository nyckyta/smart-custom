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

    /**
     * Returns query that should be used in prepared statement with single parameter for table key.
     * Result set signature must be
     * property_key(string),
     * name_description_json(string),
     * isNullable(bool),
     * typename,
     * precision_of_numeric_type(int),
     * scale_of_numeric_type(int),
     * constr_id(int),
     * constr_def(string).
     *
     * <p>Note, that property records may be repeated.
     */
    Return<String> getProperties(String tableKey);

    Return<String> addProperty(AddProperty addProperty);

    Return<String> dropProperty(DropProperty dropProperty);

    Return<String> insertIntoTable(InsertRow insertRow);

    Return<SelectStatement> select(SelectQuery selectQuery);

    Return<String> updateRow(UpdateRow updateRow);

    Return<String> deleteFromTable(DeleteRow deleteRow);

    Return<String> foreignKeyTableReferences();
}
