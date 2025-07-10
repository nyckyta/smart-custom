package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.ColumnValue;
import edu.ukma.smart.virtual.values.DecimalValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.ListValue;
import edu.ukma.smart.virtual.values.ReferenceValue;
import edu.ukma.smart.virtual.values.StringValue;
import edu.ukma.smart.virtual.values.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVirtualTableService implements VirtualTableService {

    private static final Logger log = LoggerFactory.getLogger(DefaultVirtualTableService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSX");
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

        try (final var statement = connection.prepareStatement(query.value())) {
            int index = 1;
            for (final var cv : columnValues) {
                switch (cv) {
                    case StringValue s -> statement.setString(index, s.value());
                    case IntegerValue i -> statement.setLong(index, i.value());
                    case BooleanValue b -> statement.setBoolean(index, b.value());
                    case DecimalValue d -> statement.setBigDecimal(index, d.value());
                    case ReferenceValue r -> statement.setInt(index, r.value());
                    default -> {
                        return Optional.of(InputValidationErr.error("Insert: not supported column value"));
                    }
                }
                index += 1;
            }
            statement.execute();
        }

        return Optional.empty();
    }

    @Override
    public Optional<? extends Err> updateRow(UpdateRow updateRow) throws SQLException {
        var query = queryBuilder.updateRow(updateRow);
        if (query.error().isPresent()) {
            return query.error();
        }

        int index = 1;
        try (final var statement = connection.prepareStatement(query.value())) {
            for (var value : updateRow.valuesToUpdate()) {
                switch (value) {
                    case StringValue s -> statement.setString(index, s.value());
                    case IntegerValue i -> statement.setLong(index, i.value());
                    case BooleanValue b -> statement.setBoolean(index, b.value());
                    case DecimalValue d -> statement.setBigDecimal(index, d.value());
                    case ReferenceValue r -> statement.setInt(index, r.value());
                    default -> {
                        return Optional.of(InputValidationErr.error("Insert: not supported column value"));
                    }
                }
                index += 1;
            }
            statement.setInt(index, updateRow.rowId());
            statement.execute();
        }

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

    @Override
    public Return<List<List<ColumnValue<?>>>> select(SelectQuery query) throws SQLException {
        var selectQuery = queryBuilder.select(query);
        if (selectQuery.error().isPresent()) {
            return Return.error(selectQuery.error().get());
        }

        try (final var statement = connection.prepareStatement(selectQuery.value().preparedStatement())) {
            int index = 1;
            for (var v : selectQuery.value().params()) {
                switch (v) {
                    case StringValue s -> statement.setString(index, s.value());
                    case IntegerValue i -> statement.setLong(index, i.value());
                    case BooleanValue b -> statement.setBoolean(index, b.value());
                    case DecimalValue d -> statement.setBigDecimal(index, d.value());
                    case ReferenceValue r -> statement.setInt(index, r.value());
                    case ListValue<?> l -> index = setListValue(statement, l, index);
                    default -> {
                        return Return.error(InputValidationErr.error("Insert: not supported column value"));
                    }
                }
                index += 1;
            }

            var resultSet = statement.executeQuery();
            List<List<ColumnValue<?>>> queryResult = new ArrayList<>();
            while (resultSet.next()) {
                List<ColumnValue<?>> rowResult = new ArrayList<>(query.columnKeysToReturn().size());
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i += 1) {
                    String name = resultSet.getMetaData().getColumnName(i);
                    var value = switch (resultSet.getMetaData().getColumnType(i)) {
                        case Types.BIGINT, Types.INTEGER -> IntegerValue.of(name, resultSet.getLong(i));
                        case Types.TIMESTAMP -> IntegerValue.of(name, dateFormat.parse(resultSet.getString(i)).getTime());
                        case Types.BOOLEAN, Types.BIT -> BooleanValue.of(name, resultSet.getObject(i, Boolean.class));
                        case Types.DECIMAL, Types.NUMERIC -> DecimalValue.of(name, resultSet.getBigDecimal(i));
                        case Types.VARCHAR -> StringValue.of(name, resultSet.getString(i));
                        default -> throw new IllegalStateException(
                            "Unsupported column type: " + resultSet.getMetaData().getColumnType(i)
                        );

                    };

                    rowResult.add(value);
                }
                queryResult.add(rowResult);
            }

            return Return.of(queryResult);
        } catch (ParseException e) {
            log.error("Failed to parse timestamp", e);
            throw new IllegalStateException("Failure during select. Unexpected time value.");
        }
    }

    private Integer setListValue(PreparedStatement statement, ListValue<?> l, int index) throws SQLException {
        int _index = index - 1;
        switch (l.type()) {
            case STRING -> {
                for (var v : l.value()) {
                    _index += 1;
                    statement.setString(_index, (String) v);
                }
            }
            case INTEGER -> {
                for (var v : l.value()) {
                    _index += 1;
                    statement.setLong(_index, (Long) v);
                }
            }
            case BOOLEAN -> {
                for (var v : l.value()) {
                    _index += 1;
                    statement.setBoolean(_index, (Boolean) v);
                }
            }
            case DECIMAL -> {
                for (var v : l.value()) {
                    _index += 1;
                    statement.setBigDecimal(_index, (BigDecimal) v);
                }
            }
            case REFERENCE -> {
                for (var v : l.value()) {
                    _index += 1;
                    statement.setInt(_index, (Integer) v);
                }
            }
        }
        ;

        return _index;
    }

    private String resolveArrayType(Type type) {
        return switch (type) {
            case STRING -> "VARCHAR";
            case INTEGER -> "BIGINT";
            case BOOLEAN -> "BOOLEAN";
            case DECIMAL -> "DECIMAL";
            case REFERENCE -> "INTEGER";
        };
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

