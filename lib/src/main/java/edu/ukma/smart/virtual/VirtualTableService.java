package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.metadata.Table;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface VirtualTableService {

    /**
     * Creates a new virtual table.
     *
     * @param newTable the details of the new table to create
     * @return the created table
     */
    Optional<? extends Err> createTable(NewTable newTable);

    Optional<? extends Err> dropTable(DropTable tableKey);

    Return<List<Table>> getTables();

    Optional<? extends Err> addProperty(AddProperty addProperty);

    Optional<? extends Err> dropProperty(DropProperty dropProperty);

    Optional<? extends Err> addRow(InsertRow insertRow);

    default Optional<? extends Err> addRow(String tableKey, List<? extends ColumnValue<?>> columnValues) throws SQLException {
        return addRow(InsertRow.of(tableKey, columnValues));
    }

    Optional<? extends Err> updateRow(UpdateRow updateRow);

    Optional<? extends Err> deleteRow(DeleteRow deleteRow);

    Return<List<List<ColumnValue<?>>>> select(SelectQuery query);
}
