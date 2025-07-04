package edu.ukma.smart.virtual;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.properties.BooleanProperty;
import edu.ukma.smart.virtual.properties.DecimalProperty;
import edu.ukma.smart.virtual.properties.IntegerProperty;
import edu.ukma.smart.virtual.properties.Property;
import edu.ukma.smart.virtual.properties.ReferenceProperty;
import edu.ukma.smart.virtual.properties.StringProperty;
import java.math.BigDecimal;
import java.util.Arrays;
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
        List<Property<?>> properties = Arrays.asList(
            StringProperty.builder()
                .key("name")
                .name("Name")
                .description("User name")
                .defaultValue("default_name")
                .isRequired(true)
                .isUnique(false)
                .minLength(1)
                .maxLength(50)
                .build(),
            IntegerProperty.builder()
                .key("age")
                .name("Age")
                .description("User age")
                .defaultValue(25L)
                .isRequired(false)
                .isUnique(false)
                .min(0L)
                .max(120L)
                .build(),
            BooleanProperty.builder()
                .key("active")
                .name("Active")
                .description("Is user active")
                .defaultValue(true)
                .isRequired(false)
                .isUnique(false)
                .build(),
            DecimalProperty.builder()
                .key("salary")
                .name("Salary")
                .description("User salary")
                .defaultValue(new BigDecimal("50000.00"))
                .isRequired(false)
                .isUnique(false)
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
                .isRequired(true)
                .isUnique(false)
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
            {"user/*comment*/"}  // SQL comment injection
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
                .isRequired(true)
                .isUnique(false)
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
                .isRequired(true)
                .isUnique(false)
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
                .isRequired(true)
                .isUnique(false)
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
                .isRequired(true)
                .isUnique(false)
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
                .isRequired(true)
                .isUnique(false)
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
                .isRequired(true)
                .isUnique(false)
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
                .isRequired(false)
                .isUnique(false)
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
                .isRequired(false)
                .isUnique(false)
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
}
