package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.List;
import java.util.Objects;

public record SelectStatement(String preparedStatement, List<ColumnValue<?>> params) {

    public SelectStatement(String preparedStatement, List<ColumnValue<?>> params) {
        this.preparedStatement = Objects.requireNonNull(preparedStatement);
        this.params = Objects.requireNonNull(params);
    }

    public static SelectStatement of(String preparedStatement) {
        return new SelectStatement(preparedStatement, List.of());
    }

    public static SelectStatement of(String preparedStatement, List<ColumnValue<?>> values) {
        return new SelectStatement(preparedStatement, values);
    }

}
