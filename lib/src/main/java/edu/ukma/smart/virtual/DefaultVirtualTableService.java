package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.values.ColumnValue;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVirtualTableService implements VirtualTableService {

    private static final Logger log = LoggerFactory.getLogger(DefaultVirtualTableService.class);
    private final Connection connection;
    private final QueryGenerator queryBuilder = new PostgreQueryGenerator();

    public DefaultVirtualTableService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<? extends Err> createTable(NewTable newTable) throws SQLException {

        final var query = queryBuilder.createTable(newTable);
        if (query.error().isPresent()) {
            return query.error();
        }

        executeStatement(query.value());
        return Optional.empty();
    }

    @Override
    public Optional<? extends Err> deleteTable(String tableKey) throws SQLException {
        final var query = queryBuilder.deleteTable(tableKey);
        if (query.error().isPresent()) {
            return query.error();
        }

        executeStatement(query.value());

        return Optional.empty();
    }

    @Override
    // TODO: error handling for sql exceptions
    public Optional<? extends Err> addRow(String tableKey, List<? extends ColumnValue<?>> columnValues)
        throws SQLException {
        var query = queryBuilder.insertIntoTable(tableKey, columnValues);
        if (query.error().isPresent()) {
            return query.error();
        }

        executeStatement(query.value());
        return Optional.empty();
    }

    @Override
    public Optional<? extends Err> deleteRow(String tableKey, int rowId) throws SQLException {
        var query = queryBuilder.deleteFromTable(tableKey, rowId);
        if (query.error().isPresent()) {
            return query.error();
        }

        executeStatement(query.value());
        return Optional.empty();
    }

    private void executeStatement(String query) throws SQLException {
        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            log.debug("Executing SQL statement to add row: {}", query);
            statement.execute(query);
        } finally {
            connection.endRequest();
        }
    }
}
