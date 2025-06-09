package edu.ukma.smart.virtual;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.ConsoleHandler;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.properties.StringProperty;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

class DefaultVirtualTableServiceTest {

    private static final String DB_NAME = "test_db";

    private GenericContainer<?> container;

    @BeforeClass
    void startContainer() throws IOException, InterruptedException {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        container = new GenericContainer<>("postgres:latest")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "test");
        container.start();
        container.execInContainer("psql",
            "-U", "postgres",
            "-c", "CREATE DATABASE %s;".formatted(DB_NAME));
    }

    @AfterClass
    void stopContainer() {
        container.stop();
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
        return new Object[][]{
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
        return new Object[][]{
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


    @Test(dataProvider = "maliciousTableKeys")
    void testErrorOnInvalidTableKey(String maliciousKey) throws SQLException {
        final var maliciousTable = new NewTable(
            maliciousKey,
            "Malicious Table",
            "This table has a malicious key",
            List.of()
        );

        try (final var connection = createConnection()) {
            final var service = new DefaultVirtualTableService(connection);
            Optional<? extends Err> result = service.createTable(maliciousTable);

            // Assert
            assertTrue(result.isPresent(),
                "Expected validation error for malicious table key: " + maliciousKey);
            assertTrue(result.get() instanceof InputValidationErr,
                "Expected InputValidationErr for malicious table key: " + maliciousKey);
        }
    }

    @Test(dataProvider = "maliciousPropertyKeys")
    void testErrorOnCreateWithMaliciousProperty(String maliciousPropertyKey) throws SQLException {
        final var maliciousProperty = new StringProperty(
            maliciousPropertyKey,
            "default",
            "desc",
            "default_value",
            true,
            false
        );
        final var maliciousTable = new NewTable(
            "table_key",
            "table key",
            "table description",
            List.of(maliciousProperty)
        );
        try (final var connection = createConnection()) {
            final var service = new DefaultVirtualTableService(connection);
            Optional<? extends Err> result = service.createTable(maliciousTable);

            // Assert
            assertTrue(result.isPresent(),
                "Expected validation error for malicious property key: " + maliciousPropertyKey);
            assertTrue(result.get() instanceof InputValidationErr,
                "Expected InputValidationErr for malicious property key: " + maliciousPropertyKey);
        }
    }

    // TODO: complete test once insert is supported
    @Test(dataProvider = "maliciousDefaultStringValue", enabled = false)
    void testPropertyCreationWithMaliciousDefaultValue(String defaultValue) throws SQLException {
        final var maliciousProperty = new StringProperty(
            "test",
            "default",
            "desc",
            "default_value",
            true,
            false
        );
        final var maliciousTable = new NewTable(
            "users",
            "table key",
            "table description",
            List.of(maliciousProperty)
        );
        try (final var connection = createConnection()) {
            final var service = new DefaultVirtualTableService(connection);
            Optional<? extends Err> result = service.createTable(maliciousTable);

        }
    }

    @Test
    void testTableCreation() throws SQLException {
        try (Connection conn = createConnection()) {
            var service = new DefaultVirtualTableService(conn);

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
                        .isRequired(true)
                        .isUnique(false)
                        .build()
                )
            );
            var err = service.createTable(newTable);
            Assert.assertFalse(err.isPresent(), "Expected no error when creating table");

            var statement = conn.createStatement();
            assertTrue(statement.execute(
                """
                    SELECT table_schema, table_name, table_type, is_insertable_into 
                    FROM information_schema.tables
                    WHERE table_name = 'table_key'"""
            ), "Statement must return result");

            var result = statement.getResultSet();
            assertTrue(result.next(), "Expected at least one result row");
            Assert.assertEquals(result.getString(1), "public", "Expected schema to be 'public'");
            Assert.assertEquals(result.getString(2), "table_key", "Expected table name to be 'table_key'");
            Assert.assertEquals(result.getString(3), "BASE TABLE", "Expected table type to be 'BASE TABLE'");
            assertTrue(result.getBoolean(4), "Expected table to be insertable into");

            var columnsTest = conn.createStatement();
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
                "character varying",
                "Expected column 'property1' to be of type 'character varying'"
            );
            Assert.assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column 'property1' to be NOT NULL"
            );
            Assert.assertEquals(
                columnsResult.getString("column_default"),
                "'default_value_1'::character varying",
                "Expected column 'property1' to have default value"
            );
            Assert.assertFalse(columnsResult.next(), "Expected no more columns");
        }
    }

    @Test
    void testTableDeletion() throws SQLException {
        try (Connection conn = createConnection()) {
            var service = new DefaultVirtualTableService(conn);

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
                        .isRequired(true)
                        .isUnique(false)
                        .build()
                )
            );
            var err = service.createTable(newTable);
            Assert.assertFalse(err.isPresent(), "Expected no error when creating table to delete");

            // Delete the table
            service.deleteTable("table_to_delete");

            // Verify the table is deleted
            var statement = conn.createStatement();
            statement.execute(
                """
                    SELECT 1 FROM information_schema.tables 
                    WHERE table_name = 'table_to_delete'
                    """
            );

            Assert.assertFalse(statement.getResultSet().next(), "Expected no results for deleted table");
        }
    }

    private Connection createConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:%d/%s".formatted(container.getMappedPort(5432), DB_NAME);
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "test");
        return DriverManager.getConnection(url, props);
    }
}
