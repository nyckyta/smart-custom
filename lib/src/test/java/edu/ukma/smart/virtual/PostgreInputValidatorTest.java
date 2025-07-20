package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_OPERATOR_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_TABLE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_DEFAULT_GREATER_MAX_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_DEFAULT_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_MAX_VAL_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_PRECISION_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_SCALE_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.FORBIDDEN_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.INTEGER_DEFAULT_GREATER_MAX_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.INTEGER_DEFAULT_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.INTEGER_MAX_VAL_LESS_MIN_VAL;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.LIST_VALUE_MISSING_TYPE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.UPDATE_ROW_NO_PROPERTIES;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_ROW_ID_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.CompoundPredicate;
import edu.ukma.smart.virtual.dml.select.SelectProperty;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.select.StringPredicate;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.dml.values.ListValue;
import edu.ukma.smart.virtual.dml.values.StringValue;
import java.math.BigDecimal;
import java.util.List;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PostgreInputValidatorTest {

    private final PostgreInputValidator validator = new PostgreInputValidator();

    // NewTable validation tests
    @DataProvider(name = "newTableNullValues")
    Object[][] newTableNullValues() {
        return new Object[][] {
            {new NewTable(null, "ValidName", "", List.of()), WRONG_TABLE_KEY_FORMAT},
            {new NewTable("_valid_key", null, "", List.of()), CREATE_TABLE_EMPTY_NAME_FOR_TABLE},
            {new NewTable("_valid_key", "", "", List.of()), CREATE_TABLE_EMPTY_NAME_FOR_TABLE},
            {new NewTable("_valid_key", "   ", "", List.of()), CREATE_TABLE_EMPTY_NAME_FOR_TABLE},
            {new NewTable("_valid_key", "\t", "", List.of()), CREATE_TABLE_EMPTY_NAME_FOR_TABLE},
            {new NewTable("_valid_key", "\n", "", List.of()), CREATE_TABLE_EMPTY_NAME_FOR_TABLE},
            {new NewTable("_valid_key", "\r\n", "", List.of()), CREATE_TABLE_EMPTY_NAME_FOR_TABLE}
        };
    }

    @Test(dataProvider = "newTableNullValues")
    void testNewTableNullValuesAreNotAllowed(NewTable newTable, InputValidationErr.ErrorCode expectedError) {
        var result = validator.validateNewTable(newTable);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + newTable);
        assertEquals(result.get().code(), expectedError);
    }

    @DataProvider(name = "invalidTableKey")
    Object[][] invalidTableKey() {
        return new Object[][] {
            {null},
            {"invalid"},
            {"_"},
            {"_9invalid"},
            {"_INVALID"},
            {"_invalid-key"},
            {"_invalid.key"},
            {"_a" + "b".repeat(63)}, // too lon,
            {"__invalid"}
        };
    }

    @Test(dataProvider = "invalidTableKey")
    void testNewTableInvalidKeyFormatsAreRejected(String key) {
        var newTable = new NewTable(key, "ValidName", "", List.of());
        var result = validator.validateNewTable(newTable);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + newTable);
        assertEquals(result.get().code(), WRONG_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "invalidTableKey")
    void testDeleteRowNullValuesAreNotAllowed(String key) {
        var delete = new DeleteRow(key, 1);
        var result = validator.validateDeleteRow(delete);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + delete);
        assertEquals(result.get().code(), WRONG_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "invalidTableKey")
    void testDeleteTableNullValuesAreNotAllowed(String key) {
        var delete = new DropTable(key);
        var result = validator.validateDeleteTable(delete);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + delete);
        assertEquals(result.get().code(), WRONG_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "invalidTableKey")
    void testSelectWithInvalidKeysIsNotAllowed(String key) {
        var selectQuery = SelectQuery.wildcard(key);
        var result = validator.validateSelectQuery(selectQuery);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + selectQuery);
        assertEquals(result.get().code(), WRONG_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "invalidTableKey")
    void testInsertRowNullValuesAreNotAllowed(String key) {
        var row = InsertRow.of(key, List.of());
        var result = validator.validateInsertRow(row);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + row);
        assertEquals(result.get().code(), WRONG_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "invalidTableKey")
    void testUpdateRowCantBeDoneWithInvalidKey(String key) {
        var update = UpdateRow.of(key, 1, List.of(StringValue.of("one", "1")));
        var result = validator.validateUpdateRow(update);
        assertTrue(result.isPresent(), "Expected error, but no error returned, parameter: " + update);
        assertEquals(result.get().code(), WRONG_TABLE_KEY_FORMAT);
    }

    @DataProvider(name = "newTableValidKeys")
    Object[][] newTableValidKeys() {
        return new Object[][] {
            {new NewTable("_valid_key", "ValidName", "", List.of())},
            {new NewTable("_ab", "ValidName", "", List.of())},
            {new NewTable("_valid_key_with_underscores", "ValidName", "", List.of())},
            {new NewTable("_" + "a".repeat(62), "ValidName", "", List.of())} // max length
        };
    }

    @Test(dataProvider = "newTableValidKeys")
    void testNewTableValidKeyFormatsAreAccepted(NewTable newTable) {
        var result = validator.validateNewTable(newTable);
        assertTrue(result.isEmpty(), "Expected no errors, but got %s; Params: %s".formatted(result.orElse(null), newTable));
    }

    @DataProvider(name = "deleteRowInvalidRowIds")
    Object[][] deleteRowInvalidRowIds() {
        return new Object[][] {
            {new DeleteRow("_valid_key", 0)},
            {new DeleteRow("_valid_key", -1)},
            {new DeleteRow("_valid_key", -100)}
        };
    }

    @Test(dataProvider = "deleteRowInvalidRowIds")
    void testDeleteRowInvalidRowIdsAreRejected(DeleteRow deleteRow) {
        var result = validator.validateDeleteRow(deleteRow);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), WRONG_ROW_ID_FORMAT);
    }

    @DataProvider(name = "deleteRowValidValues")
    Object[][] deleteRowValidValues() {
        return new Object[][] {
            {new DeleteRow("_valid_key", 1)},
            {new DeleteRow("_valid_key", 100)},
            {new DeleteRow("_valid_key", Integer.MAX_VALUE)}
        };
    }

    @Test(dataProvider = "deleteRowValidValues")
    void testDeleteRowValidValuesAreAccepted(DeleteRow deleteRow) {
        var result = validator.validateDeleteRow(deleteRow);
        assertTrue(result.isEmpty());
    }

    // UpdateRow validation tests
    @DataProvider(name = "updateRowNullValues")
    Object[][] updateRowNullValues() {
        return new Object[][] {
            {new UpdateRow("_valid_key", 1, null), UPDATE_ROW_NO_PROPERTIES},
            {new UpdateRow("_valid_key", 1, List.of()), UPDATE_ROW_NO_PROPERTIES}
        };
    }

    @Test(dataProvider = "updateRowNullValues")
    void testUpdateRowNullValuesAreNotAllowed(UpdateRow updateRow, InputValidationErr.ErrorCode expectedError) {
        var result = validator.validateUpdateRow(updateRow);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), expectedError);
    }

    @DataProvider(name = "updateRowInvalidRowIds")
    Object[][] updateRowInvalidRowIds() {
        return new Object[][] {
            {new UpdateRow("_valid_key", 0, List.of(new StringValue("test", "value")))},
            {new UpdateRow("_valid_key", -1, List.of(new StringValue("test", "value")))}
        };
    }

    @Test(dataProvider = "updateRowInvalidRowIds")
    void testUpdateRowInvalidRowIdsAreRejected(UpdateRow updateRow) {
        var result = validator.validateUpdateRow(updateRow);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), WRONG_ROW_ID_FORMAT);
    }

    // ColumnValue validation tests
    @DataProvider(name = "columnValueNullKeys")
    Object[][] columnValueNullKeys() {
        return new Object[][] {
            {new StringValue(null, "value"), WRONG_PROPERTY_KEY_FORMAT}
        };
    }

    @Test
    void testColumnValueNullKeysAreNotAllowed() {
        var result = validator.validateColumnValue(new StringValue(null, "value"));
        assertTrue(result.isPresent(), "Expected no errors, but got %s".formatted(result.orElse(null)));
        assertEquals(result.get().code(), WRONG_PROPERTY_KEY_FORMAT);
    }

    @DataProvider(name = "columnValueInvalidKeys")
    Object[][] columnValueInvalidKeys() {
        return new Object[][] {
            {new StringValue("", "value")},
            {new StringValue("Invalid", "value")},
            {new StringValue("9invalid", "value")},
            {new StringValue("invalid-key", "value")},
            {new StringValue("invalid.key", "value")},
            {new StringValue("a" + "b".repeat(63), "value")} // too long
        };
    }

    @Test(dataProvider = "columnValueInvalidKeys")
    void testColumnValueInvalidKeyFormatsAreRejected(ColumnValue<?> columnValue) {
        var result = validator.validateColumnValue(columnValue);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), WRONG_PROPERTY_KEY_FORMAT);
    }

    @DataProvider(name = "columnValueForbiddenKeys")
    Object[][] columnValueForbiddenKeys() {
        return new Object[][] {
            {new StringValue("tableoid", "value")},
            {new StringValue("xmin", "value")},
            {new StringValue("cmin", "value")},
            {new StringValue("xmax", "value")},
            {new StringValue("cmax", "value")},
            {new StringValue("ctid", "value")}
        };
    }

    @Test(dataProvider = "columnValueForbiddenKeys")
    void testColumnValueForbiddenKeysAreRejected(ColumnValue<?> columnValue) {
        var result = validator.validateColumnValue(columnValue);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), FORBIDDEN_PROPERTY_KEY);
    }

    @Test
    void testListValueMissingTypeIsNotAllowed() {
        var result = validator.validateColumnValue(new ListValue<>("valid_key", List.of("value1", "value2"), null));
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), LIST_VALUE_MISSING_TYPE);
    }

    // Property validation tests
    @DataProvider(name = "invalidPropertyKeys")
    Object[][] invalidPropertyKeys() {
        return new Object[][] {
            {null},
            {"_invalid_key"},
            {"--invalid_key"},
            {"_invalid.key"},
            {"_invalid/*key"},
            {"_invalid//key"},
            {"invalid-key"}
        };
    }

    @Test(dataProvider = "invalidPropertyKeys")
    void testPropertiesWithInvalidKeysAreNotAllowed(String pKey) {
        var result = validator.validateProperty(StringProperty.builder().key(pKey).name("valid_name").build());
        assertTrue(result.isPresent(), "Expected error, but found no errors; Params: %s".formatted(pKey));
        assertEquals(result.get().code(), WRONG_PROPERTY_KEY_FORMAT);
    }

    @DataProvider(name = "propertyForbiddenKeys")
    Object[][] propertyForbiddenKeys() {
        return new Object[][] {
            {"_id"},
            {"_created"},
            {"tableoid"},
            {"xmin"},
            {"cmin"},
            {"xmax"},
            {"cmax"},
            {"ctid"},
        };
    }

    @Test(dataProvider = "propertyForbiddenKeys")
    void testPropertyForbiddenKeysAreRejected(String forbiddenKey) {
        var result = validator.validateProperty(StringProperty.builder().key(forbiddenKey).name("valid_name").build());
        assertTrue(result.isPresent(), "Expected error, but found no errors; Params: %s".formatted(forbiddenKey));
        assertEquals(result.get().code(), FORBIDDEN_PROPERTY_KEY);
    }

    // DecimalProperty validation tests
    @DataProvider(name = "decimalPropertyInvalidPrecision")
    Object[][] decimalPropertyInvalidPrecision() {
        return new Object[][] {
            {new DecimalProperty("valid_key", "ValidName", "",
                null, false, false, 0, 2, null, null)},
            {new DecimalProperty("valid_key", "ValidName", "",
                null, false, false, -1, 2, null, null)},
            {new DecimalProperty("valid_key", "ValidName", "",
                null, false, false, 131073, 2, null, null)} // MAX_PRECISION + 1
        };
    }

    @Test(dataProvider = "decimalPropertyInvalidPrecision")
    void testDecimalPropertyInvalidPrecisionIsRejected(DecimalProperty decimalProperty) {
        var result = validator.validateProperty(decimalProperty);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), DECIMAL_PRECISION_IS_INVALID);
    }

    @DataProvider(name = "decimalPropertyInvalidScale")
    Object[][] decimalPropertyInvalidScale() {
        return new Object[][] {
            {new DecimalProperty("valid_key", "ValidName", "",
                null, false, false, 10, 0, null, null)},
            {new DecimalProperty("valid_key", "ValidName", "",
                null, false, false, 10, -1, null, null)},
            {new DecimalProperty("valid_key", "ValidName", "",
                null, false, false, 20000, 16384, null, null)}
        };
    }

    @Test(dataProvider = "decimalPropertyInvalidScale")
    void testDecimalPropertyInvalidScaleIsRejected(DecimalProperty decimalProperty) {
        var result = validator.validateProperty(decimalProperty);
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), DECIMAL_SCALE_IS_INVALID);
    }

    @Test(dataProvider = "invalidTableKey")
    void testReferencePropertyNullTableKeyIsNotAllowed(
        String invalidReferenceTableKey
    ) {
        var result = validator.validateProperty(
            ReferenceProperty.builder().key("valid_key").name("ValidName").refTableKey(invalidReferenceTableKey).build());
        assertTrue(result.isPresent(), "Expected error, but found no errors; Params: %s".formatted(invalidReferenceTableKey));
        assertEquals(result.get().code(), WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "invalidPropertyKeys")
    void testSelectPropertyNullValuesAreNotAllowed(String invalidPropertyKeys) {
        var result = validator.validateSelectProperty(SelectProperty.of(invalidPropertyKeys));
        assertTrue(result.isPresent(), "Expected error, but found no errors; Params: %s".formatted(invalidPropertyKeys));
        assertEquals(result.get().code(), WRONG_PROPERTY_KEY_FORMAT);
    }

    @DataProvider(name = "selectPropertyStaticFields")
    Object[][] selectPropertyStaticFields() {
        return new Object[][] {
            {new SelectProperty("_id")},
            {new SelectProperty("_created")}
        };
    }

    @Test(dataProvider = "selectPropertyStaticFields")
    void testSelectPropertyStaticFieldsAreAllowed(SelectProperty selectProperty) {
        var result = validator.validateSelectProperty(selectProperty);
        assertTrue(result.isEmpty());
    }

    @DataProvider(name = "systemProperties")
    Object[][] systemProperties() {
        return new Object[][] {
            {"tableoid"},
            {"xmin"},
            {"cmin"},
            {"xmax"},
            {"cmax"},
            {"ctid"}
        };
    }

    @Test(dataProvider = "systemProperties")
    void testSelectPropertyForbiddenKeysAreRejected(String selectProperty) {
        var result = validator.validateSelectProperty(new SelectProperty(selectProperty));
        assertTrue(result.isPresent());
        assertEquals(result.get().code(), FORBIDDEN_PROPERTY_KEY);
    }


//    @Test(dataProvider = "stringPropertyInvalidConstraints")
//    void testStringPropertyInvalidConstraintsAreRejected(StringProperty stringProperty) {
//        var result = validator.validateProperty(stringProperty);
//        assertTrue(result.isPresent());
//        assertTrue(result.get().code() == STRING_MAX_LEN_LESS_ONE ||
//                   result.get().code() == STRING_MIN_LEN_LESS_ZERO ||
//                   result.get().code() == STRING_MAX_LEN_LESS_MIN_LEN);
//    }

    // Integer property validation tests
    @DataProvider(name = "integerPropertyInvalidConstraints")
    Object[][] integerPropertyInvalidConstraints() {
        return new Object[][] {
            {IntegerProperty.builder().key("valid_key").name("ValidName").min(10L).max(5L).build(),
                INTEGER_MAX_VAL_LESS_MIN_VAL},
            {IntegerProperty.builder().key("valid_key").name("ValidName").max(15L).defaultValue(20L).build(),
                INTEGER_DEFAULT_GREATER_MAX_VAL},
            {IntegerProperty.builder().key("valid_key").name("ValidName").min(15L).defaultValue(10L).build(),
                INTEGER_DEFAULT_LESS_MIN_VAL}
        };
    }

    @Test(dataProvider = "integerPropertyInvalidConstraints")
    void testIntegerPropertyInvalidConstraintsAreRejected(IntegerProperty integerProperty,
                                                          InputValidationErr.ErrorCode code) {
        var result = validator.validateProperty(integerProperty);
        assertTrue(result.isPresent(), "Expected error, but found no errors; Params: %s".formatted(integerProperty));
        assertEquals(result.get().code(), code);
    }

    // Decimal property validation tests from base validator
    @DataProvider(name = "decimalPropertyInvalidConstraints")
    Object[][] decimalPropertyInvalidConstraints() {
        return new Object[][] {
            {DecimalProperty.builder().key("valid_key").name("ValidName")
                .precision(10).scale(2).min(BigDecimal.valueOf(10.0)).max(BigDecimal.valueOf(5.0)).build(),
                DECIMAL_MAX_VAL_LESS_MIN_VAL},
            {DecimalProperty.builder().key("valid_key").name("ValidName")
                .precision(10).scale(2).defaultValue(BigDecimal.valueOf(10.0)).max(BigDecimal.valueOf(5.0)).build(),
                DECIMAL_DEFAULT_GREATER_MAX_VAL},
            {DecimalProperty.builder().key("valid_key").name("ValidName")
                .precision(10).scale(2).defaultValue(BigDecimal.valueOf(4.0)).min(BigDecimal.valueOf(5.0)).build(),
                DECIMAL_DEFAULT_LESS_MIN_VAL}
        };
    }

    @Test(dataProvider = "decimalPropertyInvalidConstraints")
    void testDecimalPropertyInvalidConstraintsAreRejected(
        DecimalProperty decimalProperty,
        InputValidationErr.ErrorCode err
    ) {
        var result = validator.validateProperty(decimalProperty);
        assertTrue(result.isPresent(), "Expected error, but found no errors; Params: %s".formatted(decimalProperty));
        assertEquals(result.get().code(), err);
    }

    @Test(dataProvider = "invalidPropertyKeys")
    void testInvalidPropertyNameIsRejectedOnPredicate(String invalidPropertyKey) {
        var err = validator.validatePredicate(StringPredicate.like(invalidPropertyKey, "123"));
        assertTrue(err.isPresent(), "Expected error, but found no errors; Params: %s".formatted(invalidPropertyKey));
        assertEquals(err.get().code(), WRONG_PROPERTY_KEY_FORMAT);
    }

    @Test(dataProvider = "systemProperties")
    void testPredicateIsRejectedOnSystemColumns(String systemProp) {
        var err = validator.validatePredicate(StringPredicate.like(systemProp, "123"));
        assertTrue(err.isPresent(), "Expected error, but found no errors; Params: %s".formatted(systemProp));
        assertEquals(err.get().code(), FORBIDDEN_PROPERTY_KEY);
    }

    @Test
    void testCompoundValidatorShouldNotHaveNulls() {
        var err = validator.validateCompoundPredicate(CompoundPredicate.and(null, null));
        assertTrue(err.isPresent(), "Expected error, but found no errors;");
        assertEquals(err.get().code(), COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY);

        err = validator.validateCompoundPredicate(
            CompoundPredicate.and(StringPredicate.like("string", "123"), null)
        );
        assertTrue(err.isPresent(), "Expected error, but found no errors;");
        assertEquals(err.get().code(), COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY);

        err = validator.validateCompoundPredicate(
            new CompoundPredicate(
                StringPredicate.like("string", "123"),
                StringPredicate.like("string", "123"),
                null
            )
        );

        assertTrue(err.isPresent(), "Expected error, but found no errors;");
        assertEquals(err.get().code(), COMPOUND_PREDICATE_OPERATOR_IS_EMPTY);
    }
}
