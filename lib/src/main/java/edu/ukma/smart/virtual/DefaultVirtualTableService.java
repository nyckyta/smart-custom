package edu.ukma.smart.virtual;

import java.sql.Connection;
import java.sql.SQLException;

public class DefaultVirtualTableService implements VirtualTableService {

    private Connection connection;

    public DefaultVirtualTableService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable(NewTable newTable) throws SQLException {
        connection.beginRequest();

        try (var statement = connection.createStatement()) {
            var statementBuilder = new StringBuilder();
            statementBuilder
                .append(
                    """
                        CREATE TABLE %s (
                            _id SERIAL PRIMARY KEY NOT NULL,
                            _created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            _updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n
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
            statement.execute(statementBuilder.toString());
        }

        connection.endRequest();
    }

    @Override
    public void deleteTable(String tableKey) throws SQLException {
        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            // TODO: escape string
            statement.execute("DROP TABLE IF EXISTS %s;".formatted(tableKey));
        }

        connection.endRequest();
    }
}
