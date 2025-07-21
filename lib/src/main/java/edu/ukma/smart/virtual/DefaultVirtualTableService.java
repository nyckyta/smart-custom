package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.ADD_ROW_LISTS_ARE_NOT_SUPPORTED;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.UPDATE_ROW_LISTS_VALUES_ARE_NOT_SUPPORTED;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.BooleanValue;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.dml.values.DecimalValue;
import edu.ukma.smart.virtual.dml.values.IntegerValue;
import edu.ukma.smart.virtual.dml.values.ListValue;
import edu.ukma.smart.virtual.dml.values.ReferenceValue;
import edu.ukma.smart.virtual.dml.values.StringValue;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.Return;
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
    private final PostgreSQLExceptionHandler exceptionHandler = new PostgreSQLExceptionHandler();

    public DefaultVirtualTableService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<? extends Err> createTable(NewTable newTable) {
        final var query = queryBuilder.createTable(newTable);
        if (query.error().isPresent()) {
            return query.error();
        }

        return executeStatement(query.value());
    }

    @Override
    public Optional<? extends Err> dropTable(DropTable deleteTable) {
        final var query = queryBuilder.dropTable(deleteTable);
        if (query.error().isPresent()) {
            return query.error();
        }

        return executeStatement(query.value());
    }

    @Override
    public Optional<? extends Err> addProperty(AddProperty addProperty) {
        final var query = queryBuilder.addProperty(addProperty);
        if (query.error().isPresent()) {
            return query.error();
        }

        return executeStatement(query.value());
    }

    @Override
    public Optional<? extends Err> dropProperty(DropProperty dropProperty) {
        final var query = queryBuilder.dropProperty(dropProperty);
        if (query.error().isPresent()) {
            return query.error();
        }

        return executeStatement(query.value());
    }

    @Override
    public Optional<? extends Err> addRow(InsertRow insertRow) {
        var query = queryBuilder.insertIntoTable(insertRow);
        if (query.error().isPresent()) {
            return query.error();
        }

        try (final var statement = connection.prepareStatement(query.value())) {
            int index = 1;
            for (final var cv : insertRow.columnValues()) {
                switch (cv.type()) {
                    case STRING -> statement.setString(index, ((StringValue) cv).value());
                    case INTEGER -> statement.setLong(index, ((IntegerValue) cv).value());
                    case BOOLEAN -> statement.setBoolean(index, ((BooleanValue) cv).value());
                    case DECIMAL -> statement.setBigDecimal(index, ((DecimalValue) cv).value());
                    case REFERENCE -> statement.setInt(index, ((ReferenceValue) cv).value());
                    case LIST -> {
                        return Optional.of(InputValidationErr.of(ADD_ROW_LISTS_ARE_NOT_SUPPORTED));
                    }
                    case null -> throw new NullPointerException();
                }
                index += 1;
            }
            statement.execute();
        } catch (final SQLException e) {
            return Optional.of(exceptionHandler.handle(e));
        }

        return Optional.empty();
    }

    @Override
    public Optional<? extends Err> updateRow(UpdateRow updateRow) {
        var query = queryBuilder.updateRow(updateRow);
        if (query.error().isPresent()) {
            return query.error();
        }

        int index = 1;
        try (final var statement = connection.prepareStatement(query.value())) {
            for (var value : updateRow.valuesToUpdate()) {
                switch (value.type()) {
                    case STRING -> statement.setString(index, ((StringValue) value).value());
                    case INTEGER -> statement.setLong(index, ((IntegerValue) value).value());
                    case BOOLEAN -> statement.setBoolean(index, ((BooleanValue) value).value());
                    case DECIMAL -> statement.setBigDecimal(index, ((DecimalValue) value).value());
                    case REFERENCE -> statement.setInt(index, ((ReferenceValue) value).value());
                    case LIST -> {
                        return Optional.of(InputValidationErr.of(UPDATE_ROW_LISTS_VALUES_ARE_NOT_SUPPORTED));
                    }
                }
                index += 1;
            }
            statement.setInt(index, updateRow.rowId());
            statement.execute();
        } catch (final SQLException e) {
            return Optional.of(exceptionHandler.handle(e));
        }

        return Optional.empty();
    }

    @Override
    public Optional<? extends Err> deleteRow(DeleteRow deleteRow) {
        var query = queryBuilder.deleteFromTable(deleteRow);
        if (query.error().isPresent()) {
            return query.error();
        }

        return executeStatement(query.value());
    }

    @Override
    public Return<List<List<ColumnValue<?>>>> select(SelectQuery query) {
        var selectQuery = queryBuilder.select(query);
        if (selectQuery.error().isPresent()) {
            return Return.error(selectQuery.error().get());
        }

        try (final var statement = connection.prepareStatement(selectQuery.value().preparedStatement())) {
            int index = 1;
            for (var v : selectQuery.value().params()) {
                switch (v.type()) {
                    case STRING -> statement.setString(index, ((StringValue) v).value());
                    case INTEGER -> statement.setLong(index, ((IntegerValue) v).value());
                    case BOOLEAN -> statement.setBoolean(index, ((BooleanValue) v).value());
                    case DECIMAL -> statement.setBigDecimal(index, ((DecimalValue) v).value());
                    case REFERENCE -> statement.setInt(index, ((ReferenceValue) v).value());
                    case LIST -> index = setListValue(statement, (ListValue<?>) v, index);
                    case null -> throw new NullPointerException();
                }
                index += 1;
            }

            var resultSet = statement.executeQuery();
            List<List<ColumnValue<?>>> queryResult = new ArrayList<>();
            while (resultSet.next()) {
                List<ColumnValue<?>> rowResult = new ArrayList<>(query.propertyKeysToReturn().size());
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
        } catch (SQLException e) {
            return Return.error(exceptionHandler.handle(e));
        }
    }

    private Integer setListValue(PreparedStatement statement, ListValue<?> l, int index) throws SQLException {
        int _index = index - 1;
        switch (l.elementsType()) {
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

        return _index;
    }

    private Optional<Err> executeStatement(String query) {
        try {
            connection.beginRequest();
            try (var statement = connection.createStatement()) {
                log.debug("Executing SQL statement to add row: {}", query);
                statement.execute(query);
            } finally {
                connection.endRequest();
            }
        } catch (SQLException e) {
            log.debug("Error executing SQL statement", e);
            return Optional.of(exceptionHandler.handle(e));
        }

        return Optional.empty();
    }
}

