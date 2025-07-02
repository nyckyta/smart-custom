package edu.ukma.smart.virtual;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.properties.BooleanProperty;
import edu.ukma.smart.virtual.properties.IntegerProperty;
import edu.ukma.smart.virtual.properties.StringProperty;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.ColumnValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVirtualTableService implements VirtualTableService {

    private final static Logger log = LoggerFactory.getLogger(DefaultVirtualTableService.class);
    private final static Pattern KEY_REGEXP = Pattern.compile("^[a-z][a-z_]{1,100}$");
    private final Connection connection;

    public DefaultVirtualTableService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<? extends Err> createTable(NewTable newTable) throws SQLException {
        if (!KEY_REGEXP.matcher(newTable.key()).matches()) {
            log.error("Create table: Table key '{}' does not match the required pattern '{}'", newTable.key(), KEY_REGEXP);
            return Optional.of(
                InputValidationErr.error("Table key %s should consist only lower case english and '_'".formatted(newTable.key()))
            );
        }

        var statementBuilder = new StringBuilder()
            // TODO: figure out how to add timestamp on update
            // _updated TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            .append(
                """
                    CREATE TABLE public.%s (
                        _id SERIAL PRIMARY KEY NOT NULL,
                        _created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                    """.formatted(newTable.key())
            );

        if (newTable.properties().isEmpty()) {
            log.error("Create table: Table '{}' has no properties defined", newTable.key());
            return Optional.of(
                InputValidationErr.error("Table %s should have at least one property".formatted(newTable.key()))
            );
        }

        var checks = new ArrayList<String>();
        for (var property : newTable.properties()) {
            if (!KEY_REGEXP.matcher(property.key()).matches()) {
                log.error("Create table: Property key '{}' does not match the required pattern '{}'", newTable.key(), KEY_REGEXP);
                return Optional.of(
                    InputValidationErr.error(
                        "Property key %s should consist only lower case english and '_' up to 100 chars".formatted(newTable.key())
                    )
                );
            }

            var err = switch (property) {
                case StringProperty s -> addStringColumn(s, statementBuilder, checks);
                case IntegerProperty i -> addIntegerColumn(i, statementBuilder, checks);
                case BooleanProperty b -> addBooleanColumn(b, statementBuilder);
                default -> throw new IllegalStateException("Unexpected value: " + property);
            };

            if (err.isPresent()) {
                return err;
            }
        }

        checks.forEach(c -> statementBuilder.append(",%s".formatted(c)));
        statementBuilder.append(");");

        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            log.debug("Executing SQL statement to create table:\n {}", statementBuilder);
            statement.execute(statementBuilder.toString());
        } finally {
            connection.endRequest();
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> addIntegerColumn(IntegerProperty i, StringBuilder statementBuilder, List<String> checks) {
        if (i.isRequired() && i.defaultValue() == null) {
            return Optional.of(InputValidationErr.error("Create table: Property key '%s' is required, default value is null".formatted(i.key())));
        }
        statementBuilder.append(
            ",%s BIGINT DEFAULT %s %s %s\n".formatted(
                i.key(),
                i.defaultValue() == null ? "NULL" : i.defaultValue(),
                i.isRequired() ? "NOT NULL" : "",
                i.isUnique() ? "UNIQUE" : ""
            )
        );

        if (i.max() != null && i.min() != null) {
            if (i.max() < i.min()) {
                return Optional.of(
                    InputValidationErr.error("Create table: Property key '%s' max can not be less than min".formatted(i.key()))
                );
            }
            checks.add("CHECK (%s BETWEEN %d AND %d)".formatted(i.key(), i.min(), i.max()));
            return Optional.empty();
        }

        if (i.max() != null) {
            checks.add("CHECK (%s <= %d)".formatted(i.key(), i.max()));
            return Optional.empty();
        }

        if (i.min() != null) {
            checks.add("CHECK (%s >= %d)".formatted(i.key(), i.min()));
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> addStringColumn(StringProperty s, StringBuilder statementBuilder, List<String> checks) {
        if (s.isRequired() && s.defaultValue() == null) {
            return Optional.of(InputValidationErr.error("Create table: Property key '%s' is required, default value is null".formatted(s.key())));
        }
        statementBuilder.append(
        ",%s TEXT DEFAULT %s %s %s\n".formatted(
            s.key(),
            s.defaultValue() == null ? "NULL" : "$$" + s.defaultValue() + "$$",
            s.isRequired() ? "NOT NULL" : "",
            s.isUnique() ? "UNIQUE" : "")
        );

        if (s.maxLength() != null && s.minLength() != null) {
            if (s.maxLength() < 1) {
                return Optional.of(
                    InputValidationErr.error("Create table: Property key '%s' max length can not be less than one".formatted(s.key()))
                );
            }

            if (s.minLength() < 1) {
                return Optional.of(
                    InputValidationErr.error("Create table: Property key '%s' min length can not be less than one".formatted(s.key()))
                );
            }

            if (s.maxLength() < s.minLength()) {
                return Optional.of(
                    InputValidationErr.error("Create table: Property key '%s' max length can not be less than min length".formatted(s.key()))
                );
            }
            checks.add("CHECK (char_length(%s) BETWEEN %d AND %d)".formatted(s.key(), s.minLength(), s.maxLength()));
            return Optional.empty();
        }

        if (s.maxLength() != null) {
            if (s.maxLength() < 1) {
                return Optional.of(
                    InputValidationErr.error("Create table: Property key '%s' max length can not be less than one".formatted(s.key()))
                );
            }

            checks.add("CHECK (char_length(%s) <= %d)".formatted(s.key(), s.maxLength()));
            return Optional.empty();
        }

        if (s.minLength() != null) {
            if (s.minLength() < 1) {
                return Optional.of(
                    InputValidationErr.error("Create table: Property key '%s' min length can not be less than one".formatted(s.key()))
                );
            }

            checks.add("CHECK (char_length(%s) >= %d)".formatted(s.key(), s.minLength()));
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> addBooleanColumn(BooleanProperty b, StringBuilder statementBuilder) {
        if (b.isRequired() && b.defaultValue() == null) {
            return Optional.of(
                InputValidationErr.error("Create table: Property key '%s' is required, default value is null".formatted(b.key())));
        }
        statementBuilder.append(
            ",%s BOOLEAN DEFAULT %s %s %s\n".formatted(
                b.key(),
                b.defaultValue() == null ? "NULL" : b.defaultValue(),
                b.isRequired() ? "NOT NULL" : "",
                b.isUnique() ? "UNIQUE" : ""
            )
        );

        return Optional.empty();
    }



    @Override
    public Optional<? extends Err> deleteTable(String tableKey) throws SQLException {
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Drop: Table key '{}' does not match the required pattern '{}'", tableKey, KEY_REGEXP);
            return Optional.of(
                InputValidationErr.error("Table key %s should consist only lower case english and '_'".formatted(tableKey))
            );
        }
        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            log.info("Executing SQL statement to delete table:\n {}", statement);
            statement.execute("DROP TABLE %s;".formatted(tableKey));
        } finally {
            connection.endRequest();
        }

        return Optional.empty();
    }

    @Override
    // TODO: error handling for sql exceptions
    public Optional<? extends Err> addRow(String tableKey, List<? extends ColumnValue> columnValues) throws SQLException {
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Add row: Table key '{}' does not match the required pattern '{}'", tableKey, KEY_REGEXP);
            return Optional.of(
                InputValidationErr.error("Wrong table key %s".formatted(tableKey))
            );
        }

        if (columnValues.isEmpty()) {
            log.error("Add row: No column values provided for table '{}'", tableKey);
            return Optional.of(
                InputValidationErr.error("At least one column value is required to add a row to table %s".formatted(tableKey))
            );
        }

        var columnsPart = new StringBuilder("(");
        var valuesPart = new StringBuilder("VALUES (");
        for (var column : columnValues) {
            if (!KEY_REGEXP.matcher(column.key()).matches()) {
                log.error("Add row: Table key '{}' does not match the required pattern '{}'", tableKey, KEY_REGEXP);
                return Optional.of(
                    InputValidationErr.error("Wrong table key %s".formatted(tableKey))
                );
            }

            columnsPart.append(column.key()).append(",");
            switch (column) {
                case StringValue s -> valuesPart.append("$$").append(s.value()).append("$$").append(",");
                case IntegerValue i -> valuesPart.append(i.value()).append(",");
                case BooleanValue b -> valuesPart.append(b.value()).append(",");
                default -> throw new IllegalStateException("Unexpected value: " + column);
            }
        }
        // Remove last comma
        columnsPart.setLength(columnsPart.length() - 1);
        valuesPart.setLength(valuesPart.length() - 1);

        columnsPart.append(")");
        valuesPart.append(")");

        var query = """
            INSERT INTO %s %s %s;
            """.formatted(tableKey, columnsPart, valuesPart);

        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            log.debug("Executing SQL statement to add row:\n {}", query);
            statement.execute(query);
        } finally {
            connection.endRequest();
        }

        return Optional.empty();
    }

    @Override
    public Optional<? extends Err> deleteRow(String tableKey, long rowId) throws SQLException {
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Delete row: Table key '{}' does not match the required pattern '{}'", tableKey, KEY_REGEXP);
            return Optional.of(
                InputValidationErr.error("Wrong table key %s".formatted(tableKey))
            );
        }

        connection.beginRequest();
        try (var statement = connection.createStatement()) {
            var query = "DELETE FROM %s WHERE _id = %d;".formatted(tableKey, rowId);
            log.debug("Executing SQL statement to add row: {}", query);
            statement.execute(query);
        } finally {
            connection.endRequest();
        }

        return Optional.empty();
    }
}
