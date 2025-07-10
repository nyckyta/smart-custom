package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.properties.BooleanProperty;
import edu.ukma.smart.virtual.properties.DecimalProperty;
import edu.ukma.smart.virtual.properties.IntegerProperty;
import edu.ukma.smart.virtual.properties.ReferenceProperty;
import edu.ukma.smart.virtual.properties.StringProperty;
import edu.ukma.smart.virtual.select.BooleanPredicate;
import edu.ukma.smart.virtual.select.CompoundPredicate;
import edu.ukma.smart.virtual.select.DecimalPredicate;
import edu.ukma.smart.virtual.select.IntegerPredicate;
import edu.ukma.smart.virtual.select.RawPredicate;
import edu.ukma.smart.virtual.select.ReferencePredicate;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.select.StringPredicate;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.ColumnValue;
import edu.ukma.smart.virtual.values.DecimalValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.ListValue;
import edu.ukma.smart.virtual.values.StringValue;
import edu.ukma.smart.virtual.values.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PostgreQueryGenerator implements QueryGenerator {

    private static final int MAX_PRECISION = 131072;
    private static final int MAX_SCALE = 16383;
    private static final Set<String> STATIC_FIELDS = Set.of("_id", "_created");
    private static final Pattern KEY_REGEXP = Pattern.compile("^[a-z][a-z_]{1,100}$");
    private static final Logger log = LoggerFactory.getLogger(PostgreQueryGenerator.class);

    private static Optional<InputValidationErr> addIntegerColumn(IntegerProperty i,
                                                                 StringBuilder statementBuilder,
                                                                 List<String> checks) {
//        if (i.required() && i.defaultValue() == null) {
//            return Optional.of(InputValidationErr.error(
//                "Create table: Property key '%s' is required, default value is null"
//                    .formatted(i.key())));
//        }

        statementBuilder.append(
            ",%s BIGINT DEFAULT %s %s %s%n".formatted(
                i.key(),
                i.defaultValue() == null ? "NULL" : i.defaultValue(),
                i.required() ? "NOT NULL" : "",
                i.unique() ? "UNIQUE" : ""
            )
        );

        if (i.max() != null && i.min() != null) {
            if (i.max() < i.min()) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' max can not be less than min".formatted(
                            i.key()))
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

    private static Optional<InputValidationErr> addStringColumn(StringProperty s,
                                                                StringBuilder statementBuilder,
                                                                List<String> checks) {
//        if (s.required() && s.defaultValue() == null) {
//            return Optional.of(InputValidationErr.error(
//                "Create table: Property key '%s' is required, default value is null"
//                    .formatted(s.key())));
//        }
        statementBuilder.append(
            ",%s TEXT DEFAULT %s %s %s%n".formatted(
                s.key(),
                s.defaultValue() == null ? "NULL" : "$$" + s.defaultValue() + "$$",
                s.required() ? "NOT NULL" : "",
                s.unique() ? "UNIQUE" : "")
        );

        if (s.maxLength() != null && s.minLength() != null) {
            if (s.maxLength() < 1) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' max length can not be less than one".formatted(
                            s.key()))
                );
            }

            if (s.minLength() < 1) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' min length can not be less than one".formatted(
                            s.key()))
                );
            }

            if (s.maxLength() < s.minLength()) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' max length can not be less than min length".formatted(
                            s.key()))
                );
            }
            checks.add("CHECK (char_length(%s) BETWEEN %d AND %d)".formatted(s.key(), s.minLength(),
                s.maxLength()));
            return Optional.empty();
        }

        if (s.maxLength() != null) {
            if (s.maxLength() < 1) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' max length can not be less than one".formatted(
                            s.key()))
                );
            }

            checks.add("CHECK (char_length(%s) <= %d)".formatted(s.key(), s.maxLength()));
            return Optional.empty();
        }

        if (s.minLength() != null) {
            if (s.minLength() < 1) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' min length can not be less than one".formatted(
                            s.key()))
                );
            }

            checks.add("CHECK (char_length(%s) >= %d)".formatted(s.key(), s.minLength()));
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> addBooleanColumn(BooleanProperty b,
                                                                 StringBuilder statementBuilder) {
//        if (b.required() && b.defaultValue() == null) {
//            return Optional.of(InputValidationErr.error(
//                "Create table: Property key '%s' is required, default value is null"
//                    .formatted(b.key())));
//        }

        statementBuilder.append(
            ",%s BOOLEAN DEFAULT %s %s %s%n".formatted(
                b.key(),
                b.defaultValue() == null ? "NULL" : b.defaultValue(),
                b.required() ? "NOT NULL" : "",
                b.unique() ? "UNIQUE" : ""
            )
        );

        return Optional.empty();
    }

    @Override
    public Return<String> createTable(NewTable newTable) {
        if (!KEY_REGEXP.matcher(newTable.key()).matches()) {
            log.error("Create table: Table key '{}' does not match the required pattern '{}'",
                newTable.key(), KEY_REGEXP);
            return Return.error(
                InputValidationErr.error(
                    "Table key %s should consist only lower case english and '_'".formatted(
                        newTable.key()))
            );
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

        if (newTable.properties().isEmpty()) {
            log.error("Create table: Table '{}' has no properties defined", newTable.key());
            return Return.error(
                InputValidationErr.error(
                    "Table %s should have at least one property".formatted(newTable.key()))
            );
        }

        var constraints = new ArrayList<String>();
        for (var property : newTable.properties()) {
            if (!KEY_REGEXP.matcher(property.key()).matches()) {
                log.error(
                    "Create table: Property key '{}' does not match the required pattern '{}'",
                    property.key(), KEY_REGEXP);
                return Return.error(
                    InputValidationErr.error(
                        "Property key %s should consist only lower case english and '_' up to 100 chars".formatted(
                            property.key())
                    )
                );
            }

            var err = switch (property) {
                case StringProperty s -> addStringColumn(s, query, constraints);
                case IntegerProperty i -> addIntegerColumn(i, query, constraints);
                case BooleanProperty b -> addBooleanColumn(b, query);
                case DecimalProperty d -> addDecimalColumn(d, query, constraints);
                case ReferenceProperty r -> addReferenceProperty(r, query, constraints);
                default -> Optional.of(InputValidationErr.error("Create: not supported property"));
            };

            if (err.isPresent()) {
                return Return.error(err.get());
            }
        }

        constraints.forEach(c -> query.append(",%s".formatted(c)));
        query.append(");");

        return Return.of(query.toString());
    }

    @Override
    public Return<String> deleteTable(String tableKey) {
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Drop: Table key '{}' does not match the required pattern '{}'", tableKey,
                KEY_REGEXP);
            return Return.error(InputValidationErr.error(
                "Table key %s should consist only lower case english and '_'".formatted(tableKey)));
        }
        return Return.of("DROP TABLE %s;".formatted(tableKey));
    }


    @Override
    public Return<String> updateRow(UpdateRow updateRow) {
        if (!KEY_REGEXP.matcher(updateRow.tableKey()).matches()) {
            log.error("Update row: Table key '{}' does not match the required pattern '{}'", updateRow.tableKey(),
                KEY_REGEXP);
            return Return.error(InputValidationErr.error("Wrong table key %s".formatted(updateRow.tableKey())));
        }

        if (updateRow.rowId() < 1) {
            log.error("Update row: row id is less than 1");
            return Return.error(InputValidationErr.error("Row id cannot be less than 1"));
        }

        if (updateRow.valuesToUpdate().isEmpty()) {
            log.error("Update row: no params provided to update row");
            return Return.error(InputValidationErr.error("No params provided to update row"));
        }

        var queryBuilder = new StringBuilder()
            .append("UPDATE public.%s SET ".formatted(updateRow.tableKey()));
        for (var column : updateRow.valuesToUpdate()) {
            if (!KEY_REGEXP.matcher(column.key()).matches()) {
                log.error("Update row: Column key '{}' does not match the required pattern '{}'", column.key(),
                    KEY_REGEXP);
                return Return.error(InputValidationErr.error("Wrong table key %s".formatted(column.key())));
            }

            queryBuilder.append("%s=?,".formatted(column.key()));
        }

        // remove last comma
        queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        queryBuilder.append(" WHERE _id=?;");
        return Return.of(queryBuilder.toString());
    }

    @Override
    public Return<String> insertIntoTable(String tableKey, List<? extends ColumnValue<?>> columnValues) {
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Insert: Table key '{}' does not match the required pattern '{}'", tableKey,
                KEY_REGEXP);
            return Return.error(InputValidationErr.error("Wrong table key %s".formatted(tableKey)));
        }

        if (columnValues.isEmpty()) {
            return Return.of("INSERT INTO %s DEFAULT VALUES;".formatted(tableKey));
        }

        var columnsPart = new StringBuilder("(");
        var valuesPart = new StringBuilder("VALUES (");

        for (var column : columnValues) {
            if (!KEY_REGEXP.matcher(column.key()).matches()) {
                log.error("Add row: Column key '{}' does not match the required pattern '{}'", column.key(), KEY_REGEXP);
                return Return.error(
                    InputValidationErr.error("Wrong table key %s".formatted(column.key())));
            }

            columnsPart.append(column.key()).append(",");
            valuesPart.append("?,");
        }
        // Remove last comma
        columnsPart.setLength(columnsPart.length() - 1);
        valuesPart.setLength(valuesPart.length() - 1);

        columnsPart.append(")");
        valuesPart.append(")");

        return Return.of("INSERT INTO %s %s %s;".formatted(tableKey, columnsPart, valuesPart));
    }

    @Override
    public Return<SelectStatement> select(SelectQuery selectQuery) {
        final var tableKey = selectQuery.tableKey();
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Select: Table key '{}' does not match the required pattern '{}'", tableKey, KEY_REGEXP);
            return Return.error(InputValidationErr.error("Wrong table key %s".formatted(tableKey)));
        }

        final var query = new StringBuilder("SELECT ");
        if (selectQuery.columnKeysToReturn().isEmpty()) {
            query.append("*");
        } else {
            for (final var cKey : selectQuery.columnKeysToReturn()) {
                if (cKey == null || (!KEY_REGEXP.matcher(cKey).matches() && !STATIC_FIELDS.contains(cKey))) {
                    log.error("Select: Column key '{}' does not match the required pattern '{}'", cKey, KEY_REGEXP);
                    return Return.error(
                        InputValidationErr.error("Select: Wrong column key %s".formatted(cKey)));
                }

                query.append("%s,".formatted(cKey));
            }
            // remove last comma
            query.delete(query.length() - 1, query.length());
        }

        query.append(" FROM public.%s ".formatted(tableKey));

        // build where part of the query
        if (selectQuery.predicate() == null) {
            return Return.of(SelectStatement.of(query.append(";").toString()));
        }

        List<ColumnValue<?>> parameters = new ArrayList<>();
        Return<String> where = switch (selectQuery.predicate()) {
            case CompoundPredicate c -> buildCompoundQuery(c, parameters);
            case RawPredicate<?> r -> buildRawQuery(r, parameters);
            default -> Return.error(InputValidationErr.error("Select: Unknown predicate"));
        };

        if (where.error().isPresent()) {
            return Return.error(where.error().get());
        }

        query.append("WHERE %s ;".formatted(where.value()));
        return Return.of(SelectStatement.of(query.toString(), Collections.unmodifiableList(parameters)));
    }

    @Override
    public Return<String> deleteFromTable(String tableKey, int rowId) {
        if (!KEY_REGEXP.matcher(tableKey).matches()) {
            log.error("Delete row: Table key '{}' does not match the required pattern '{}'",
                tableKey,
                KEY_REGEXP);
            return Return.error(InputValidationErr.error("Wrong table key %s".formatted(tableKey)));
        }

        if (rowId < 1) {
            log.error("Delete row: row id is less than 1");
            return Return.error(InputValidationErr.error("Row id cannot be less than 1"));
        }

        var query = "DELETE FROM %s WHERE _id = %d;".formatted(tableKey, rowId);
        return Return.of(query);
    }

    private Optional<InputValidationErr> addReferenceProperty(
        ReferenceProperty r,
        StringBuilder statementBuilder,
        List<String> constraints
    ) {
        if (!KEY_REGEXP.matcher(r.refTableKey()).matches()) {
            return Optional.of(InputValidationErr.error(
                "Create table: wrong table reference %s".formatted(r.refTableKey())));
        }

        statementBuilder.append(
            ",%s INTEGER %s %s%n".formatted(
                r.key(),
                r.required() ? "NOT NULL" : "",
                r.unique() ? "UNIQUE" : ""
            )
        );

        constraints.add("FOREIGN KEY (%s) REFERENCES %s(_id)".formatted(r.key(), r.refTableKey()));
        return Optional.empty();
    }

    private Optional<InputValidationErr> addDecimalColumn(DecimalProperty d,
                                                          StringBuilder statementBuilder,
                                                          List<String> checks) {
//        if (d.required() && d.defaultValue() == null) {
//            return Optional.of(InputValidationErr.error(
//                "Create table: Property key '%s' is required, default value is null"
//                    .formatted(d.key())));
//        }

        if (d.precision() < 1 || d.precision() > MAX_PRECISION) {
            return Optional.of(InputValidationErr.error(
                "Create table: property %s has invalid precision".formatted(d.key())));
        }

        if (d.scale() < 1 || d.scale() > MAX_SCALE) {
            return Optional.of(InputValidationErr.error(
                "Create table: property %s has invalid scale".formatted(d.key())));
        }

        statementBuilder.append(
            ",%s NUMERIC(%d,%d) DEFAULT %s %s %s%n".formatted(
                d.key(),
                d.precision(),
                d.scale(),
                d.defaultValue() == null ? "NULL" : d.defaultValue(),
                d.required() ? "NOT NULL" : "",
                d.unique() ? "UNIQUE" : ""
            )
        );

        if (d.max() != null && d.min() != null) {
            if (d.max().compareTo(d.min()) < 0) {
                return Optional.of(
                    InputValidationErr.error(
                        "Create table: Property key '%s' max can not be less than min".formatted(
                            d.key()))
                );
            }
            checks.add("CHECK (%s BETWEEN %f AND %f)".formatted(d.key(), d.min(), d.max()));
            return Optional.empty();
        }

        if (d.max() != null) {
            checks.add("CHECK (%s <= %f)".formatted(d.key(), d.max()));
            return Optional.empty();
        }

        if (d.min() != null) {
            checks.add("CHECK (%s >= %f)".formatted(d.key(), d.min()));
            return Optional.empty();
        }

        return Optional.empty();
    }

    private <V> Return<String> buildRawQuery(RawPredicate<V> pred, List<ColumnValue<?>> parameters) {
        final var cKey = pred.columnKey();
        if (!KEY_REGEXP.matcher(pred.columnKey()).matches()) {
            log.error("Select: Column key '{}' does not match the required pattern '{}'", cKey, KEY_REGEXP);
            return Return.error(
                InputValidationErr.error("Select: Wrong table key %s".formatted(cKey)));
        }

        return switch (pred) {
            case IntegerPredicate i -> {
                parameters.add(IntegerValue.of(i.columnKey(), i.value()));
                yield Return.of("%s%s?".formatted(i.columnKey(), getIntegerOperator(i.op())));
            }
            case DecimalPredicate d -> {
                parameters.add(DecimalValue.of(d.columnKey(), d.value()));
                yield Return.of("%s%s?".formatted(d.columnKey(), getDecimalOperator(d.op())));
            }
            case BooleanPredicate b -> {
                parameters.add(BooleanValue.of(b.columnKey(), b.value()));
                yield Return.of("%s%s?".formatted(b.columnKey(), getBooleanOperator(b.op())));
            }
            case StringPredicate s -> {
                parameters.add(StringValue.of(s.columnKey(), s.value()));
                yield Return.of("%s%s?".formatted(s.columnKey(), getStringOperator(s.op())));
            }
            case ReferencePredicate r -> {
                parameters.add(ListValue.of(r.columnKey(), r.value(), Type.REFERENCE));
                yield Return.of(
                    "%s %s (%s)".formatted(
                        r.columnKey(),
                        getReferenceOperator(r.op()),
                        r.value().stream().map(v -> "?").collect(Collectors.joining(","))
                    )
                );
            }
            default -> Return.error(InputValidationErr.error("Select: unknown raw predicate"));
        };
    }

    private Return<String> buildCompoundQuery(CompoundPredicate c, List<ColumnValue<?>> parameters) {
        Return<String> leftSubquery = switch (c.left()) {
            case CompoundPredicate cl -> buildCompoundQuery(cl, parameters);
            case RawPredicate<?> rl -> buildRawQuery(rl, parameters);
            default -> Return.error(InputValidationErr.error("Select: unknown left sub predicate"));
        };

        if (leftSubquery.error().isPresent()) {
            log.error("Select: left subquery of compound query is invalid");
            return leftSubquery;
        }

        Return<String> rightSubquery = switch (c.right()) {
            case CompoundPredicate cr -> buildCompoundQuery(cr, parameters);
            case RawPredicate<?> rr -> buildRawQuery(rr, parameters);
            default -> Return.error(InputValidationErr.error("Select: unknown right sub predicate"));
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
