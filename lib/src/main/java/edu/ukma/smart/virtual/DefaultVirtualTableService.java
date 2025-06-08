package edu.ukma.smart.virtual;

import java.sql.Connection;
import java.sql.SQLException;

import edu.ukma.smart.virtual.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVirtualTableService implements VirtualTableService {

    private final static Logger log = LoggerFactory.getLogger(DefaultVirtualTableService.class);

    private final Connection connection;

    public DefaultVirtualTableService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable(NewTable newTable) throws SQLException {
        connection.beginRequest();

        try (var statement = connection.createStatement()) {
            var statementBuilder = new StringBuilder();
            statementBuilder
                // TODO: figure out how to add timestamp on update
                // _updated TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                .append(
                    """
                        CREATE TABLE public.%s (
                            _id SERIAL PRIMARY KEY NOT NULL,
                            _created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                        """.formatted(newTable.key())
                );

            for (var property : newTable.properties()) {
                switch (property) {
                    case StringProperty s -> statementBuilder.append(
                        ",%s %s DEFAULT '%s' %s %s\n".formatted(
                            s.key(),
                            "VARCHAR(255)", // TODO: make this configurable
                            s.defaultValue() == null ? "NULL" : s.defaultValue(),
                            s.isRequired() ? "NOT NULL" : "",
                            s.isUnique() ? "UNIQUE" : ""
                        )
                    );
                    default -> throw new IllegalStateException("Unexpected value: " + property);
                }

            }

            statementBuilder.append(");");
            log.debug("Executing SQL statement to create table:\n {}", statementBuilder);
            statement.execute(statementBuilder.toString());
        }

        connection.endRequest();
    }

    @Override
    public void deleteTable(String tableKey) throws SQLException {
        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            // TODO: escape string
            log.info("Executing SQL statement to delete table:\n {}", statement);
            statement.execute("DROP TABLE %s;".formatted(tableKey));
        }

        connection.endRequest();
    }
}
