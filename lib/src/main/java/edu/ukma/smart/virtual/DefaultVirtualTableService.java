package edu.ukma.smart.virtual;

import java.sql.Connection;
import java.sql.SQLException;

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
                // figure out how to add timestamp on update
                // _updated TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                .append(
                    """
                        CREATE TABLE public.%s (
                            _id SERIAL PRIMARY KEY NOT NULL,
                            _created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        """.formatted(newTable.key())
                );

            for (var property : newTable.properties()) {
                statementBuilder.append(
                    ",%s %s DEFAULT %s %s %s\n".formatted(
                        property.name(),
                        property.type().sqlType,
                        property.defaultValue() == null ? "NULL" : property.defaultValue(),
                        property.isRequired() ? "NOT NULL" : "",
                        property.isUnique() ? "UNIQUE" : ""
                    )
                );
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
