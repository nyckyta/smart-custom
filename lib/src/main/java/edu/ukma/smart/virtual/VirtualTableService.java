package edu.ukma.smart.virtual;

import java.sql.SQLException;

public interface VirtualTableService {

    /**
     * Creates a new virtual table.
     *
     * @param newTable the details of the new table to create
     * @return the created table
     */
    // TODO: return optional error here
    void createTable(NewTable newTable) throws SQLException;

    // TODO: optional error here
    void deleteTable(String tableKey) throws SQLException;
}
