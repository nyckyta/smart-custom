package edu.ukma.smart.virtual;

import java.sql.SQLException;
import java.util.Optional;

import edu.ukma.smart.virtual.errors.Err;

public interface VirtualTableService {

    /**
     * Creates a new virtual table.
     *
     * @param newTable the details of the new table to create
     * @return the created table
     */
    // TODO: return optional error here
    Optional<? extends Err> createTable(NewTable newTable) throws SQLException;

    // TODO: optional error here
    Optional<? extends Err> deleteTable(String tableKey) throws SQLException;
}
