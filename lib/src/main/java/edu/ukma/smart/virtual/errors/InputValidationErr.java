package edu.ukma.smart.virtual.errors;

public record InputValidationErr(ErrorCode code) implements Err {

    public static InputValidationErr of(ErrorCode err) {
        return new InputValidationErr(err);
    }

    public enum ErrorCode {
        WRONG_TABLE_KEY_FORMAT("table.key.format.is.wrong"),
        FORBIDDEN_PROPERTY_KEY("property.key.is.forbidden"),
        WRONG_PROPERTY_KEY_FORMAT("property.key.format.is.wrong"),
        WRONG_ROW_ID_FORMAT("table.row.id.format.is.wrong"),

        // create table specific
        CREATE_TABLE_EMPTY_NAME_FOR_TABLE("create.table.name.is.empty"),
        CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY("create.table.property.is.empty"),

        // alter table specific
        ALTER_TABLE_PROPERTY_IS_NOT_SET("alter.table.property.is.not.set"),

        // add row specific
        ADD_ROW_LISTS_ARE_NOT_SUPPORTED("add.row.lists.are.not.supported"),

        // update row specific
        UPDATE_ROW_NO_PROPERTIES("update.row.no.properties.set"),
        UPDATE_ROW_LISTS_ARE_NOT_SUPPORTED("update.row.lists.are.not.supported"),

        // select specific
        //  predicates:
        SELECT_PREDICATE_VALUE_IS_EMPTY("select.predicate.value.is.empty"),
        COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY("select.compound.predicate.left.left.value.is.empty"),
        COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY("select.compound.predicate.right.right.value.is.empty"),
        COMPOUND_PREDICATE_OPERATOR_IS_EMPTY("operator.is.empty"),
        // values
        LIST_VALUE_MISSING_TYPE("list.value.type.is.empty"),

        // property conditions
        // strings
        STRING_MAX_LEN_LESS_MIN_LEN("string.property.max.len.greater.min.len"),
        STRING_MAX_LEN_LESS_ONE("string.property.max.len.less.one"),
        STRING_MIN_LEN_LESS_ZERO("string.property.min.len.less.zero"),

        // references
        WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT("reference.property.table.key.format.is.wrong"),

        // integers
        INTEGER_MAX_VAL_LESS_MIN_VAL("integer.max.less.min"),
        INTEGER_DEFAULT_LESS_MIN_VAL("integer.default.less.min"),
        INTEGER_DEFAULT_GREATER_MAX_VAL("integer.default.greater.max"),

        // decimals
        DECIMAL_PRECISION_IS_INVALID("decimal.precision.is.invalid"),
        DECIMAL_SCALE_IS_INVALID("decimal.scale.is.invalid"),
        DECIMAL_MAX_VAL_LESS_MIN_VAL("decimal.max.less.min"),
        DECIMAL_DEFAULT_LESS_MIN_VAL("decimal.default.less.min"),
        DECIMAL_DEFAULT_GREATER_MAX_VAL("DECIMAL.default.greater.max");

        public final String errorCode;

        ErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
    }
}
