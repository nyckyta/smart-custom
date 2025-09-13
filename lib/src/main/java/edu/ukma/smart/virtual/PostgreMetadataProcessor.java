package edu.ukma.smart.virtual;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.constraints.UniqueConstraint;
import edu.ukma.smart.virtual.ddl.create.BooleanProperty;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.Property;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.errors.FatalError;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.metadata.Table;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ArrayConstructor;
import net.sf.jsqlparser.expression.BooleanValue;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PostgreMetadataProcessor implements MetadataProcessor {

    private static final Logger log = LoggerFactory.getLogger(PostgreMetadataProcessor.class);
    private static final JsonFactory jsonFactory = new JsonFactory();

    @Override
    public Return<List<Table>> processTables(ResultSet rs) throws SQLException {
        List<Table> tables = new ArrayList<>();
        while (rs.next()) {
            var tableKey = rs.getString(1);
            var nameDesc = parseComment(rs.getString(2));
            tables.add(Table.of(tableKey, nameDesc.left(), nameDesc.right()));
        }

        return Return.of(tables);
    }

    @Override
    public Return<List<Property>> processProperties(ResultSet rs) throws SQLException {
        final var processedConstraints = new HashSet<Integer>();
        final var propertyKeyToBuilder = new TreeMap<String, Property.Builder<?>>();

        while (rs.next()) {
            var propertyKey = rs.getString(1);
            var type = parseType(rs.getString(5));
            if (!propertyKeyToBuilder.containsKey(propertyKey)) {
                var nameDescr = parseComment(rs.getString(2));
                boolean isNullable = rs.getBoolean(3);

                var builder = switch (type) {
                    case STRING -> {
                        var defColumn = rs.getString(4);
                        // by default cast expression returned, we remove casting part and take only literal
                        defColumn =
                            defColumn == null ? null : defColumn.substring(1, defColumn.length() - "::text".length() - 1);
                        yield StringProperty.builder().key(propertyKey).name(nameDescr.left())
                            .description(nameDescr.right()).notNull(isNullable).defaultValue(defColumn);
                    }
                    case BOOLEAN -> BooleanProperty.builder().key(propertyKey).name(nameDescr.left())
                        .description(nameDescr.right()).notNull(isNullable).defaultValue(rs.getBoolean(4));
                    case REFERENCE -> {
                        int columnDefValue = rs.getInt(4);
                        var defaultVal = rs.wasNull() ? null : columnDefValue;
                        yield ReferenceProperty.builder().key(propertyKey).name(nameDescr.left())
                            .description(nameDescr.right()).notNull(isNullable).defaultValue(defaultVal);
                    }
                    case DECIMAL -> {
                        var defaultVal = rs.getString(4);
                        BigDecimal defVal = parseDecimal(defaultVal);
                        yield DecimalProperty.builder()
                            .key(propertyKey)
                            .name(nameDescr.left())
                            .description(nameDescr.right())
                            .notNull(isNullable)
                            .defaultValue(defVal)
                            .precision(rs.getInt(6))
                            .scale(rs.getInt(7));
                    }
                    case INTEGER -> {
                        long columnDefValue = rs.getLong(4);
                        var defaultVal = rs.wasNull() ? null : columnDefValue;
                        yield IntegerProperty.builder().key(propertyKey).name(nameDescr.left())
                            .description(nameDescr.right()).notNull(isNullable).defaultValue(defaultVal);
                    }
                };
                propertyKeyToBuilder.put(propertyKey, builder);
            }

            var builder = propertyKeyToBuilder.get(propertyKey);
            // constraints processing
            var constraintId = rs.getInt(9);
            if (processedConstraints.contains(constraintId)) {
                continue;
            }

            var constraintDefinition = rs.getString(8);
            if (constraintDefinition == null) {
                continue;
            }

            addConstraint(builder, type, constraintDefinition);
            processedConstraints.add(constraintId);
        }

        // unsafe cast in fact safe, build returns (? extends Property), though compile sees just ?
        return Return.of((List<Property>) propertyKeyToBuilder.values().stream().map(Property.Builder::build)
            .sorted(Comparator.comparing((Property s) -> s.key())).toList());
    }

    private static void addConstraint(
        Property.Builder<?> builder,
        Property.Type type,
        String constraintDefinition

    ) throws SQLException {
        if (constraintDefinition.startsWith("UNIQUE")) {
            var constraint = parseUniqueConstraint(constraintDefinition);
            builder.addConstraint(constraint);
            return;
        }

        if (constraintDefinition.startsWith("CHECK")) {
            var constraint = parseCheckExpression(type, constraintDefinition);
            builder.addConstraint(constraint);
            return;
        }

        if (constraintDefinition.startsWith("FOREIGN KEY")) {
            parseFkConsAndAddToBuilder((ReferenceProperty.ReferencePropertyBuilder) builder, constraintDefinition);
            return;
        }

        log.error("Unexpected constraint definition: " + constraintDefinition);
        throw new FatalError("Unexpected constraint appearance");
    }

    private static UniqueConstraint parseUniqueConstraint(String expression) {
        try {
            var exp = CCJSqlParserUtil.parseExpression(expression.substring("UNIQUE ".length()));
            if (exp instanceof ParenthesedExpressionList<?> pel) {
                var columns = pel.stream().map(p -> (((Column) p).getColumnName())).toList();
                return UniqueConstraint.of(columns);
            }

            log.error("Expected unique expression, but got {}", exp);
            throw new FatalError("Expected unique expression, but did not receive any");
        } catch (JSQLParserException e) {
            log.error("Failed to parse unique expression", e);
            throw new FatalError("Failed to parse unique expression");
        }
    }

    private static Property.Type parseType(String type) {
        return switch (type) {
            case "text" -> Property.Type.STRING;
            case "int8" -> Property.Type.INTEGER;
            case "int4" -> Property.Type.REFERENCE;
            case "numeric" -> Property.Type.DECIMAL;
            case "bool" -> Property.Type.BOOLEAN;
            default -> throw new FatalError("Unexpected type: " + type);
        };
    }

    private static Pair<String, String> parseComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        String name = null;
        String description = null;
        try (var p = jsonFactory.createParser(comment)) {
            while (p.nextToken() != JsonToken.END_OBJECT) {
                if (p.currentToken() == JsonToken.FIELD_NAME) {
                    if (p.getText().equals("name") && p.nextToken() == JsonToken.VALUE_STRING) {
                        name = p.getValueAsString();
                        continue;
                    }

                    if (p.getText().equals("description") && p.nextToken() == JsonToken.VALUE_STRING) {
                        description = p.getValueAsString();
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Pair<>(name, description);
    }

    // this is very basic parsing, for supporting more complex constraint need to use more detailed parsing
    public static PropertyConstraint parseCheckExpression(Property.Type type, String constraintExpression) {
        try {
            if (!constraintExpression.startsWith("CHECK")) {
                throw new FatalError("Unexpected expression, expected CHECK(...) expression, got " + constraintExpression);
            }
            var expression = CCJSqlParserUtil.parseExpression(constraintExpression.substring("CHECK".length()));
            if (!(expression instanceof ParenthesedExpressionList<?>)) {
                log.error("Unexpected expression, expected CHECK(...) expression, got " + expression);
                throw new FatalError("Unexpected constraint");
            }
            expression = ((ParenthesedExpressionList<?>) expression).get(0);
            switch (expression) {
                case GreaterThan gt -> {
                    return switch (type) {
                        case REFERENCE, INTEGER -> PropertyConstraint.greaterThan(gt.getRightExpression(LongValue.class).getValue());
                        case DECIMAL -> PropertyConstraint.greaterThan(
                            BigDecimal.valueOf(gt.getRightExpression(DoubleValue.class).getValue()));
                        case BOOLEAN -> PropertyConstraint.greaterThan(gt.getRightExpression(BooleanValue.class).getValue());
                        case STRING -> PropertyConstraint.greaterThan(gt.getRightExpression(StringValue.class).getValue());
                    };

                }
                case GreaterThanEquals gte -> {
                    return type == Property.Type.STRING ? getStringConstraint(gte) : getNonStringConstraint(type, gte);
                }
                case MinorThanEquals mte -> {
                    return type == Property.Type.STRING ? getStringConstraint(mte) : getNonStringConstraint(type, mte);
                }
                case MinorThan mt -> {
                    return switch (type) {
                        case REFERENCE, INTEGER -> PropertyConstraint.lessThan(mt.getLeftExpression(LongValue.class).getValue());
                        case DECIMAL -> PropertyConstraint.lessThan(
                            BigDecimal.valueOf(mt.getLeftExpression(DoubleValue.class).getValue()));
                        case BOOLEAN -> PropertyConstraint.lessThan(mt.getLeftExpression(BooleanValue.class).getValue());
                        case STRING -> PropertyConstraint.lessThan(mt.getLeftExpression(StringValue.class).getValue());
                    };
                }
                // in/not in are compiled to postgres column = ANY (ARRAY['option1'::, 'option2'::])
                case EqualsTo et -> {
                    if (et.getRightExpression() instanceof Function f) {
                        var arrConst = (ArrayConstructor) f.getParameters().get(0);
                        var valueExpressions = arrConst.getExpressions().stream();
                        var arrayOfPossibleValues = switch (type) {
                            case INTEGER -> valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(Long[]::new);
                            case REFERENCE -> valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                            .toArray(Integer[]::new);
                            case DECIMAL -> valueExpressions.map(
                                    ce -> BigDecimal.valueOf(((DoubleValue) ((CastExpression) ce).getLeftExpression()).getValue()))
                                .toArray(BigDecimal[]::new);
                            case BOOLEAN -> valueExpressions.map(ce -> ((BooleanValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(Boolean[]::new);
                            case STRING -> valueExpressions.map(ce -> ((StringValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(String[]::new);
                        };

                        return PropertyConstraint.in(arrayOfPossibleValues);
                    }

                    log.error("Unexpected equals constraint {}", et);
                    throw new FatalError("Unexpected constraint");
                }
                case NotEqualsTo net -> {
                    if (net.getRightExpression() instanceof Function f) {
                        var arrConst = (ArrayConstructor) f.getParameters().get(0);
                        var valueExpressions = arrConst.getExpressions().stream();
                        var arrayOfPossibleValues = switch (type) {
                            case INTEGER -> valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(
                                    Long[]::new);
                            case REFERENCE -> valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(
                                    Integer[]::new);
                            case DECIMAL -> valueExpressions.map(
                                    ce -> BigDecimal.valueOf(((DoubleValue) ((CastExpression) ce).getLeftExpression()).getValue()))
                                .toArray(BigDecimal[]::new);
                            case BOOLEAN -> valueExpressions.map(ce -> ((BooleanValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(Boolean[]::new);
                            case STRING -> valueExpressions.map(ce -> ((StringValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                .toArray(String[]::new);

                        };

                        return PropertyConstraint.notIn(arrayOfPossibleValues);
                    }

                    log.error("Unexpected not equals constraint {}", net);
                    throw new FatalError("Unexpected constraint");
                }
                default -> {
                    log.error("Unexpected expression {}", expression);
                    throw new FatalError("Unexpected check constraint expression " + type);
                }
            }
        } catch (JSQLParserException e) {
            log.error("Failed to parse expression: " + constraintExpression, e);
            throw new FatalError("Failed to parse expression from the existing schema");
        } catch (ClassCastException e) {
            log.error("Unexpected class cast, can it be that database is somehow in invalid state? ", e);
            throw new FatalError("Failed to parse expression from the existing schema");
        }
    }

    private static void parseFkConsAndAddToBuilder(
        ReferenceProperty.ReferencePropertyBuilder builder,
        String constraintDefinition
    ) {
        // JSqlParser can not parse just a single constraint, thus we attach it to dummy alter table and parse it this way
        var cons = "ALTER TABLE dummy ADD CONSTRAINT dum " + constraintDefinition;
        try {
            var exp = (Alter) CCJSqlParserUtil.parse(cons);
            var fkIndex = (ForeignKeyIndex) exp.getAlterExpressions().get(0).getIndex();
            builder.refTableKey(fkIndex.getTable().getName());
        } catch (JSQLParserException e) {
            log.error("Failed to dummy foreign key constraint {}", cons);
            throw new FatalError("Unexpected foreign key constraint");
        }
    }

    private static PropertyConstraint getStringConstraint(GreaterThanEquals gte) {
        // char_length(column_name) > constant i.e min_length
        if (gte.getLeftExpression() instanceof Function f && f.getName().equals("char_length")) {
            return PropertyConstraint.minLength((int) gte.getRightExpression(LongValue.class).getValue());
        }

        if (gte.getLeftExpression() instanceof Column) {
            return PropertyConstraint.greaterOrEqual(gte.getRightExpression(LongValue.class).getValue());
        }

        log.error("Unexpected constraint expression: {}", gte);
        throw new FatalError("Unexpected constraint expression");
    }

    private static PropertyConstraint getStringConstraint(MinorThanEquals mte) {
        // char_length(column_name) < constant i.e max_length
        if (mte.getLeftExpression() instanceof Function f && f.getName().equals("char_length")) {
            return PropertyConstraint.maxLength((int) mte.getRightExpression(LongValue.class).getValue());
        }

        if (mte.getLeftExpression() instanceof Column) {
            return PropertyConstraint.lessOrEqual(mte.getRightExpression(LongValue.class).getValue());
        }

        log.error("Unexpected constraint expression: {}", mte);
        throw new FatalError("Unexpected constraint expression");
    }

    private static PropertyConstraint getNonStringConstraint(Property.Type type, GreaterThanEquals gte) {
        return switch (type) {
            case REFERENCE, INTEGER -> PropertyConstraint.greaterOrEqual(gte.getRightExpression(LongValue.class).getValue());
            case DECIMAL -> PropertyConstraint.greaterOrEqual(BigDecimal.valueOf(gte.getRightExpression(DoubleValue.class).getValue()));
            case BOOLEAN -> PropertyConstraint.greaterOrEqual(gte.getRightExpression(BooleanValue.class).getValue());
            case STRING -> throw new IllegalStateException(
                "String property is not expected here, is there a race condition?");
        };
    }

    private static PropertyConstraint getNonStringConstraint(Property.Type type, MinorThanEquals mte) {
        return switch (type) {
            case REFERENCE, INTEGER -> PropertyConstraint.lessOrEqual(mte.getRightExpression(LongValue.class).getValue());
            case DECIMAL -> PropertyConstraint.lessOrEqual(
                BigDecimal.valueOf(mte.getRightExpression(DoubleValue.class).getValue()));
            case BOOLEAN -> PropertyConstraint.lessOrEqual(mte.getRightExpression(BooleanValue.class).getValue());
            case STRING -> throw new IllegalStateException(
                "String property is not expected here, is there a race condition?");
        };
    }

    public static BigDecimal parseDecimal(String decimal) {
        return "NULL::numeric".equals(decimal) ? null : new BigDecimal(decimal);
    }

}
