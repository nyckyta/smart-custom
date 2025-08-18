package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.create.Property;
import edu.ukma.smart.virtual.errors.FatalError;
import java.math.BigDecimal;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreUtils {

    private static final Logger log = LoggerFactory.getLogger(PostgreUtils.class);

    public static Property.Type parseType(String type) {
        return switch (type) {
            case "text" -> Property.Type.STRING;
            case "int8" -> Property.Type.INTEGER;
            case "int4" -> Property.Type.REFERENCE;
            case "numeric" -> Property.Type.DECIMAL;
            case "bool" -> Property.Type.BOOLEAN;
            default -> throw new FatalError("Unexpected type: " + type);
        };
    }

    public static BigDecimal parseDecimal(String decimal) {
        return "NULL::numeric".equals(decimal) ? null : new BigDecimal(decimal);
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
                        case REFERENCE, INTEGER ->
                            PropertyConstraint.greaterThan(gt.getRightExpression(LongValue.class).getValue());
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
                            case INTEGER ->
                                valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                    .toArray(
                                        Long[]::new);
                            case REFERENCE ->
                                valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                    .toArray(
                                        Integer[]::new);
                            case DECIMAL -> valueExpressions.map(
                                    ce -> BigDecimal.valueOf(((DoubleValue) ((CastExpression) ce).getLeftExpression()).getValue()))
                                .toArray(BigDecimal[]::new);
                            case BOOLEAN ->
                                valueExpressions.map(ce -> ((BooleanValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                    .toArray(Boolean[]::new);
                            case STRING ->
                                valueExpressions.map(ce -> ((StringValue) ((CastExpression) ce).getLeftExpression()).getValue())
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
                            case INTEGER ->
                                valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                    .toArray(
                                        Long[]::new);
                            case REFERENCE ->
                                valueExpressions.map(ce -> ((LongValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                    .toArray(
                                        Integer[]::new);
                            case DECIMAL -> valueExpressions.map(
                                    ce -> BigDecimal.valueOf(((DoubleValue) ((CastExpression) ce).getLeftExpression()).getValue()))
                                .toArray(BigDecimal[]::new);
                            case BOOLEAN ->
                                valueExpressions.map(ce -> ((BooleanValue) ((CastExpression) ce).getLeftExpression()).getValue())
                                    .toArray(Boolean[]::new);
                            case STRING ->
                                valueExpressions.map(ce -> ((StringValue) ((CastExpression) ce).getLeftExpression()).getValue())
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
            case DECIMAL -> PropertyConstraint.greaterOrEqual(
                BigDecimal.valueOf(gte.getRightExpression(DoubleValue.class).getValue()));
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

}


