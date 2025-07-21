package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.create.BooleanProperty;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.BooleanPredicate;
import edu.ukma.smart.virtual.dml.select.CompoundPredicate;
import edu.ukma.smart.virtual.dml.select.DecimalPredicate;
import edu.ukma.smart.virtual.dml.select.IntegerPredicate;
import edu.ukma.smart.virtual.dml.select.NullablePredicate;
import edu.ukma.smart.virtual.dml.select.RawPredicate;
import edu.ukma.smart.virtual.dml.select.ReferencePredicate;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.select.StringPredicate;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.BooleanValue;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.dml.values.DecimalValue;
import edu.ukma.smart.virtual.dml.values.IntegerValue;
import edu.ukma.smart.virtual.dml.values.ListValue;
import edu.ukma.smart.virtual.dml.values.StringValue;
import edu.ukma.smart.virtual.errors.Return;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PostgreQueryGenerator implements QueryGenerator {

    private static final Logger log = LoggerFactory.getLogger(PostgreQueryGenerator.class);
    private final InputValidator inputValidator = new PostgreInputValidator();

    private static void addIntegerProperty(
        IntegerProperty i,
        StringBuilder statementBuilder,
        List<String> checks
    ) {

        statementBuilder.append(
            "\"%s\" BIGINT %s %s %s%n".formatted(
                i.key(),
                i.defaultValue() == null ? "" : "DEFAULT %s".formatted(i.defaultValue()),
                i.required() ? "NOT NULL" : "",
                i.unique() ? "UNIQUE" : ""
            )
        );

        if (i.max() != null && i.min() != null) {
            checks.add("CHECK (\"%s\" BETWEEN %d AND %d)".formatted(i.key(), i.min(), i.max()));
            return;
        }

        if (i.max() != null) {
            checks.add("CHECK (\"%s\" <= %d)".formatted(i.key(), i.max()));
            return;
        }

        if (i.min() != null) {
            checks.add("CHECK (\"%s\" >= %d)".formatted(i.key(), i.min()));
            return;
        }
    }

    private static void addStringProperty(
        StringProperty s,
        StringBuilder statementBuilder,
        List<String> checks
    ) {
        statementBuilder.append(
            "\"%s\" TEXT DEFAULT %s %s %s%n".formatted(
                s.key(),
                s.defaultValue() == null ? "NULL" : "$$" + s.defaultValue() + "$$",
                s.required() ? "NOT NULL" : "",
                s.unique() ? "UNIQUE" : "")
        );

        if (s.maxLength() != null && s.minLength() != null) {
            checks.add("CHECK (char_length(\"%s\") BETWEEN %d AND %d)".formatted(s.key(), s.minLength(), s.maxLength()));
            return;
        }

        if (s.maxLength() != null) {
            checks.add("CHECK (char_length(\"%s\") <= %d)".formatted(s.key(), s.maxLength()));
            return;
        }

        if (s.minLength() != null) {
            checks.add("CHECK (char_length(\"%s\") >= %d)".formatted(s.key(), s.minLength()));
            return;
        }
    }

    private static void addBooleanProperty(
        BooleanProperty b,
        StringBuilder statementBuilder
    ) {

        statementBuilder.append(
            "\"%s\" BOOLEAN DEFAULT %s %s %s%n".formatted(
                b.key(),
                b.defaultValue() == null ? "NULL" : b.defaultValue(),
                b.required() ? "NOT NULL" : "",
                b.unique() ? "UNIQUE" : ""
            )
        );

    }

    @Override
    public Return<String> createTable(NewTable newTable) {
        var err = inputValidator.validateNewTable(newTable);
        if (err.isPresent()) {
            return Return.error(err.get());
        }
        var query = new StringBuilder()
            // TODO: figure out how to add timestamp on update
            // _updated TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            // TODO: schema check
            .append(
                """
                    CREATE TABLE public.%s (
                        _id SERIAL PRIMARY KEY NOT NULL,
                        _created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                    """.formatted(newTable.key())
            );

        var constraints = new ArrayList<String>();
        for (var property : newTable.properties()) {
            err = inputValidator.validateProperty(property);
            if (err.isPresent()) {
                return Return.error(err.get());
            }

            query.append(",");
            switch (property.type()) {
                case STRING -> addStringProperty((StringProperty) property, query, constraints);
                case INTEGER -> addIntegerProperty((IntegerProperty) property, query, constraints);
                case BOOLEAN -> addBooleanProperty((BooleanProperty) property, query);
                case DECIMAL -> addDecimalProperty((DecimalProperty) property, query, constraints);
                case REFERENCE -> addReferenceProperty((ReferenceProperty) property, query, constraints);
            }
        }

        constraints.forEach(c -> query.append(",%s".formatted(c)));
        query.append(");");

        return Return.of(query.toString());
    }

    @Override
    public Return<String> dropTable(DropTable deleteTable) {
        var err = inputValidator.validateDeleteTable(deleteTable);
        return err.<Return<String>>map(Return::error)
            .orElseGet(() -> Return.of("DROP TABLE %s;".formatted(deleteTable.tableKey())));

    }

    @Override
    public Return<String> addProperty(AddProperty addProperty) {
        var err = inputValidator.validateAddProperty(addProperty);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        var query = new StringBuilder("ALTER TABLE %s ADD COLUMN%n".formatted(addProperty.tableKey()));

        var constraints = new ArrayList<String>();
        var prop = addProperty.property();
        switch (prop.type()) {
            case STRING -> addStringProperty((StringProperty) prop, query, constraints);
            case INTEGER -> addIntegerProperty((IntegerProperty) prop, query, constraints);
            case BOOLEAN -> addBooleanProperty((BooleanProperty) prop, query);
            case DECIMAL -> addDecimalProperty((DecimalProperty) prop, query, constraints);
            case REFERENCE -> addReferenceProperty((ReferenceProperty) prop, query, constraints);
        }

        query.append(" %s;".formatted(String.join(" ", constraints)));
        return Return.of(query.toString());
    }

    @Override
    public Return<String> dropProperty(DropProperty dropProperty) {
        var err = inputValidator.validateDropProperty(dropProperty);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        var query = "ALTER TABLE %s DROP COLUMN %s;".formatted(dropProperty.tableKey(), dropProperty.columnKey());
        return Return.of(query);
    }


    @Override
    public Return<String> updateRow(UpdateRow updateRow) {
        var err = inputValidator.validateUpdateRow(updateRow);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        var queryBuilder = new StringBuilder()
            .append("UPDATE public.%s SET ".formatted(updateRow.tableKey()));
        for (var column : updateRow.valuesToUpdate()) {
            err = inputValidator.validateColumnValue(column);
            if (err.isPresent()) {
                return Return.error(err.get());
            }

            queryBuilder.append("\"%s\"=?,".formatted(column.key()));
        }

        // remove last comma
        queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        queryBuilder.append(" WHERE _id=?;");
        return Return.of(queryBuilder.toString());
    }

    @Override
    public Return<String> insertIntoTable(InsertRow insertRow) {
        var err = inputValidator.validateInsertRow(insertRow);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        if (insertRow.columnValues().isEmpty()) {
            return Return.of("INSERT INTO %s DEFAULT VALUES;".formatted(insertRow.tableKey()));
        }

        var propertiesPart = new StringBuilder("(");
        var valuesPart = new StringBuilder("VALUES (");

        for (var v : insertRow.columnValues()) {
            err = inputValidator.validateColumnValue(v);
            if (err.isPresent()) {
                return Return.error(err.get());
            }

            propertiesPart.append("\"%s\"".formatted(v.key())).append(",");
            valuesPart.append("?,");
        }
        // Remove last comma
        propertiesPart.setLength(propertiesPart.length() - 1);
        valuesPart.setLength(valuesPart.length() - 1);

        propertiesPart.append(")");
        valuesPart.append(")");

        return Return.of("INSERT INTO %s %s %s;".formatted(insertRow.tableKey(), propertiesPart, valuesPart));
    }

    @Override
    public Return<SelectStatement> select(SelectQuery selectQuery) {
        var err = inputValidator.validateSelectQuery(selectQuery);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        final var query = new StringBuilder("SELECT ");
        if (selectQuery.propertyKeysToReturn().isEmpty()) {
            query.append("*");
        } else {
            for (final var prop : selectQuery.propertyKeysToReturn()) {
                err = inputValidator.validateSelectProperty(prop);
                if (err.isPresent()) {
                    return Return.error(err.get());
                }

                query.append("\"%s\",".formatted(prop.propertyKey()));
            }
            // remove last comma
            query.delete(query.length() - 1, query.length());
        }

        query.append(" FROM public.%s ".formatted(selectQuery.tableKey()));

        if (selectQuery.predicate() == null) {
            return Return.of(SelectStatement.of(query.append(";").toString(), List.of()));
        }

        List<ColumnValue<?>> parameters = new ArrayList<>();
        Return<String> where = switch (selectQuery.predicate()) {
            case CompoundPredicate c -> buildCompoundQuery(c, parameters);
            case RawPredicate<?> r -> buildRawQuery(r, parameters);
            default -> throw new IllegalStateException("Unexpected predicate " + selectQuery.predicate());
        };

        if (where.error().isPresent()) {
            return Return.error(where.error().get());
        }

        query.append("WHERE %s ;".formatted(where.value()));
        return Return.of(SelectStatement.of(query.toString(), Collections.unmodifiableList(parameters)));
    }

    @Override
    public Return<String> deleteFromTable(DeleteRow deleteRow) {
        var err = inputValidator.validateDeleteRow(deleteRow);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        var query = "DELETE FROM %s WHERE _id = %d;".formatted(deleteRow.tableKey(), deleteRow.rowId());
        return Return.of(query);
    }

    private void addReferenceProperty(
        ReferenceProperty r,
        StringBuilder statementBuilder,
        List<String> constraints
    ) {
        statementBuilder.append(
            "\"%s\" INTEGER%s%s%s%n".formatted(
                r.key(),
                r.required() ? " NOT NULL" : "",
                r.unique() ? " UNIQUE" : "",
                r.defaultValue() == null ? "" : " DEFAULT %d".formatted(r.defaultValue())
            )
        );

        constraints.add("FOREIGN KEY (\"%s\") REFERENCES %s(_id) ON DELETE %s ON UPDATE RESTRICT"
            .formatted(
                r.key(),
                r.refTableKey(),
                r.required() ? "RESTRICT" : (r.defaultValue() == null ? "SET NULL" : "SET DEFAULT")
            )
        );
    }

    private void addDecimalProperty(
        DecimalProperty d,
        StringBuilder statementBuilder,
        List<String> checks
    ) {
        statementBuilder.append(
            "\"%s\" NUMERIC(%d,%d) DEFAULT %s %s %s%n".formatted(
                d.key(),
                d.precision(),
                d.scale(),
                d.defaultValue() == null ? "NULL" : d.defaultValue(),
                d.required() ? "NOT NULL" : "",
                d.unique() ? "UNIQUE" : ""
            )
        );

        if (d.max() != null && d.min() != null) {
            checks.add("CHECK (\"%s\" BETWEEN %f AND %f)".formatted(d.key(), d.min(), d.max()));
            return;
        }

        if (d.max() != null) {
            checks.add("CHECK (\"%s\" <= %f)".formatted(d.key(), d.max()));
            return;
        }

        if (d.min() != null) {
            checks.add("CHECK (\"%s\" >= %f)".formatted(d.key(), d.min()));
            return;
        }
    }

    private <V> Return<String> buildRawQuery(RawPredicate<V> pred, List<ColumnValue<?>> parameters) {
        var err = inputValidator.validatePredicate(pred);
        return err.<Return<String>>map(Return::error).orElseGet(() -> switch (pred) {
            case IntegerPredicate i -> {
                parameters.add(IntegerValue.of(i.propertyKey(), i.value()));
                yield Return.of("\"%s\"%s?".formatted(i.propertyKey(), getIntegerOperator(i.op())));
            }
            case DecimalPredicate d -> {
                parameters.add(DecimalValue.of(d.propertyKey(), d.value()));
                yield Return.of("\"%s\"%s?".formatted(d.propertyKey(), getDecimalOperator(d.op())));
            }
            case BooleanPredicate b -> {
                parameters.add(BooleanValue.of(b.propertyKey(), b.value()));
                yield Return.of("\"%s\"%s?".formatted(b.propertyKey(), getBooleanOperator(b.op())));
            }
            case StringPredicate s -> {
                parameters.add(StringValue.of(s.propertyKey(), s.value()));
                yield Return.of("\"%s\"%s?".formatted(s.propertyKey(), getStringOperator(s.op())));
            }
            case ReferencePredicate r -> {
                parameters.add(ListValue.of(r.propertyKey(), r.value(), ListValue.ListType.REFERENCE));
                yield Return.of(
                    "\"%s\" %s (%s)".formatted(
                        r.propertyKey(),
                        getReferenceOperator(r.op()),
                        r.value().stream().map(v -> "?").collect(Collectors.joining(","))
                    )
                );
            }
            case NullablePredicate p -> Return.of(
                "\"%s\" IS%sNULL".formatted(p.propertyKey(), p.op() == NullablePredicate.Operator.IS_NULL ? " " : " NOT ")
            );
            default -> throw new IllegalStateException("Unexpected predicate " + pred);
        });
    }

    private Return<String> buildCompoundQuery(CompoundPredicate c, List<ColumnValue<?>> parameters) {
        var err = inputValidator.validateCompoundPredicate(c);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        Return<String> leftSubquery = switch (c.left()) {
            case CompoundPredicate cl -> buildCompoundQuery(cl, parameters);
            case RawPredicate<?> rl -> buildRawQuery(rl, parameters);
            default -> throw new IllegalStateException("Select: unknown left sub predicate");
        };

        if (leftSubquery.error().isPresent()) {
            log.error("Select: left subquery of compound query is invalid");
            return leftSubquery;
        }

        Return<String> rightSubquery = switch (c.right()) {
            case CompoundPredicate cr -> buildCompoundQuery(cr, parameters);
            case RawPredicate<?> rr -> buildRawQuery(rr, parameters);
            default -> throw new IllegalStateException("Select: unknown right sub predicate");
        };

        if (rightSubquery.error().isPresent()) {
            log.error("Select: right subquery of compound query is invalid");
            return rightSubquery;
        }

        return switch (c.op()) {
            case AND -> Return.of("(%s AND %s)".formatted(leftSubquery.value(), rightSubquery.value()));
            case OR -> Return.of("(%s OR %s)".formatted(leftSubquery.value(), rightSubquery.value()));
        };

    }

    private String getIntegerOperator(IntegerPredicate.Operator operator) {
        return switch (operator) {
            case IntegerPredicate.Operator.EQUAL -> "=";
            case IntegerPredicate.Operator.NOT_EQUAL -> "<>";
            case IntegerPredicate.Operator.LESS -> "<";
            case IntegerPredicate.Operator.GREATER -> ">";
            case IntegerPredicate.Operator.LESS_OR_EQUAL -> "<=";
            case IntegerPredicate.Operator.GREATER_OR_EQUAL -> ">=";
        };
    }

    private String getBooleanOperator(BooleanPredicate.Operator operator) {
        return switch (operator) {
            case BooleanPredicate.Operator.EQUAL -> "=";
            case BooleanPredicate.Operator.NOT_EQUAL -> "<>";
        };
    }

    private String getDecimalOperator(DecimalPredicate.Operator operator) {
        return switch (operator) {
            case DecimalPredicate.Operator.EQUAL -> "=";
            case DecimalPredicate.Operator.NOT_EQUAL -> "<>";
            case DecimalPredicate.Operator.LESS -> "<";
            case DecimalPredicate.Operator.GREATER -> ">";
            case DecimalPredicate.Operator.LESS_OR_EQUAL -> "<=";
            case DecimalPredicate.Operator.GREATER_OR_EQUAL -> ">=";
        };
    }

    private String getStringOperator(StringPredicate.Operator operator) {
        return switch (operator) {
            case StringPredicate.Operator.EQUAL -> "=";
            case StringPredicate.Operator.NOT_EQUAL -> "<>";
            case StringPredicate.Operator.LESS -> "<";
            case StringPredicate.Operator.GREATER -> ">";
            case StringPredicate.Operator.LESS_OR_EQUAL -> "<=";
            case StringPredicate.Operator.GREATER_OR_EQUAL -> ">=";
            case StringPredicate.Operator.LIKE -> "~~";
            case StringPredicate.Operator.NOT_LIKE -> "!~~";
        };
    }

    private String getReferenceOperator(ReferencePredicate.Operator operator) {
        return switch (operator) {
            case ReferencePredicate.Operator.IN -> "IN";
            case ReferencePredicate.Operator.NOT_IN -> "NOT IN";
        };
    }
}
