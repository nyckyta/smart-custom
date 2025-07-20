package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.ALTER_TABLE_PROPERTY_IS_NOT_SET;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_OPERATOR_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_TABLE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_DEFAULT_GREATER_MAX_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_DEFAULT_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_MAX_VAL_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.INTEGER_DEFAULT_GREATER_MAX_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.INTEGER_DEFAULT_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.INTEGER_MAX_VAL_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.LIST_VALUE_MISSING_TYPE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.SELECT_PREDICATE_VALUE_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.STRING_MAX_LEN_LESS_MIN_LEN;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.STRING_MAX_LEN_LESS_ONE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.STRING_MIN_LEN_LESS_ZERO;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.UPDATE_ROW_NO_PROPERTIES;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.Property;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.CompoundPredicate;
import edu.ukma.smart.virtual.dml.select.RawPredicate;
import edu.ukma.smart.virtual.dml.select.SelectProperty;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.dml.values.ListValue;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

/**
 * Provides basic entities validation.
 */
public interface InputValidator {

    default Optional<InputValidationErr> validateNewTable(NewTable newTable) {
        if (newTable.key() == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        if (newTable.name() == null || newTable.name().isBlank()) {
            return Optional.of(InputValidationErr.of(CREATE_TABLE_EMPTY_NAME_FOR_TABLE));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateAddProperty(AddProperty addProperty) {
        if (addProperty.tableKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        if (addProperty.property() == null) {
            return Optional.of(InputValidationErr.of(ALTER_TABLE_PROPERTY_IS_NOT_SET));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateDeleteRow(DeleteRow deleteRow) {
        if (deleteRow.tableKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateDeleteTable(DropTable deleteTable) {
        if (deleteTable.tableKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateSelectQuery(SelectQuery selectQuery) {
        if (selectQuery.tableKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateInsertRow(InsertRow insertRow) {
        if (insertRow.tableKey() == null || insertRow.tableKey().isBlank()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateUpdateRow(UpdateRow updateRow) {
        if (updateRow.tableKey() == null || updateRow.tableKey().isEmpty()) {
            return Optional.of(new InputValidationErr(WRONG_TABLE_KEY_FORMAT));
        }

        if (updateRow.valuesToUpdate() == null || updateRow.valuesToUpdate().isEmpty()) {
            return Optional.of(new InputValidationErr(UPDATE_ROW_NO_PROPERTIES));
        }
        return Optional.empty();
    }

    default Optional<InputValidationErr> validateColumnValue(ColumnValue<?> columnValue) {
        if (columnValue.key() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (columnValue instanceof ListValue<?> l) {
            if (l.elementsType() == null) {
                return Optional.of(InputValidationErr.of(LIST_VALUE_MISSING_TYPE));
            }
        }

        return Optional.empty();
    }

    default <T> Optional<InputValidationErr> validateProperty(Property p) {
        if (p.key() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (p.name() == null || p.name().isBlank()) {
            return Optional.of(InputValidationErr.of(CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY));
        }

        return switch (p.type()) {
            case DECIMAL -> validateDecimalProperty((DecimalProperty) p);
            case INTEGER -> validateIntegerProperty((IntegerProperty) p);
            case STRING -> validateStringProperty((StringProperty) p);
            case REFERENCE -> validateReferenceProperty((ReferenceProperty) p);
            case BOOLEAN -> Optional.empty();
            case null -> throw new NullPointerException();
        };
    }

    default Optional<InputValidationErr> validateSelectProperty(SelectProperty p) {
        if (p.propertyKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        return Optional.empty();
    }

    default <T> Optional<InputValidationErr> validatePredicate(RawPredicate<T> p) {
        if (p.propertyKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (p.value() == null) {
            return Optional.of(InputValidationErr.of(SELECT_PREDICATE_VALUE_IS_EMPTY));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateCompoundPredicate(CompoundPredicate p) {
        if (p.left() == null) {
            return Optional.of(InputValidationErr.of(COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY));
        }

        if (p.right() == null) {
            return Optional.of(InputValidationErr.of(COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY));
        }

        if (p.op() == null) {
            return Optional.of(InputValidationErr.of(COMPOUND_PREDICATE_OPERATOR_IS_EMPTY));
        }

        return Optional.empty();
    }

    static Optional<InputValidationErr> validateDecimalProperty(DecimalProperty dp) {
        boolean maxSet = dp.max() != null;
        boolean minSet = dp.min() != null;
        boolean defaultSet = dp.defaultValue() != null;

        if (defaultSet) {
            if (maxSet && dp.defaultValue().compareTo(dp.max()) > 0) {
                return Optional.of(
                    InputValidationErr.of(DECIMAL_DEFAULT_GREATER_MAX_VAL)
                );
            }

            if (minSet && dp.defaultValue().compareTo(dp.min()) < 0) {
                return Optional.of(InputValidationErr.of(DECIMAL_DEFAULT_LESS_MIN_VAL));
            }
        }

        if (minSet && maxSet) {
            if (dp.max().compareTo(dp.min()) < 0) {
                return Optional.of(InputValidationErr.of(DECIMAL_MAX_VAL_LESS_MIN_VAL));
            }
        }

        return Optional.empty();
    }

    static Optional<InputValidationErr> validateIntegerProperty(IntegerProperty ip) {
        boolean isMaxSet = ip.max() != null;
        boolean isMinSet = ip.min() != null;
        boolean defaultValueSet = ip.defaultValue() != null;

        if (defaultValueSet) {
            if (isMinSet && ip.defaultValue() < ip.min()) {
                return Optional.of(InputValidationErr.of(INTEGER_DEFAULT_LESS_MIN_VAL));
            }

            if (isMaxSet && ip.defaultValue() > ip.max()) {
                return Optional.of(InputValidationErr.of(INTEGER_DEFAULT_GREATER_MAX_VAL));
            }
        }


        if (isMinSet && isMaxSet && ip.max() < ip.min()) {
            return Optional.of(InputValidationErr.of(INTEGER_MAX_VAL_LESS_MIN_VAL));
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateStringProperty(StringProperty sp) {
        boolean maxLengthSet = sp.maxLength() != null;
        boolean minLengthSet = sp.minLength() != null;

        if (maxLengthSet && sp.maxLength() < 1) {
            return Optional.of(InputValidationErr.of(STRING_MAX_LEN_LESS_ONE));
        }

        if (minLengthSet && sp.minLength() < 0) {
            return Optional.of(InputValidationErr.of(STRING_MIN_LEN_LESS_ZERO));
        }

        if (minLengthSet && maxLengthSet) {
            if (sp.maxLength() < sp.minLength()) {
                return Optional.of(InputValidationErr.of(STRING_MAX_LEN_LESS_MIN_LEN));
            }
        }

        return Optional.empty();
    }

    default Optional<InputValidationErr> validateReferenceProperty(ReferenceProperty rp) {
        if (rp.refTableKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
