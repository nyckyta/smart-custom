package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.constraints.UniqueConstraint;
import edu.ukma.smart.virtual.ddl.create.BooleanProperty;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.Property;
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
import edu.ukma.smart.virtual.dml.select.Predicate;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PostgreQueryGenerator implements QueryGenerator {

    private static final Logger log = LoggerFactory.getLogger(PostgreQueryGenerator.class);
    private final InputValidator inputValidator = new PostgreInputValidator();
    private final PostgreSqlExceptionHandler exceptionHandler = new PostgreSqlExceptionHandler();
    private final String schema;

    public PostgreQueryGenerator(String workingSchema) {
        this.schema = workingSchema;
    }

    private static void addIntegerProperty(
        IntegerProperty i,
        StringBuilder statementBuilder
    ) {

        statementBuilder.append("%s BIGINT %s %s%n".formatted(
            EscapeUtil.escapeStringIdentifier(i.key()),
            i.defaultValue() == null ? "" : "DEFAULT %d".formatted(i.defaultValue()),
            i.notNull() ? "NOT NULL" : "")
        );
    }

    private static void addStringProperty(
        StringProperty s,
        StringBuilder statementBuilder
    ) throws SQLException {
        var defaultValuePart = s.defaultValue() == null ? "NULL" : EscapeUtil.escapeStringLiteral(s.defaultValue());
        statementBuilder.append(
            "%s TEXT DEFAULT %s %s%n"
                .formatted(
                    EscapeUtil.escapeStringIdentifier(s.key()),
                    defaultValuePart,
                    s.notNull() ? "NOT NULL" : ""
                )
        );
    }

    private static void addBooleanProperty(
        BooleanProperty b,
        StringBuilder statementBuilder
    ) {

        statementBuilder.append(
            "%s BOOLEAN DEFAULT %s %s%n".formatted(
                EscapeUtil.escapeStringIdentifier(b.key()),
                b.defaultValue() == null ? "NULL" : b.defaultValue(),
                b.notNull() ? "NOT NULL" : ""
            )
        );

    }

    private static String buildMaxLengthConstraint(Property property, PropertyConstraint c) {
        return "CHECK( char_length(%s) <= %d )".formatted(EscapeUtil.escapeStringIdentifier(property.key()),
            c.castValue(Integer.class));
    }

    private static String buildUniqueConstraintCreateTable(UniqueConstraint c) {
        return "UNIQUE (%s)".formatted(
            c.properties().stream().map(EscapeUtil::escapeStringIdentifier).collect(Collectors.joining(",")));
    }

    private static String buildUniqueConstraintAddProperty(UniqueConstraint c) {
        return "ADD UNIQUE (%s)".formatted(
            c.properties().stream().map(EscapeUtil::escapeStringIdentifier).collect(Collectors.joining(",")));
    }

    private static String buildLessThanValue(Property property, PropertyConstraint p) {
        var escapedKey = EscapeUtil.escapeStringIdentifier(property.key());
        return switch (property.type()) {
            case INTEGER -> "CHECK( %s < %d )".formatted(escapedKey, p.castValue(Long.class));
            case REFERENCE -> "CHECK( %s < %d )".formatted(escapedKey, p.castValue(Integer.class));
            case DECIMAL -> "CHECK( %s < %f )".formatted(escapedKey, p.castValue(BigDecimal.class));
            case STRING ->
                "CHECK( %s < '%s' )".formatted(escapedKey, EscapeUtil.escapeStringLiteral(p.castValue(String.class)));
            case BOOLEAN -> throw new IllegalStateException("Boolean constraint can't be here");
        };
    }

    private static String buildLessOrEqualValue(Property property, PropertyConstraint cons) {
        var escapedKey = EscapeUtil.escapeStringIdentifier(property.key());
        return switch (property.type()) {
            case INTEGER -> "CHECK( %s <= %d )".formatted(escapedKey, cons.castValue(Long.class));
            case REFERENCE -> "CHECK( %s <= %d )".formatted(escapedKey, cons.castValue(Integer.class));
            case DECIMAL -> "CHECK( %s <= %f )".formatted(escapedKey, cons.castValue(BigDecimal.class));
            case STRING ->
                "CHECK( %s <= %s )".formatted(escapedKey, EscapeUtil.escapeStringLiteral(cons.castValue(String.class)));
            case BOOLEAN -> throw new IllegalStateException("Boolean constraint can't be here");
        };
    }

    private static String buildGreaterThanValue(Property property, PropertyConstraint cons) {
        var escapedKey = EscapeUtil.escapeStringIdentifier(property.key());
        return switch (property.type()) {
            case INTEGER -> "CHECK( %s > %d )".formatted(escapedKey, cons.castValue(Long.class));
            case REFERENCE -> "CHECK( %s > %d )".formatted(escapedKey, cons.castValue(Integer.class));
            case DECIMAL -> "CHECK( %s > %f )".formatted(escapedKey, cons.castValue(BigDecimal.class));
            case STRING ->
                "CHECK( %s > %s )".formatted(escapedKey, EscapeUtil.escapeStringLiteral(cons.castValue(String.class)));
            case BOOLEAN -> throw new IllegalStateException("Boolean constraint can't be here");
        };
    }

    private static String buildGreaterOrEqualThanValue(Property property, PropertyConstraint cons) {
        var escapedKey = EscapeUtil.escapeStringIdentifier(property.key());
        return switch (property.type()) {
            case INTEGER -> "CHECK( %s >= %d )".formatted(escapedKey, cons.castValue(Long.class));
            case REFERENCE -> "CHECK( %s >= %d )".formatted(escapedKey, cons.castValue(Integer.class));
            case DECIMAL -> "CHECK( %s >= %f )".formatted(escapedKey, cons.castValue(BigDecimal.class));
            case STRING ->
                "CHECK( %s >= %s )".formatted(escapedKey, EscapeUtil.escapeStringLiteral(cons.castValue(String.class)));
            case BOOLEAN -> throw new IllegalStateException("Boolean constraint can't be here");
        };
    }

    private static String buildIn(Property property, PropertyConstraint cons) {
        var escapedKey = EscapeUtil.escapeStringIdentifier(property.key());
        return switch (property.type()) {
            case STRING ->
                "CHECK( %s IN (%s))".formatted(escapedKey, convertArrayToInParameter(cons.castValue(String[].class)));
            case REFERENCE ->
                "CHECK( %s IN (%s))".formatted(escapedKey, convertArrayToInParameter(cons.castValue(Integer[].class)));
            case INTEGER ->
                "CHECK( %s IN (%s))".formatted(escapedKey, convertArrayToInParameter(cons.castValue(Long[].class)));
            case DECIMAL -> "CHECK( %s IN (%s))".formatted(escapedKey,
                convertArrayToInParameter(cons.castValue(BigDecimal[].class)));
            case BOOLEAN -> throw new IllegalStateException("Boolean constraint can't be here");
        };
    }

    private static String buildNotIn(Property property, PropertyConstraint cons) {
        var escapedKey = EscapeUtil.escapeStringIdentifier(property.key());
        return switch (property.type()) {
            case STRING -> "CHECK( %s NOT IN (%s)".formatted(escapedKey,
                convertArrayToInParameter(cons.castValue(String[].class)));
            case REFERENCE -> "CHECK( %s NOT IN (%s)".formatted(escapedKey,
                convertArrayToInParameter(cons.castValue(Integer[].class)));
            case INTEGER ->
                "CHECK( %s NOT IN (%s)".formatted(escapedKey, convertArrayToInParameter(cons.castValue(Long[].class)));
            case DECIMAL -> "CHECK( %s NOT IN (%s)".formatted(escapedKey,
                convertArrayToInParameter(cons.castValue(BigDecimal[].class)));
            case BOOLEAN -> throw new IllegalStateException("Boolean constraint can't be here");
        };
    }

    private static String convertArrayToInParameter(String[] arr) {
        return Arrays.stream(arr).map(EscapeUtil::escapeStringLiteral).collect(Collectors.joining(","));
    }

    private static <T> String convertArrayToInParameter(T[] arr) {
        return Arrays.stream(arr).map(Object::toString).collect(Collectors.joining(","));
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
            .append("""
                    CREATE TABLE %s.%s (
                        _id SERIAL PRIMARY KEY NOT NULL,
                        _created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                    """.formatted(schema, EscapeUtil.escapeStringIdentifier(newTable.key()))
            );

        var constraints = new ArrayList<String>();
        var comments = new ArrayList<String>();
        comments.add(buildTableComment(newTable));
        try {
            for (var property : newTable.properties()) {
                comments.add(buildPropertyComment(newTable.key(), property));
                query.append(",");
                switch (property.type()) {
                    case STRING -> addStringProperty((StringProperty) property, query);
                    case INTEGER -> addIntegerProperty((IntegerProperty) property, query);
                    case BOOLEAN -> addBooleanProperty((BooleanProperty) property, query);
                    case DECIMAL -> addDecimalProperty((DecimalProperty) property, query);
                    case REFERENCE -> createTableAddReferenceProperty((ReferenceProperty) property, query, constraints);
                }

                for (var c : property.constraints()) {
                    switch (c.type()) {
                        case UNIQUE -> constraints.add(buildUniqueConstraintCreateTable((UniqueConstraint) c));
                        case STRING_MAX_LENGTH ->
                            constraints.add(buildMaxLengthConstraint(property, (PropertyConstraint) c));
                        case STRING_MIN_LENGTH ->
                            constraints.add(buildMinLengthConstraint(property, (PropertyConstraint) c));
                        case LESS_THAN_VALUE -> constraints.add(buildLessThanValue(property, (PropertyConstraint) c));
                        case GREATER_THAN_VALUE ->
                            constraints.add(buildGreaterThanValue(property, (PropertyConstraint) c));
                        case LESS_OR_EQUAL_THAN_VALUE ->
                            constraints.add(buildLessOrEqualValue(property, (PropertyConstraint) c));
                        case GREATER_OR_EQUAL_THAN_VALUE ->
                            constraints.add(buildGreaterOrEqualThanValue(property, (PropertyConstraint) c));
                        case IN -> constraints.add(buildIn(property, (PropertyConstraint) c));
                        case NOT_IN -> constraints.add(buildNotIn(property, (PropertyConstraint) c));
                    }
                }
            }
        } catch (SQLException e) {
            return Return.error(exceptionHandler.handle(e));
        }

        constraints.forEach(c -> query.append(",%s".formatted(c)));
        query.append(");\n");
        query.append(String.join(";\n", comments));
        query.append(";");
        return Return.of(query.toString());
    }

    private String buildMinLengthConstraint(Property property, PropertyConstraint c) {
        return "CHECK( char_length(%s) >= %d )".formatted(EscapeUtil.escapeStringIdentifier(property.key()),
            c.castValue(Integer.class));
    }

    @Override
    public Return<String> dropTable(DropTable deleteTable) {
        var err = inputValidator.validateDeleteTable(deleteTable);
        return err.<Return<String>>map(Return::error)
            .orElseGet(() -> Return.of(
                "DROP TABLE %s.%s;".formatted(schema, EscapeUtil.escapeStringIdentifier(deleteTable.tableKey()))));

    }

    @Override
    public Return<String> getTables() {
        return Return.of("""
                         SELECT t.tablename, obj_description(t.schema_table::regclass::oid, 'pg_class')
                         FROM (SELECT schemaname, tablename, concat(schemaname, '.', tablename) AS schema_table FROM pg_tables WHERE schemaname = '%s') AS t;
                         """.formatted(schema)
        );
    }

    @Override
    public Return<String> getProperties(String tableKey) {
        var err = inputValidator.validateTableKey(tableKey);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        // exclude system tables as well as static fields that users do not want to see
        var ignoredProperties = Stream.concat(
            PostgreInputValidator.SYSTEM_EXCLUDED_FIELDS.stream(),
            PostgreInputValidator.STATIC_FIELDS.stream()
        ).map("'%s'"::formatted).collect(Collectors.joining(","));
        // TODO: return static fields too
        return Return.of(
            """
            SELECT
                attr.attname,
                col_description(attr.attrelid::regclass::oid, attr.attnum),
                attr.attnotnull,
                pg_get_expr(adef.adbin, adef.adrelid),
                typ.typname,
                (information_schema._pg_numeric_precision(
                    information_schema._pg_truetypid(attr.*, typ.*),
                    information_schema._pg_truetypmod(attr.*, typ.*))
                )::information_schema.cardinal_number AS numeric_precision,
                (information_schema._pg_numeric_scale(
                    information_schema._pg_truetypid(attr.*, typ.*),
                    information_schema._pg_truetypmod(attr.*, typ.*))
                )::information_schema.cardinal_number AS numeric_scale,
                pg_get_constraintdef(dep.objid, TRUE),
                dep.objid
            FROM
                pg_attribute attr
                INNER JOIN pg_type typ ON attr.atttypid = typ.oid
                LEFT JOIN pg_attrdef adef ON attr.attrelid = adef.adrelid AND attr.attnum = adef.adnum
                LEFT JOIN pg_depend dep ON dep.refobjid = attr.attrelid
                    AND (dep.refclassid = ('pg_class'::regclass)::oid)
                    AND (dep.classid = ('pg_constraint'::regclass)::oid)
                    AND (dep.refobjid = attr.attrelid)
                    AND (dep.refobjsubid = attr.attnum)
                    AND (dep.deptype = 'a')
            WHERE
                (attr.attrelid = (select oid from pg_class where relnamespace = (select oid from pg_namespace where nspname = '%s')
                AND relkind = 'r'
                AND relname=?))
              AND
                (attr.attname NOT IN ('_id', '_created', 'tableoid', 'xmin', 'cmin', 'xmax', 'cmax', 'ctid'))
              AND
                (attr.attisdropped <> TRUE)
            ORDER BY attr.attname ASC;
            """.formatted(schema));
    }

    @Override
    public Return<String> addProperty(AddProperty addProperty) {
        var err = inputValidator.validateAddProperty(addProperty);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        String escapedKey = EscapeUtil.escapeStringIdentifier(addProperty.tableKey());
        var query =
            new StringBuilder("ALTER TABLE %s.%s ADD COLUMN%n".formatted(schema, escapedKey));
        var constraints = new ArrayList<String>();
        var uniqueConstraint = new StringBuilder();
        var foreignKeyConstraint = new StringBuilder();
        var prop = addProperty.property();
        var comment = buildPropertyComment(addProperty.tableKey(), prop);
        try {
            switch (prop.type()) {
                case STRING -> addStringProperty((StringProperty) prop, query);
                case INTEGER -> addIntegerProperty((IntegerProperty) prop, query);
                case BOOLEAN -> addBooleanProperty((BooleanProperty) prop, query);
                case DECIMAL -> addDecimalProperty((DecimalProperty) prop, query);
                case REFERENCE -> addReferenceProperty((ReferenceProperty) prop, query, foreignKeyConstraint);
            }

            for (var c : prop.constraints()) {
                switch (c.type()) {
                    case UNIQUE -> uniqueConstraint.append(buildUniqueConstraintAddProperty((UniqueConstraint) c));
                    case STRING_MAX_LENGTH -> constraints.add(buildMaxLengthConstraint(prop, (PropertyConstraint) c));
                    case STRING_MIN_LENGTH -> constraints.add(buildMinLengthConstraint(prop, (PropertyConstraint) c));
                    case LESS_THAN_VALUE -> constraints.add(buildLessThanValue(prop, (PropertyConstraint) c));
                    case GREATER_THAN_VALUE -> constraints.add(buildGreaterThanValue(prop, (PropertyConstraint) c));
                    case LESS_OR_EQUAL_THAN_VALUE ->
                        constraints.add(buildLessOrEqualValue(prop, (PropertyConstraint) c));
                    case GREATER_OR_EQUAL_THAN_VALUE ->
                        constraints.add(buildGreaterOrEqualThanValue(prop, (PropertyConstraint) c));
                    case IN -> constraints.add(buildIn(prop, (PropertyConstraint) c));
                    case NOT_IN -> constraints.add(buildNotIn(prop, (PropertyConstraint) c));
                }
            }
        } catch (SQLException e) {
            return Return.error(exceptionHandler.handle(e));
        }

        query.append(" %s%s".formatted(String.join(" ", constraints),
            foreignKeyConstraint.isEmpty() ? "" : ",ADD " + foreignKeyConstraint));
        query.append(uniqueConstraint.isEmpty() ? "" : ",%s".formatted(uniqueConstraint.toString()));
        query.append(";").append(comment);
        return Return.of(query.toString());
    }

    @Override
    public Return<String> dropProperty(DropProperty dropProperty) {
        var err = inputValidator.validateDropProperty(dropProperty);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        var query =
            "ALTER TABLE %s.%s DROP COLUMN %s;".formatted(schema, dropProperty.tableKey(), dropProperty.columnKey());
        return Return.of(query);
    }


    @Override
    public Return<String> updateRow(UpdateRow updateRow) {
        var err = inputValidator.validateUpdateRow(updateRow);
        if (err.isPresent()) {
            return Return.error(err.get());
        }

        var queryBuilder = new StringBuilder()
            .append("UPDATE %s.%s SET ".formatted(schema, updateRow.tableKey()));
        for (var column : updateRow.valuesToUpdate()) {
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
            return Return.of("INSERT INTO %s.%s DEFAULT VALUES;".formatted(schema, insertRow.tableKey()));
        }

        var propertiesPart = new StringBuilder("(");
        var valuesPart = new StringBuilder("VALUES (");

        for (var v : insertRow.columnValues()) {
            propertiesPart.append("\"%s\"".formatted(v.key())).append(",");
            valuesPart.append("?,");
        }
        // Remove last comma
        propertiesPart.setLength(propertiesPart.length() - 1);
        valuesPart.setLength(valuesPart.length() - 1);

        propertiesPart.append(")");
        valuesPart.append(")");

        return Return.of(
            "INSERT INTO %s.%s %s %s;".formatted(schema, insertRow.tableKey(), propertiesPart, valuesPart));
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
                query.append("\"%s\",".formatted(prop.propertyKey()));
            }
            // remove last comma
            query.delete(query.length() - 1, query.length());
        }

        query.append(" FROM %s.%s ".formatted(schema, selectQuery.tableKey()));

        if (selectQuery.predicate() == null) {
            return Return.of(SelectStatement.of(query.append(";").toString(), List.of()));
        }

        List<ColumnValue<?>> parameters = new ArrayList<>();
        var predicate = selectQuery.predicate();
        var where = buildPredicate(predicate, parameters);

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

        var query = "DELETE FROM %s.%s WHERE _id = %d;".formatted(
            schema,
            EscapeUtil.escapeStringIdentifier(deleteRow.tableKey()),
            deleteRow.rowId()
        );
        return Return.of(query);
    }

    @Override
    public Return<String> foreignKeyTableReferences() {
        return Return.of(
            """
            SELECT DISTINCT
                tc.table_name constraint_table_name,
                ccu.table_name AS foreign_table_name
            FROM information_schema.table_constraints AS tc
               INNER JOIN information_schema.key_column_usage AS kcu
                   ON tc.constraint_name = kcu.constraint_name
                   AND tc.table_schema = kcu.table_schema
               INNER JOIN information_schema.constraint_column_usage AS ccu
                   ON ccu.constraint_name = tc.constraint_name
                   AND ccu.table_schema = tc.table_schema
            WHERE
                tc.constraint_type = 'FOREIGN KEY'
                AND tc.table_schema IN ('%s')
            ORDER BY
                tc.table_name;
            """.formatted(schema)
        );
    }

    private void createTableAddReferenceProperty(
        ReferenceProperty r,
        StringBuilder statementBuilder,
        List<String> constraints
    ) {
        statementBuilder.append(
            "%s INTEGER%s%s%n".formatted(
                EscapeUtil.escapeStringIdentifier(r.key()),
                r.notNull() ? " NOT NULL" : "",
                r.defaultValue() == null ? "" : " DEFAULT %d".formatted(r.defaultValue())
            )
        );

        constraints.add("FOREIGN KEY (%s) REFERENCES %s.%s(_id) ON DELETE %s ON UPDATE RESTRICT"
            .formatted(
                EscapeUtil.escapeStringIdentifier(r.key()),
                schema,
                EscapeUtil.escapeStringIdentifier(r.refTableKey()),
                r.notNull() ? "RESTRICT" : (r.defaultValue() == null ? "SET NULL" : "SET DEFAULT")
            )
        );
    }

    private void addReferenceProperty(
        ReferenceProperty r,
        StringBuilder statementBuilder,
        StringBuilder foreignKeyConstraint
    ) {
        statementBuilder.append(
            "%s INTEGER%s%s%n".formatted(
                EscapeUtil.escapeStringIdentifier(r.key()),
                r.notNull() ? " NOT NULL" : "",
                r.defaultValue() == null ? "" : " DEFAULT %d".formatted(r.defaultValue())
            )
        );

        foreignKeyConstraint.append("FOREIGN KEY (%s) REFERENCES %s.%s(_id) ON DELETE %s ON UPDATE RESTRICT"
            .formatted(
                EscapeUtil.escapeStringIdentifier(r.key()),
                schema,
                EscapeUtil.escapeStringIdentifier(r.refTableKey()),
                r.notNull() ? "RESTRICT" : (r.defaultValue() == null ? "SET NULL" : "SET DEFAULT")
            )
        );
    }

    private void addDecimalProperty(
        DecimalProperty d,
        StringBuilder statementBuilder
    ) {
        statementBuilder.append(
            "%s NUMERIC(%d,%d) DEFAULT %s %s%n".formatted(
                EscapeUtil.escapeStringIdentifier(d.key()),
                d.precision(),
                d.scale(),
                d.defaultValue() == null ? "NULL" : d.defaultValue(),
                d.notNull() ? "NOT NULL" : ""
            )
        );
    }

    private Return<String> buildPredicate(Predicate pred, List<ColumnValue<?>> parameters) {
        return switch (pred.type()) {
            case INTEGER -> {
                final var i = (IntegerPredicate) pred;
                parameters.add(IntegerValue.of(i.propertyKey(), i.value()));
                yield Return.of("\"%s\"%s?".formatted(i.propertyKey(), getIntegerOperator(i.op())));
            }
            case DECIMAL -> {
                final var d = (DecimalPredicate) pred;
                parameters.add(DecimalValue.of(d.propertyKey(), d.value()));
                yield Return.of("\"%s\"%s?".formatted(d.propertyKey(), getDecimalOperator(d.op())));
            }
            case BOOLEAN -> {
                final var b = (BooleanPredicate) pred;
                parameters.add(BooleanValue.of(b.propertyKey(), b.value()));
                yield Return.of("\"%s\"%s?".formatted(b.propertyKey(), getBooleanOperator(b.op())));
            }
            case STRING -> {
                final var s = (StringPredicate) pred;
                parameters.add(StringValue.of(s.propertyKey(), s.value()));
                yield Return.of("\"%s\"%s?".formatted(s.propertyKey(), getStringOperator(s.op())));
            }
            case REFERENCE -> {
                final var r = (ReferencePredicate) pred;
                parameters.add(ListValue.ofReference(r.propertyKey(), r.value()));
                yield Return.of(
                    "\"%s\" %s (%s)".formatted(
                        r.propertyKey(),
                        getReferenceOperator(r.op()),
                        r.value().stream().map(v -> "?").collect(Collectors.joining(","))
                    )
                );
            }
            case NULL -> {
                final var n = (NullablePredicate) pred;
                yield Return.of(
                    "\"%s\" IS%sNULL".formatted(n.propertyKey(),
                        n.op() == NullablePredicate.Operator.IS_NULL ? " " : " NOT ")
                );
            }
            case COMPOUND -> {
                final var c = (CompoundPredicate) pred;
                yield buildCompoundQuery(c, parameters);
            }

        };
    }

    private Return<String> buildCompoundQuery(CompoundPredicate c, List<ColumnValue<?>> parameters) {
        final var leftSubquery = buildPredicate(c.left(), parameters);

        if (leftSubquery.error().isPresent()) {
            log.error("Select: left subquery of compound query is invalid");
            return leftSubquery;
        }

        final var rightSubquery = buildPredicate(c.right(), parameters);

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

    private String buildTableComment(NewTable newTable) {
        return "COMMENT ON TABLE %s.%s IS '%s'".formatted(
            schema,
            EscapeUtil.escapeStringIdentifier(newTable.key()),
            JsonUtils.generateComment(newTable.name(), newTable.description())
        );
    }

    private String buildPropertyComment(String tableKey, Property property) {
        return "COMMENT ON COLUMN %s.%s.%s IS '%s'".formatted(
            schema,
            EscapeUtil.escapeStringIdentifier(tableKey),
            EscapeUtil.escapeStringIdentifier(property.key()),
            JsonUtils.generateComment(property.name(), property.description())
        );
    }
}
