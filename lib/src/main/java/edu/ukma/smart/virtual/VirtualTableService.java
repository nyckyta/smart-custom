package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.select.SelectQuery;
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

    Optional<? extends Err> deleteTable(String tableKey) throws SQLException;

    Optional<? extends Err> addRow(String tableKey, List<? extends ColumnValue<?>> columnValues)
        throws SQLException;

    Optional<? extends Err> updateRow(UpdateRow updateRow) throws SQLException;

    Optional<? extends Err> deleteRow(String tableKey, int rowId) throws SQLException;

    Return<List<List<ColumnValue<?>>>> select(SelectQuery query) throws SQLException;
}
