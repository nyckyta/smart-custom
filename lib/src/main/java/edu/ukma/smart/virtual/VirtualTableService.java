package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.Return;
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
    Optional<? extends Err> createTable(NewTable newTable) throws SQLException;

    Optional<? extends Err> dropTable(DropTable tableKey) throws SQLException;

    Optional<? extends Err> addRow(InsertRow insertRow) throws SQLException;

    default Optional<? extends Err> addRow(String tableKey, List<? extends ColumnValue<?>> columnValues) throws SQLException {
        return addRow(InsertRow.of(tableKey, columnValues));
    }

    Optional<? extends Err> updateRow(UpdateRow updateRow) throws SQLException;

    Optional<? extends Err> deleteRow(DeleteRow deleteRow) throws SQLException;

    Return<List<List<ColumnValue<?>>>> select(SelectQuery query) throws SQLException;
}
