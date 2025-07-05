package edu.ukma.smart.virtual;

import static org.testng.Assert.assertTrue;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.properties.BooleanProperty;
import edu.ukma.smart.virtual.properties.DecimalProperty;
import edu.ukma.smart.virtual.properties.IntegerProperty;
import edu.ukma.smart.virtual.properties.ReferenceProperty;
import edu.ukma.smart.virtual.properties.StringProperty;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.DecimalValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.ReferenceValue;
import edu.ukma.smart.virtual.values.StringValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

class DefaultVirtualTableServiceTest {

    private static final String DB_NAME = "test_db";

    private GenericContainer<?> container;
    private Connection connection;
    private DefaultVirtualTableService service;

    @BeforeClass
    void startContainer() throws IOException, InterruptedException, SQLException {
        container = new GenericContainer<>("postgres:latest")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "test");
        container.start();
        container.execInContainer("psql",
            "-U", "postgres",
            "-c", "CREATE DATABASE %s;".formatted(DB_NAME));
        connection = createConnection();
        service = new DefaultVirtualTableService(connection);
    }

    @AfterClass(alwaysRun = true)
    void stopContainer() throws SQLException {
        container.stop();
        connection.close();
    }

    @DataProvider
    Object[][] maliciousStringDefaultValues() {
        return new Object[][] {
            {"default_value'; DROP TABLE users; --"},
            {"default_value; DROP TABLE users; --"},
            {"default_value') OR 1=1; DROP TABLE users; --"},
            {"default_value\"; DROP TABLE users; --"},
        };
    }

    @DataProvider(name = "maliciousTableKeys")
    Object[][] maliciousTableKeys() {
        return new Object[][] {
            // Basic SQL injection attempts
            {"users'; DROP TABLE test; --"},
            {"users; DELETE FROM information_schema.tables; --"},
            {"users') UNION SELECT * FROM pg_user; --"},

            // Schema manipulation attempts
            {"users; CREATE TABLE malicious (id INT); --"},
            {"users; ALTER TABLE existing_table DROP COLUMN important_col; --"},
            {"users; GRANT ALL ON DATABASE TO public; --"},

            // Information disclosure attempts
            {"users'; SELECT * FROM pg_stat_activity; --"},
            {"users UNION SELECT version(); --"},
            {"users'; SELECT current_user; --"},

            // Special characters and encoding
            {"user\"; DROP TABLE test; --"},
            {"user\\\"; DROP TABLE test; --"},
            {"user%27; DROP TABLE test; --"},
            {"user'; INSERT INTO log VALUES ('hacked'); --"},

            // Case variations
            {"Users"}, // uppercase
            {"USERS"}, // all caps
            {"UsErS"}, // mixed case

            // Invalid patterns that should be rejected
            {"user-table"}, // contains dash
            {"user table"}, // contains space
            {"user@table"}, // contains @
            {"9users"}, // starts with number
            {"user.table"}, // contains dot
            {"user#table"}, // contains hash
            {"user$table"}, // contains dollar
            {"user%table"}, // contains percent
            {"user*table"}, // contains asterisk

            // Boundary testing
            {"u"}, // too short (less than 2 chars)
            {"a" + "b".repeat(101)}, // too long (over 100 chars)
            {""}, // empty string
            {" users"}, // leading space
            {"users "}, // trailing space
            {"_users"}, // starts with underscore
        };
    }

    @DataProvider(name = "maliciousPropertyKeys")
    Object[][] maliciousPropertyKeys() {
        return new Object[][] {
            {"name'; DROP TABLE users; --"},
            {"name; DELETE FROM pg_tables; --"},
            {"name') OR 1=1; --"},
            {"name\"; UPDATE users SET admin=true; --"},
            {"Name"}, // uppercase
            {"name-field"}, // contains dash
            {"name field"}, // contains space
            {"1name"}, // starts with number
            {"name.field"}, // contains dot
        };
    }

    @DataProvider(name = "invalidMinMaxStringLengthInput")
    Object[][] invalidMinMaxLength() {
        return new Object[][] {
            {0, 0}, // both invalid
            {null, -1}, // unset, invalid
            {0, null}, // invalid, unset
            {1, 0}, // valid, invalid
            {-1, 1}, // invalid, valid
            {-1, -1}, // invalid-negative, invalid negative
            {10, 9} // valid more than max, valid less than min
        };
    }


    @Test(dataProvider = "maliciousTableKeys")
    void testErrorOnInvalidTableKey(String maliciousKey) throws SQLException {
        final var maliciousTable = new NewTable(
            maliciousKey,
            "Malicious Table",
            "This table has a malicious key",
            List.of()
        );

        Optional<? extends Err> result = service.createTable(maliciousTable);
        // Assert
        assertTrue(result.isPresent(),
            "Expected validation error for malicious table key: " + maliciousKey);
        assertTrue(result.get() instanceof InputValidationErr,
            "Expected InputValidationErr for malicious table key: " + maliciousKey);
    }

    @Test(dataProvider = "maliciousPropertyKeys")
    void testErrorOnCreateWithMaliciousProperty(String maliciousPropertyKey) throws SQLException {
        final var maliciousProperty = new StringProperty(
            maliciousPropertyKey,
            "default",
            "desc",
            "default_value",
            true,
            false,
            null,
            null
        );
        final var maliciousTable = new NewTable(
            "table_key",
            "table key",
            "table description",
            List.of(maliciousProperty)
        );
        Optional<? extends Err> result = service.createTable(maliciousTable);

        // Assert
        assertTrue(result.isPresent(),
            "Expected validation error for malicious property key: " + maliciousPropertyKey);
        assertTrue(result.get() instanceof InputValidationErr,
            "Expected InputValidationErr for malicious property key: " + maliciousPropertyKey);
    }

    @Test(dataProvider = "maliciousDefaultStringValue", enabled = false)
    void testPropertyCreationWithMaliciousDefaultValue(String defaultValue) throws SQLException {
        final var tableKey = "testPropertyCreationWithMaliciousDefaultValue";
        final var maliciousProperty = new StringProperty(
            "malicious",
            "default",
            "desc",
            defaultValue,
            true,
            false,
            null,
            null
        );
        final var maliciousTable = new NewTable(
            tableKey,
            "table key",
            "table description",
            List.of(maliciousProperty)
        );
        Optional<? extends Err> result = service.createTable(maliciousTable);

        Assert.assertFalse(result.isPresent(), "Expected no errors on table creation");

        service.addRow(tableKey, List.of());

        try (final var statement = connection.createStatement()) {
            Assert.assertTrue(
                statement.execute(
                    "SELECT * FROM %s WHERE malicious = $$%s$$".formatted(tableKey, defaultValue)),
                "Expected results in result set"
            );
        }
    }

    @Test(dataProvider = "invalidMinMaxStringLengthInput")
    void testErrorWhenMinMaxConstrainsAreInvalid(Integer[] boundaries) throws SQLException {
        var newTable = new NewTable(
            "table_key_1",
            "Table table",
            "This is a test table",
            List.of(
                StringProperty.builder()
                    .key("property_one")
                    .name("Property 1")
                    .description("This is property 1")
                    .defaultValue("default_value_1")
                    .required(true)
                    .unique(false)
                    .minLength(boundaries[0])
                    .maxLength(boundaries[1])
                    .build()
            )
        );

        var err = service.createTable(newTable);
        Assert.assertTrue(err.isPresent(), "Expected no error when creating table");
    }

    @Test
    void testTableCreation() throws SQLException {
        var newTable = new NewTable(
            "table_key",
            "Table table",
            "This is a test table",
            List.of(
                StringProperty.builder()
                    .key("property_one")
                    .name("Property 1")
                    .description("This is property 1")
                    .defaultValue("default_value_1")
                    .required(true)
                    .unique(false)
                    .build()
            )
        );
        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");

        try (var statement = connection.createStatement()) {
            assertTrue(statement.execute(
                """
                    SELECT table_schema, table_name, table_type, is_insertable_into 
                    FROM information_schema.tables
                    WHERE table_name = 'table_key'"""
            ), "Statement must return result");

            var result = statement.getResultSet();
            assertTrue(result.next(), "Expected at least one result row");
            Assert.assertEquals(result.getString(1), "public", "Expected schema to be 'public'");
            Assert.assertEquals(result.getString(2), "table_key",
                "Expected table name to be 'table_key'");
            Assert.assertEquals(result.getString(3), "BASE TABLE",
                "Expected table type to be 'BASE TABLE'");
            assertTrue(result.getBoolean(4), "Expected table to be insertable into");

            var columnsTest = connection.createStatement();
            assertTrue(columnsTest.execute(
                """
                    SELECT column_name, data_type, is_nullable, column_default
                    FROM information_schema.columns
                    WHERE table_name = 'table_key'"""
            ), "Statement must return result");
            var columnsResult = columnsTest.getResultSet();
            columnsResult.next();
            Assert.assertEquals(
                columnsResult.getString("column_name"),
                "_id",
                "Expected column '_id'"
            );
            Assert.assertEquals(
                columnsResult.getString("data_type"),
                "integer",
                "Expected column '_id' to be of type 'integer'"
            );
            Assert.assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column '_id' to be NOT NULL"
            );
            Assert.assertEquals(
                columnsResult.getString("column_default"),
                "nextval('table_key__id_seq'::regclass)",
                "Expected column '_id' to have default value");
            columnsResult.next();
            Assert.assertEquals(
                columnsResult.getString("column_name"),
                "_created",
                "Expected column '_created'"
            );
            Assert.assertEquals(
                columnsResult.getString("data_type"),
                "timestamp without time zone",
                "Expected column '_created' to be of type 'timestamp without time zone'"
            );
            Assert.assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column '_created' to be NOT NULL"
            );
            Assert.assertEquals(
                columnsResult.getString("column_default"),
                "CURRENT_TIMESTAMP",
                "Expected column '_created' to have default value"
            );
            columnsResult.next();
            Assert.assertEquals(
                columnsResult.getString("column_name"),
                "property_one",
                "Expected column 'property1'"
            );
            Assert.assertEquals(
                columnsResult.getString("data_type"),
                "text",
                "Expected column 'property1' to be of type 'text'"
            );
            Assert.assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column 'property1' to be NOT NULL"
            );
            Assert.assertEquals(
                columnsResult.getString("column_default"),
                "'default_value_1'::text",
                "Expected column 'property1' to have default value"
            );
            Assert.assertFalse(columnsResult.next(), "Expected no more columns");
        }
    }


    @Test
    void testTableDeletion() throws SQLException {

        // Create a table to delete
        var newTable = new NewTable(
            "table_to_delete",
            "Table to Delete",
            "This table will be deleted",
            List.of(
                StringProperty.builder()
                    .key("property_key")
                    .name("Property 1")
                    .description("This is property 1")
                    .defaultValue("default_value_1")
                    .required(true)
                    .unique(false)
                    .build()
            )
        );
        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table to delete");

        // Delete the table
        service.deleteTable("table_to_delete");

        // Verify the table is deleted
        try (var statement = connection.createStatement()) {
            statement.execute(
                """
                    
                       SELECT 1 FROM information_schema.tables 
                     WHERE table_name = 'table_to_delete'
                    
                    """
            );

            Assert.assertFalse(statement.getResultSet().next(), "Expected no results for deld");
        }
    }

    @Test
    void testRowAddingToTheVirtualTable() throws SQLException {

        var newTable = new NewTable(
            "add_row_test",
            "Table table",
            "This is a test table",
            List.of(
                StringProperty.builder()
                    .key("property_one")
                    .name("Property 1")
                    .description("This is property 1")
                    .defaultValue("default_value_1")
                    .required(true)
                    .unique(false)
                    .build(),
                IntegerProperty.builder()
                    .key("property_two")
                    .name("Property 2")
                    .description("This is property 2")
                    .defaultValue(42L)
                    .required(true)
                    .unique(false)
                    .build(),
                BooleanProperty.builder()
                    .key("property_three")
                    .name("Property 3")
                    .description("This is property 3")
                    .defaultValue(true)
                    .required(true)
                    .unique(false)
                    .build(),
                DecimalProperty.builder()
                    .key("property_four")
                    .name("property 4")
                    .scale(3)
                    .precision(6)
                    .build()
            )
        );

        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");
        var columnValues = List.of(
            StringValue.of("property_one", "value1"),
            IntegerValue.of("property_two", 123L),
            BooleanValue.of("property_three", true),
            DecimalValue.of("property_four", BigDecimal.valueOf(123.321))
        );

        err = service.addRow("add_row_test", columnValues);
        Assert.assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (var statement = connection.createStatement()) {
            statement.execute("SELECT 1 WHERE EXISTS(SELECT property_one" +
                " FROM add_row_test" +
                " WHERE property_one = 'value1' AND property_two = 123 AND property_three = true AND property_four = 123.321);");
            var hasNext = statement.getResultSet().next();
            Assert.assertTrue(hasNext, "Expected the row to be added to the virtual table");
        }
    }

    @Test
    void testLengthLimitValidationForTextProperties() throws SQLException {
        var newTable = new NewTable(
            "table_key_add_row",
            "Table table",
            "This is a test table",
            List.of(
                StringProperty.builder()
                    .key("property_two")
                    .name("Property 2")
                    .description("This is property 2")
                    .maxLength(5)
                    .build(),
                StringProperty.builder()
                    .key("property_three")
                    .name("Property 3")
                    .description("This is property 3")
                    .minLength(5)
                    .build(),
                StringProperty.builder()
                    .key("property_four")
                    .name("Property 4")
                    .description("This is property 3")
                    .defaultValue("default_value_3")
                    .maxLength(10)
                    .minLength(5)
                    .build()
            )
        );

        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");
        var columnValues = List.of(
            StringValue.of("property_two", "value"),
            StringValue.of("property_three", "value"),
            StringValue.of("property_four", "value12345")
        );

        err = service.addRow("table_key_add_row", columnValues);
        Assert.assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (var statement = connection.createStatement()) {
            statement.execute("SELECT 1 WHERE EXISTS(" +
                "SELECT property_two, property_three, property_four " +
                "FROM table_key_add_row " +
                "WHERE property_two = 'value' AND property_three = 'value' AND property_four = 'value12345');");
            var hasNext = statement.getResultSet().next();
            Assert.assertTrue(hasNext, "Expected the row to be added to the virtual table");

            var fourthColumnFailureTooLow = List.of(
                StringValue.of("property_four", "val")
            );
            var fourthColumnFailureTooHigh = List.of(
                StringValue.of("property_four", "value123456")
            );
            try {
                service.addRow("table_key_add_row", fourthColumnFailureTooLow);
                Assert.fail("Error should have been thrown");
            } catch (SQLException ex) {
            }

            try {
                service.addRow("table_key_add_row", fourthColumnFailureTooHigh);
                Assert.fail("Error should have been thrown");
            } catch (SQLException ex) {
            }

            var thirdColumnFailure = List.of(
                StringValue.of("property_three", "val")
            );

            try {
                service.addRow("table_key_add_row", thirdColumnFailure);
                Assert.fail("Error should have been thrown");
            } catch (SQLException ex) {
            }

            var secondColumnFailure = List.of(
                StringValue.of("property_two", "value123456")
            );

            try {
                service.addRow("table_key_add_row", secondColumnFailure);
                Assert.fail("Error should have been thrown");
            } catch (SQLException ex) {
            }
        }

    }

    @Test
    void testMinMaxValidationForIntegerProperties() throws SQLException {

        var newTable = new NewTable(
            "table_key_integer_add_row",
            "Table table",
            "This is a test table",
            List.of(
                IntegerProperty.builder()
                    .key("property_two")
                    .name("Property 2")
                    .description("This is property 2")
                    .max(5L)
                    .build(),
                IntegerProperty.builder()
                    .key("property_three")
                    .name("Property 3")
                    .description("This is property 3")
                    .min(5L)
                    .build(),
                IntegerProperty.builder()
                    .key("property_four")
                    .name("Property 4")
                    .description("This is property four")
                    .min(5L)
                    .max(10L)
                    .build()
            ));

        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Table must be created without issues");

        try {
            service.addRow(
                "table_key_integer_add_row",
                List.of(
                    IntegerValue.of("property_two", 6L)
                )
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        try {
            service.addRow(
                "table_key_integer_add_row",
                List.of(
                    IntegerValue.of("property_three", 4L)
                )
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        try {
            service.addRow(
                "table_key_integer_add_row",
                List.of(
                    IntegerValue.of("property_four", 11L)
                )
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        try {
            service.addRow(
                "table_key_integer_add_row",
                List.of(
                    IntegerValue.of("property_four", 4L)
                )
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        err = service.addRow(
            "table_key_integer_add_row",
            List.of(
                IntegerValue.of("property_four", 10L),
                IntegerValue.of("property_three", 6L),
                IntegerValue.of("property_two", 2L)
            )
        );

        Assert.assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (var assertStatement = connection.createStatement()) {
            assertStatement.execute("""   
                SELECT 1 WHERE EXISTS(
                    SELECT * FROM table_key_integer_add_row
                    WHERE property_two = 2 AND property_three = 6 AND property_four = 10)""");
            boolean hasNext = assertStatement.getResultSet().next();
            Assert.assertTrue(hasNext, "Expected the row to be added to the virtual table");
        }
    }

    @Test
    void testMinMaxValidationForDecimalProperties() throws SQLException {

        var newTable = new NewTable(
            "table_key_decimal_add_row",
            "Table table",
            "This is a test table",
            List.of(
                DecimalProperty.builder()
                    .key("property_two")
                    .name("Property 2")
                    .description("This is property 2")
                    .max(BigDecimal.valueOf(2.5))
                    .precision(4)
                    .scale(3)
                    .build(),
                DecimalProperty.builder()
                    .key("property_three")
                    .name("Property 3")
                    .description("This is property 3")
                    .min(BigDecimal.valueOf(1.125))
                    .precision(4)
                    .scale(3)
                    .build(),
                DecimalProperty.builder()
                    .key("property_four")
                    .name("Property 4")
                    .description("This is property four")
                    .precision(4)
                    .scale(3)
                    .min(BigDecimal.valueOf(1.125))
                    .max(BigDecimal.valueOf(2.5))
                    .build()
            ));

        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Table must be created without issues");

        try {
            service.addRow(
                "table_key_decimal_add_row",
                List.of(
                    DecimalValue.of("property_two", BigDecimal.valueOf(6))
                )
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        try {
            service.addRow(
                "table_key_decimal_add_row",
                List.of(
                    DecimalValue.of("property_three", BigDecimal.valueOf(1))
                )
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        try {
            service.addRow(
                "table_key_decimal_add_row",
                List.of(
                    DecimalValue.of("property_four", BigDecimal.valueOf(10)))
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        try {
            service.addRow(
                "table_key_decimal_add_row",
                List.of(
                    DecimalValue.of("property_four", BigDecimal.valueOf(1)))
            );
            Assert.fail("Error should have been thrown");
        } catch (SQLException ex) {
        }

        err = service.addRow(
            "table_key_decimal_add_row",
            List.of(
                DecimalValue.of("property_four", BigDecimal.valueOf(2.5)),
                DecimalValue.of("property_three", BigDecimal.valueOf(1.2453)),
                DecimalValue.of("property_two", BigDecimal.valueOf(1.126)))
        );

        Assert.assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (var assertStatement = connection.createStatement()) {
            assertStatement.execute("""   
                SELECT 1 WHERE EXISTS(
                    SELECT * FROM table_key_decimal_add_row
                    WHERE property_four = 2.5 AND property_three = 1.245 AND property_two = 1.126)""");
            boolean hasNext = assertStatement.getResultSet().next();
            Assert.assertTrue(hasNext, "Expected the row to be added to the virtual table");
        }
    }

    @Test
    void testRowDeletionFromTheVirtualTable() throws SQLException {
        var newTable = new NewTable(
            "table_key_delete_row",
            "Table table",
            "This is a test table",
            List.of(
                StringProperty.builder()
                    .key("property_one")
                    .name("Property 1")
                    .description("This is property 1")
                    .defaultValue("default_value_1")
                    .required(true)
                    .unique(false)
                    .build()
            )
        );

        var err = service.createTable(newTable);
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");

        try (var statement = connection.createStatement()) {
            statement.execute("INSERT INTO table_key_delete_row (property_one) VALUES ('value1');");
            statement.execute(
                "SELECT _id FROM table_key_delete_row WHERE property_one = 'value1';");
            var resultSet = statement.getResultSet();
            var hasNext = resultSet.next();
            Assert.assertTrue(hasNext, "Expected the row to be added to the virtual table");
            int rowId = resultSet.getInt("_id");

            err = service.deleteRow("table_key_delete_row", rowId);
            Assert.assertFalse(err.isPresent(),
                "Expected no error when deleting row from the virtual table");
            try (var assertStatement = connection.createStatement()) {
                assertStatement.execute("""   
                    SELECT 1 WHERE EXISTS(
                        SELECT property_one FROM table_key_delete_row WHERE property_one = 'value1')""");
                hasNext = assertStatement.getResultSet().next();
                Assert.assertFalse(hasNext,
                    "Expected the row to be deleted from the virtual table");
            }
        }
    }

    @Test
    void testTableCreationWithReferenceProperty() throws SQLException {
        var err = service.createTable(
            NewTable
                .builder()
                .key("test_table_creation_with_reference_property")
                .description("test_table_creation_with_reference_property_description")
                .name("test_table_creation_with_reference_property")
                .properties(List.of(
                    StringProperty.builder()
                        .key("property_one")
                        .name("Property 1")
                        .description("This is property 1")
                        .build()))
                .build()
        );
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");
        err = service.addRow("test_table_creation_with_reference_property", List.of());
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");

        err = service.createTable(
            NewTable
                .builder()
                .key("test_table_creation_with_reference_property_having_ref")
                .name("test_table_creation_with_reference_property_having_ref")
                .description("test_table_creation_with_reference_property_having_ref_description")
                .properties(List.of(
                        ReferenceProperty.builder()
                            .key("test_table_creation_with_reference_property_ref_property")
                            .name("test_table_creation_with_reference_property_ref_property")
                            .description("test_table_creation_with_reference_property_ref_description")
                            .refTableKey("test_table_creation_with_reference_property")
                            .required(true)
                            .build()
                    )
                )
                .build()
        );
        Assert.assertFalse(err.isPresent(), "Expected no error when creating table");

        try {
            service.addRow(
                "test_table_creation_with_reference_property_having_ref",
                List.of(
                    ReferenceValue.of("test_table_creation_with_reference_property_ref_property",
                        42))
            );
            Assert.fail("Expected exception being thrown");
        } catch (SQLException e) {
        }

        try (final var statement = connection.createStatement()) {
            statement.execute("SELECT _id FROM test_table_creation_with_reference_property");
            statement.getResultSet().next();
            var parentId = statement.getResultSet().getInt(1);

            service.addRow(
                "test_table_creation_with_reference_property_having_ref",
                List.of(
                    ReferenceValue.of("test_table_creation_with_reference_property_ref_property",
                        parentId))
            );

            statement.clearBatch();
            statement.execute("SELECT _id " +
                "FROM test_table_creation_with_reference_property_having_ref " +
                "WHERE test_table_creation_with_reference_property_ref_property = %d".formatted(
                    parentId));
            Assert.assertTrue(statement.getResultSet().next(),
                "Expected the row being returned with reference");
        } catch (SQLException ex) {
            Assert.fail("expected no errors for; got " + ex.getMessage());
        }
    }

    private Connection createConnection() throws SQLException {
        String url =
            "jdbc:postgresql://localhost:%d/%s".formatted(container.getMappedPort(5432), DB_NAME);
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "test");
        return DriverManager.getConnection(url, props);
    }
}
