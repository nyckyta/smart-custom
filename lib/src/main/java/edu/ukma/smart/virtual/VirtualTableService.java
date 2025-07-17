package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.create.NewTable;
import edu.ukma.smart.virtual.delete.DeleteRow;
import edu.ukma.smart.virtual.delete.DeleteTable;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.insert.InsertRow;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.update.UpdateRow;
import edu.ukma.smart.virtual.values.ColumnValue;
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

    Optional<? extends Err> deleteTable(DeleteTable tableKey) throws SQLException;

    Optional<? extends Err> addRow(InsertRow insertRow) throws SQLException;

    default Optional<? extends Err> addRow(String tableKey, List<? extends ColumnValue<?>> columnValues) throws SQLException {
        return addRow(InsertRow.of(tableKey, columnValues));
    }

    Optional<? extends Err> updateRow(UpdateRow updateRow) throws SQLException;

    Optional<? extends Err> deleteRow(DeleteRow deleteRow) throws SQLException;

    Return<List<List<ColumnValue<?>>>> select(SelectQuery query) throws SQLException;
}
