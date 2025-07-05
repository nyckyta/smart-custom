package edu.ukma.smart.virtual;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.properties.BooleanProperty;
import edu.ukma.smart.virtual.properties.DecimalProperty;
import edu.ukma.smart.virtual.properties.IntegerProperty;
import edu.ukma.smart.virtual.properties.Property;
import edu.ukma.smart.virtual.properties.ReferenceProperty;
import edu.ukma.smart.virtual.properties.StringProperty;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.ColumnValue;
import edu.ukma.smart.virtual.values.DecimalValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.ReferenceValue;
import edu.ukma.smart.virtual.values.StringValue;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PostgreQueryBuilderTest {

    private PostgreQueryBuilder queryBuilder;

    @BeforeMethod
    public void setUp() {
        queryBuilder = new PostgreQueryBuilder();
    }

    // ========== CREATE TABLE TESTS ==========

    @Test
    public void testCreateTableWithValidProperties() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("name")
                .name("Name")
                .description("User name")
                .defaultValue("default_name")
                .required(true)
                .unique(false)
                .minLength(1)
                .maxLength(50)
                .build(),
            IntegerProperty.builder()
                .key("age")
                .name("Age")
                .description("User age")
                .defaultValue(25L)
                .required(false)
                .unique(false)
                .min(0L)
                .max(120L)
                .build(),
            BooleanProperty.builder()
                .key("active")
                .name("Active")
                .description("Is user active")
                .defaultValue(true)
                .required(false)
                .unique(false)
                .build(),
            DecimalProperty.builder()
                .key("salary")
                .name("Salary")
                .description("User salary")
                .defaultValue(new BigDecimal("50000.00"))
                .required(false)
                .unique(false)
                .min(new BigDecimal("0.00"))
                .max(new BigDecimal("999999.99"))
                .precision(10)
                .scale(2)
                .build()
        );
        NewTable newTable = NewTable.builder().key("users").name("users").description("users")
            .properties(properties).build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        // Verify table structure
        assertTrue(sql.contains("CREATE TABLE public.users"));
        assertTrue(sql.contains("_id SERIAL PRIMARY KEY NOT NULL"));
        assertTrue(sql.contains("_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL"));

        // Verify columns
        assertTrue(sql.contains("name TEXT DEFAULT $$default_name$$ NOT NULL"));
        assertTrue(sql.contains("age BIGINT DEFAULT 25"));
        assertTrue(sql.contains("active BOOLEAN DEFAULT true"));
        assertTrue(sql.contains("salary NUMERIC(10,2) DEFAULT 50000.00"));

        // Verify constraints
        assertTrue(sql.contains("CHECK (char_length(name) BETWEEN 1 AND 50)"));
        assertTrue(sql.contains("CHECK (age BETWEEN 0 AND 120)"));
        assertTrue(sql.contains("CHECK (salary BETWEEN 0.000000 AND 999999.990000)"));
    }

    @Test
    public void testCreateTableWithNoProperties() {
        // Given
        NewTable newTable =
            new NewTable("empty_table", "empty_table", "empty_table", Collections.emptyList());

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithReferenceProperty() {
        // Given
        List<Property<?>> properties = List.of(
            ReferenceProperty.builder()
                .key("user_id")
                .name("User ID")
                .description("Reference to user")
                .required(true)
                .unique(false)
                .refTableKey("users")
                .build()
        );
        NewTable newTable = new NewTable("orders", "orders", "orders", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        assertTrue(sql.contains("user_id INTEGER NOT NULL"));
        assertTrue(sql.contains("FOREIGN KEY (user_id) REFERENCES users(_id)"));
    }

    // ========== SECURITY TESTS - TABLE KEY VALIDATION ==========

    @DataProvider(name = "invalidTableKeys")
    public Object[][] invalidTableKeys() {
        return new Object[][] {
            {"Users"},           // uppercase
            {"user-name"},       // hyphen
            {"user name"},       // space
            {"user@name"},       // special character
            {"1users"},          // starts with number
            {""},                // empty
            {"u"},               // too short (less than 2 chars)
            {"a".repeat(102)},   // too long (more than 100 chars)
            {"user's"},          // apostrophe
            {"user;drop"},       // semicolon (SQL injection attempt)
            // SQL comment injection
            {"user/*comment*/"},
            {"user;--"}
        };
    }

    @Test(dataProvider = "invalidTableKeys")
    public void testCreateTableWithInvalidTableKey(String invalidKey) {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("name")
                .name("Name")
                .description("User name")
                .defaultValue("default")
                .required(true)
                .unique(false)
                .minLength(1)
                .maxLength(50)
                .build()
        );
        NewTable newTable = new NewTable(invalidKey, invalidKey, invalidKey, properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @DataProvider(name = "validTableKeys")
    public Object[][] validTableKeys() {
        return new Object[][] {
            {"users"},
            {"user_profile"},
            {"user_data_log"},
            {"a".repeat(100)},  // exactly 100 chars
            {"ab"}              // exactly 2 chars
        };
    }

    @Test(dataProvider = "validTableKeys")
    public void testCreateTableWithValidTableKey(String validKey) {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("name")
                .name("Name")
                .description("User name")
                .defaultValue("default")
                .required(true)
                .unique(false)
                .minLength(1)
                .maxLength(50)
                .build()
        );
        NewTable newTable = new NewTable(validKey, validKey, validKey, properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        assertTrue(result.value().contains("CREATE TABLE public." + validKey));
    }

    // ========== SECURITY TESTS - PROPERTY KEY VALIDATION ==========

    @Test(dataProvider = "invalidTableKeys")
    public void testCreateTableWithInvalidPropertyKey(String invalidKey) {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key(invalidKey)
                .name("Name")
                .description("User name")
                .defaultValue("default")
                .required(true)
                .unique(false)
                .minLength(1)
                .maxLength(50)
                .build()
        );
        NewTable newTable = new NewTable("users", "users", "users", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
        assertEquals(result.error().get().getClass(), InputValidationErr.class);
    }

    // ========== PROPERTY VALIDATION TESTS ==========

    @Test
    public void testCreateTableWithRequiredPropertyWithoutDefault() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("name")
                .name("Name")
                .description("User name")
                .defaultValue(null) // required but no default
                .required(true)
                .unique(false)
                .minLength(1)
                .maxLength(50)
                .build()
        );
        NewTable newTable = new NewTable("users", "users", "users", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithReferencePropertyRequiredNoDefault() {
        // Given - Reference properties are exempt from the default value requirement
        List<Property<?>> properties = List.of(
            ReferenceProperty.builder()
                .key("user_id")
                .name("User ID")
                .description("Reference to user")
                .required(true)
                .unique(false)
                .refTableKey("users")
                .build()
        );
        NewTable newTable = new NewTable("orders", "orders", "orders", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
    }

    @Test
    public void testCreateTableWithInvalidReferenceTableKey() {
        // Given
        List<Property<?>> properties = List.of(
            ReferenceProperty.builder()
                .key("user_id")
                .name("User ID")
                .description("Reference to user")
                .required(true)
                .unique(false)
                .refTableKey("Invalid-Table")
                .build()
        );
        NewTable newTable = new NewTable("orders", "orders", "orders", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    // ========== DECIMAL PROPERTY VALIDATION ==========

    @Test
    public void testCreateTableWithInvalidDecimalPrecision() {
        // Given
        List<Property<?>> properties = List.of(
            DecimalProperty.builder()
                .key("price")
                .name("Price")
                .description("Product price")
                .defaultValue(new BigDecimal("100.00"))
                .required(false)
                .unique(false)
                .precision(0) // precision = 0
                .scale(2)
                .build()
        );
        NewTable newTable = new NewTable("products", "products", "products", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithInvalidDecimalScale() {
        // Given
        List<Property<?>> properties = List.of(
            DecimalProperty.builder()
                .key("price")
                .name("Price")
                .description("Product price")
                .defaultValue(new BigDecimal("100.00"))
                .required(false)
                .unique(false)
                .precision(10)
                .scale(0) // scale = 0
                .build()
        );
        NewTable newTable = new NewTable("products", "products", "products", properties);

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithStringMaxLengthLessThanMin() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("description")
                .name("Description")
                .defaultValue("default")
                .minLength(10)
                .maxLength(5) // max < min
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("items")
            .name("Items Table")
            .description("Test table for items")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithStringInvalidMaxLength() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("description")
                .name("Description")
                .defaultValue("default")
                .minLength(null)
                .maxLength(0) // max = 0
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("items")
            .name("Items Table")
            .description("Test table for items")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithStringInvalidMinLength() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("description")
                .name("Description")
                .defaultValue("default")
                .minLength(0) // min = 0
                .maxLength(null)
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("items")
            .name("Items Table")
            .description("Test table for items")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

// ========== DELETE TABLE TESTS ==========

    @Test
    public void testDeleteTableWithValidKey() {
        // When
        Return<String> result = queryBuilder.deleteTable("users");

        // Then
        assertTrue(result.error().isEmpty());
        assertEquals(result.value(), "DROP TABLE users;");
    }

    @Test(dataProvider = "invalidTableKeys")
    public void testDeleteTableWithInvalidKey(String invalidKey) {
        // When
        Return<String> result = queryBuilder.deleteTable(invalidKey);

        // Then
        assertTrue(result.error().isPresent());
    }

// ========== INSERT INTO TABLE TESTS ==========

    @Test
    public void testInsertIntoTableWithValidData() {
        // Given
        List<ColumnValue> columnValues = List.of(
            StringValue.of("name", "John Doe"),
            IntegerValue.of("age", 30L),
            BooleanValue.of("active", true),
            DecimalValue.of("salary", new BigDecimal("50000.00")),
            ReferenceValue.of("department_id", 1)
        );

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        assertTrue(sql.contains("INSERT INTO users"));
        assertTrue(sql.contains("(name,age,active,salary,department_id)"));
        assertTrue(sql.contains("VALUES ($$John Doe$$,30,true,50000.00,1)"));
    }

    @Test
    public void testInsertIntoTableWithNoColumns() {
        // When
        Return<String> result = queryBuilder.insertIntoTable("users", Collections.emptyList());

        // Then
        assertTrue(result.error().isEmpty());
        assertEquals(result.value(), "INSERT INTO users DEFAULT VALUES;");
    }

    @Test(dataProvider = "invalidTableKeys")
    public void testInsertIntoTableWithInvalidTableKey(String invalidKey) {
        // Given
        List<ColumnValue> columnValues = List.of(StringValue.of("name", "John Doe"));

        // When
        Return<String> result = queryBuilder.insertIntoTable(invalidKey, columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test(dataProvider = "invalidTableKeys")
    public void testInsertIntoTableWithInvalidColumnKey(String invalidKey) {
        // Given
        List<ColumnValue> columnValues = List.of(StringValue.of(invalidKey, "John Doe"));

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

// ========== SQL INJECTION PROTECTION TESTS ==========

    @Test
    public void testInsertWithSQLInjectionAttemptInStringValue() {
        // Given - Malicious SQL injection attempt in string value
        List<ColumnValue> columnValues = List.of(
            new StringValue("name", "'; DROP TABLE users; --")
        );

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        // Verify the malicious content is properly escaped with $$ quoting
        assertTrue(sql.contains("$$'; DROP TABLE users; --$$"));
        // Ensure it doesn't contain unescaped semicolons that could terminate the statement
        assertFalse(sql.matches(".*[^$]';.*"));
    }

    @Test
    public void testInsertWithSQLInjectionAttemptInTableKey() {
        // Given - Malicious SQL injection attempt in table key should be rejected
        List<ColumnValue> columnValues = List.of(
            new StringValue("name", "John")
        );

        // When
        Return<String> result =
            queryBuilder.insertIntoTable("users; DROP TABLE users; --", columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testInsertWithSQLInjectionAttemptInColumnKey() {
        // Given - Malicious SQL injection attempt in column key should be rejected
        List<ColumnValue> columnValues = List.of(
            new StringValue("name; DROP TABLE users; --", "John")
        );

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithSQLInjectionInStringDefault() {
        // Given - Test that string default values are properly escaped
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("description")
                .name("description")
                .defaultValue("'; DROP TABLE users; --")
                .minLength(1)
                .maxLength(100)
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("items")
            .name("Items Table")
            .description("Test table for items")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        // Verify the malicious content is properly escaped with $$ quoting
        assertTrue(sql.contains("$$'; DROP TABLE users; --$$"));
    }

// ========== DELETE FROM TABLE TESTS ==========

    @Test
    public void testDeleteFromTableWithValidData() {
        // When
        Return<String> result = queryBuilder.deleteFromTable("users", 123);

        // Then
        assertTrue(result.error().isEmpty());
        assertEquals(result.value(), "DELETE FROM users WHERE _id = 123;");
    }

    @Test(dataProvider = "invalidTableKeys")
    public void testDeleteFromTableWithInvalidTableKey(String invalidKey) {
        // When
        Return<String> result = queryBuilder.deleteFromTable(invalidKey, 123);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testDeleteFromTableWithNegativeId() {
        // When
        Return<String> result = queryBuilder.deleteFromTable("users", -1);

        // Then
        assertTrue(result.error().isPresent());
    }

// ========== UNIQUE AND NOT NULL CONSTRAINT TESTS ==========

    @Test
    public void testCreateTableWithUniqueConstraints() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("email")
                .name("email")
                .required(true)
                .unique(true)
                .defaultValue("default@example.com")
                .minLength(1)
                .maxLength(100)
                .build(),
            IntegerProperty.builder()
                .key("employee_id")
                .name("employee_id")
                .required(true)
                .unique(true)
                .defaultValue(1000L)
                .min(null)
                .max(null)
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("employees")
            .name("Employees Table")
            .description("Table for storing employee information")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        assertTrue(sql.contains("email TEXT DEFAULT $$default@example.com$$ NOT NULL UNIQUE"));
        assertTrue(sql.contains("employee_id BIGINT DEFAULT 1000 NOT NULL UNIQUE"));
    }

    @Test
    public void testCreateTableWithNullableColumns() {
        // Given
        List<Property<?>> properties = List.of(
            StringProperty.builder()
                .key("middle_name")
                .name("Middle Name Table")
                .required(false)
                .unique(false)
                .defaultValue(null)
                .minLength(1)
                .maxLength(50)
                .build(),
            IntegerProperty.builder()
                .key("optional_number")
                .name("Optional Number Table")
                .required(false)
                .unique(false)
                .defaultValue(null)
                .min(null)
                .max(null)
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("people")
            .name("People Table")
            .description("Table for storing people information")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        assertTrue(sql.contains("middle_name TEXT DEFAULT NULL"));
        assertTrue(sql.contains("optional_number BIGINT DEFAULT NULL"));
        // Should not contain NOT NULL for these columns
        assertFalse(sql.matches(".*middle_name.*NOT NULL.*"));
        assertFalse(sql.matches(".*optional_number.*NOT NULL.*"));
    }

// ========== BOUNDARY VALUE TESTS ==========

    @Test
    public void testCreateTableWithMaxPrecisionAndScale() {
        // Given
        List<Property<?>> properties = List.of(
            DecimalProperty.builder()
                .key("big_decimal")
                .name("Big Decimal Table")
                .required(false)
                .unique(false)
                .defaultValue(new BigDecimal("0"))
                .min(null)
                .max(null)
                .precision(131072) // MAX value
                .scale(16383) // MAX value
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("test_table")
            .name("Test Table")
            .description("Table for testing maximum precision and scale")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isEmpty());
        assertTrue(result.value().contains("NUMERIC(131072,16383)"));
    }

    @Test
    public void testCreateTableWithExceededPrecision() {
        // Given
        List<Property<?>> properties = List.of(
            DecimalProperty.builder()
                .key("big_decimal")
                .name("Big Decimal Table")
                .required(false)
                .unique(false)
                .defaultValue(new BigDecimal("0"))
                .min(null)
                .max(null)
                .precision(131073) // Exceeds MAX_PRECISION
                .scale(2)
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("test_table")
            .name("Test Table")
            .description("Table for testing precision validation")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithExceededScale() {
        // Given
        List<Property<?>> properties = List.of(
            DecimalProperty.builder()
                .key("big_decimal")
                .name("Big Decimal Table")
                .required(false)
                .unique(false)
                .defaultValue(new BigDecimal("0"))
                .min(null)
                .max(null)
                .precision(10)
                .scale(16384) // Exceeds MAX_SCALE
                .build()
        );
        NewTable newTable = NewTable.builder()
            .key("test_table")
            .name("Test Table")
            .description("Table for testing scale validation")
            .properties(properties)
            .build();

        // When
        Return<String> result = queryBuilder.createTable(newTable);

        // Then
        assertTrue(result.error().isPresent());
    }
}
