package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED;
import static edu.ukma.smart.virtual.errors.OperationError.ErrorCode.TABLE_DOES_NOT_EXIST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.create.BooleanProperty;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.BooleanPredicate;
import edu.ukma.smart.virtual.dml.select.DecimalPredicate;
import edu.ukma.smart.virtual.dml.select.IntegerPredicate;
import edu.ukma.smart.virtual.dml.select.ReferencePredicate;
import edu.ukma.smart.virtual.dml.select.SelectProperty;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.select.StringPredicate;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.BooleanValue;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.dml.values.DecimalValue;
import edu.ukma.smart.virtual.dml.values.IntegerValue;
import edu.ukma.smart.virtual.dml.values.ReferenceValue;
import edu.ukma.smart.virtual.dml.values.StringValue;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.OperationError;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

class DefaultVirtualTableServiceTest {

    private static final String DB_NAME = "test_db";

    private GenericContainer<?> container;
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
        container.execInContainer("psql",
            "-U", "postgres",
            "-d", DB_NAME,
            "-c", "CREATE SCHEMA custom;");
        service = new DefaultVirtualTableService(this::createConnection, new Config());
    }

    @AfterClass(alwaysRun = true)
    void stopContainer() throws SQLException {
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
        return new Object[][] {
            // Basic SQL injection attempts
            {"_users'; DROP TABLE test; --"},
            {"_users; DELETE FROM information_schema.tables; --"},
            {"_users') UNION SELECT * FROM pg_user; --"},

            // Schema manipulation attempts
            {"_users; CREATE TABLE malicious (id INT); --"},
            {"_users; ALTER TABLE existing_table DROP COLUMN important_col; --"},
            {"_users; GRANT ALL ON DATABASE TO public; --"},

            // Information disclosure attempts
            {"_users'; SELECT * FROM pg_stat_activity; --"},
            {"_users UNION SELECT version(); --"},
            {"_users'; SELECT current_user; --"},

            // Special characters and encoding
            {"_user\"; DROP TABLE test; --"},
            {"_user\\\"; DROP TABLE test; --"},
            {"_user%27; DROP TABLE test; --"},
            {"_user'; INSERT INTO log VALUES ('hacked'); --"},

            // Case variations
            {"-Users"}, // uppercase
            {"-USERS"}, // all caps
            {"-UsErS"}, // mixed case

            // Invalid patterns that should be rejected
            {"-user_table"}, // contains underscore
            {"_user table"}, // contains space
            {"_user@table"}, // contains @
            {"-9users"}, // starts with number
            {"_user.table"}, // contains dot
            {"_user#table"}, // contains hash
            {"_user$table"}, // contains dollar
            {"_user%table"}, // contains percent
            {"_user*table"}, // contains asterisk

            // Boundary testing
            {"_u"}, // too short (less than 2 chars)
            {"_a" + "b".repeat(101)}, // too long (over 100 chars)
            {"-"}, // empty string
            {"- users"}, // leading space
            {"_users "}, // trailing space
            {"-users"}, // starts with dash
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
            {-1, -1}, // invalid_negative, invalid negative
            {10, 9} // valid more than max, valid less than min
        };
    }


    @Test(dataProvider = "maliciousTableKeys")
    void testErrorOnInvalidTableKey(String maliciousKey) throws SQLException {
        final var maliciousTable = NewTable.builder()
            .key(maliciousKey)
            .name("Malicious Table")
            .description("This table has a malicious key")
            .build();

        Optional<? extends Err> result = service.createTable(maliciousTable);
        // Assert
        assertTrue(result.isPresent(),
            "Expected validation error for malicious table key: " + maliciousKey);
        assertTrue(result.get() instanceof InputValidationErr,
            "Expected InputValidationErr for malicious table key: " + maliciousKey);
        assertEquals(((InputValidationErr) result.get()).code(), InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT);
    }

    @Test(dataProvider = "maliciousPropertyKeys")
    void testErrorOnCreateWithMaliciousProperty(String maliciousPropertyKey) throws SQLException {
        final var maliciousProperty = new StringProperty(
            maliciousPropertyKey,
            "_default",
            "desc",
            "default_value",
            true,
            Set.of()
        );
        final var maliciousTable = NewTable
            .builder()
            .key("_table_key")
            .name("table key")
            .description("table description")
            .properties(List.of(maliciousProperty))
            .build();
        Optional<? extends Err> result = service.createTable(maliciousTable);

        // Assert
        assertTrue(result.isPresent(),
            "Expected validation error for malicious property key: " + maliciousPropertyKey);
        assertEquals(((InputValidationErr) result.get()).code(), InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT);
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
            Set.of()
        );
        final var maliciousTable = NewTable.builder()
            .key(tableKey)
            .name("table key")
            .description("table description")
            .properties(List.of(maliciousProperty))
            .build();
        Optional<? extends Err> result = service.createTable(maliciousTable);

        assertFalse(result.isPresent(), "Expected no errors on table creation");

        service.addRow(tableKey, List.of());

        try (final var connection = createConnection();
             final var statement = connection.createStatement()) {
            assertTrue(
                statement.execute(
                    "SELECT * FROM %s WHERE malicious = $$%s$$".formatted(tableKey, defaultValue)),
                "Expected results in result set"
            );
        }
    }

    @Test
    void testTableCreation() throws SQLException {
        var newTable = NewTable.builder()
            .key("_table_key")
            .name("Table table")
            .description("This is a test table")
            .properties(List.of(
                    StringProperty.builder()
                        .key("property_one")
                        .name("Property 1")
                        .description("This is property 1")
                        .defaultValue("default_value_1")
                        .notNull(true)
                        .build()
                )
            ).build();
        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Expected no error when creating table");

        try (
            final var connection = createConnection();
            final var statement = connection.createStatement()
        ) {
            assertTrue(statement.execute(
                """
                    SELECT table_schema, table_name, table_type, is_insertable_into
                    FROM information_schema.tables
                    WHERE table_name = '_table_key'"""
            ), "Statement must return result");

            var result = statement.getResultSet();
            assertTrue(result.next(), "Expected at least one result row");
            assertEquals(result.getString(1), "custom", "Expected schema to be 'public'");
            assertEquals(result.getString(2), "_table_key",
                "Expected table name to be 'table_key'");
            assertEquals(result.getString(3), "BASE TABLE",
                "Expected table type to be 'BASE TABLE'");
            assertTrue(result.getBoolean(4), "Expected table to be insertable into");

            var columnsTest = connection.createStatement();
            assertTrue(columnsTest.execute(
                """
                    SELECT column_name, data_type, is_nullable, column_default
                    FROM information_schema.columns
                    WHERE table_name = '_table_key'"""
            ), "Statement must return result");
            var columnsResult = columnsTest.getResultSet();
            columnsResult.next();
            assertEquals(
                columnsResult.getString("column_name"),
                "_id",
                "Expected column '_id'"
            );
            assertEquals(
                columnsResult.getString("data_type"),
                "integer",
                "Expected column '_id' to be of type 'integer'"
            );
            assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column '_id' to be NOT NULL"
            );
            assertEquals(
                columnsResult.getString("column_default"),
                "nextval('custom._table_key__id_seq'::regclass)",
                "Expected column '_id' to have default value");
            columnsResult.next();
            assertEquals(
                columnsResult.getString("column_name"),
                "_created",
                "Expected column '_created'"
            );
            assertEquals(
                columnsResult.getString("data_type"),
                "timestamp with time zone",
                "Expected column '_created' to be of type 'timestamp without time zone'"
            );
            assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column '_created' to be NOT NULL"
            );
            assertEquals(
                columnsResult.getString("column_default"),
                "CURRENT_TIMESTAMP",
                "Expected column '_created' to have default value"
            );
            columnsResult.next();
            assertEquals(
                columnsResult.getString("column_name"),
                "property_one",
                "Expected column 'property_one'"
            );
            assertEquals(
                columnsResult.getString("data_type"),
                "text",
                "Expected column 'property1' to be of type 'text'"
            );
            assertEquals(
                columnsResult.getString("is_nullable"),
                "NO",
                "Expected column 'property1' to be NOT NULL"
            );
            assertEquals(
                columnsResult.getString("column_default"),
                "'default_value_1'::text",
                "Expected column 'property1' to have default value"
            );
            assertFalse(columnsResult.next(), "Expected no more columns");
        }
    }


    @Test
    void testTableDeletion() throws SQLException {

        // Create a table to delete
        var newTable = NewTable.builder()
            .key("_table_to_delete")
            .name("Table to Delete")
            .description("This table will be deleted")
            .properties(List.of(
                    StringProperty.builder()
                        .key("property_key")
                        .name("Property 1")
                        .description("This is property 1")
                        .defaultValue("default_value_1")
                        .notNull(true)
                        .build()
                )
            ).build();
        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Expected no error when creating table to delete, got error " + err.orElse(null));

        // Delete the table
        service.dropTable(DropTable.of("_table_to_delete"));

        // Verify the table is deleted
        try (
            final var connection = createConnection();
            var statement = connection.createStatement()
        ) {
            statement.execute(
                """
                    
                    SELECT 1 FROM information_schema.tables
                    WHERE table_name = '_table_to_delete' AND table_schema = 'custom' 
                    
                    """
            );

            assertFalse(statement.getResultSet().next(), "Expected no results for _table_to_delete");
        }
    }

    @Test
    void testRowAddingToTheVirtualTable() throws SQLException {

        var newTable = NewTable.builder()
            .key("_add_row_test")
            .name("Table table")
            .description("This is a test table")
            .properties(List.of(
                    StringProperty.builder()
                        .key("property_one")
                        .name("Property 1")
                        .description("This is property 1")
                        .defaultValue("default_value_1")
                        .notNull(true)
                        .build(),
                    IntegerProperty.builder()
                        .key("property_two")
                        .name("Property 2")
                        .description("This is property 2")
                        .defaultValue(42L)
                        .notNull(true)
                        .build(),
                    BooleanProperty.builder()
                        .key("property_three")
                        .name("Property 3")
                        .description("This is property 3")
                        .defaultValue(true)
                        .notNull(true)
                        .build(),
                    DecimalProperty.builder()
                        .key("property_four")
                        .name("property 4")
                        .scale(3)
                        .precision(6)
                        .build()
                )
            ).build();

        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Expected no error when creating table, got " + err.orElse(null));
        var columnValues = List.of(
            StringValue.of("property_one", "value1"),
            IntegerValue.of("property_two", 123L),
            BooleanValue.of("property_three", true),
            DecimalValue.of("property_four", BigDecimal.valueOf(123.321))
        );

        err = service.addRow("_add_row_test", columnValues);
        assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (
            var connection = createConnection();
            var statement = connection.createStatement()
        ) {
            statement.execute("""
                SELECT 1 WHERE EXISTS(SELECT "property_one"\
                 FROM custom._add_row_test\
                 WHERE "property_one" = 'value1' AND "property_two" = 123 AND "property_three" = true AND "property_four" = 123.321);""");
            var hasNext = statement.getResultSet().next();
            assertTrue(hasNext, "Expected the row to be added to the virtual table");
        }
    }

    @Test
    void testLengthLimitValidationForTextProperties() throws SQLException {
        var newTable = NewTable.builder()
            .key("_table_key_add_row")
            .name("Table table")
            .description("This is a test table")
            .properties(List.of(
                    StringProperty.builder()
                        .key("property_two")
                        .name("Property 2")
                        .description("This is property 2")
                        .constraints(Set.of(PropertyConstraint.maxLength(5)))
                        .build(),
                    StringProperty.builder()
                        .key("property_three")
                        .name("Property 3")
                        .description("This is property 3")
                        .constraints(Set.of(PropertyConstraint.minLength(5)))
                        .build(),
                    StringProperty.builder()
                        .key("property_four")
                        .name("Property 4")
                        .description("This is property 3")
                        .defaultValue("default_value_3")
                        .constraints(Set.of(
                                PropertyConstraint.maxLength(10),
                                PropertyConstraint.minLength(5)
                            )
                        )
                        .build()
                )
            ).build();

        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Expected no error when creating table");
        var columnValues = List.of(
            StringValue.of("property_two", "value"),
            StringValue.of("property_three", "value"),
            StringValue.of("property_four", "value12345")
        );

        err = service.addRow("_table_key_add_row", columnValues);
        assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table " + err);

        var con = createConnection();
        try (var statement = con.createStatement()) {
            statement.execute("""
                SELECT 1 WHERE EXISTS(\
                SELECT "property_two", "property_three", "property_four" \
                FROM custom._table_key_add_row \
                WHERE "property_two" = 'value' AND "property_three" = 'value' AND "property_four" = 'value12345');""");
            var hasNext = statement.getResultSet().next();
            assertTrue(hasNext, "Expected the row to be added to the virtual table");

            var fourthColumnFailureTooLow = List.of(
                StringValue.of("property_four", "val")
            );
            err = service.addRow("_table_key_add_row", fourthColumnFailureTooLow);
            assertTrue(err.isPresent(), "Expected error, but got no errors");
            assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

            var fourthColumnFailureTooHigh = List.of(
                StringValue.of("property_four", "value123456")
            );
            err = service.addRow("_table_key_add_row", fourthColumnFailureTooHigh);
            assertTrue(err.isPresent(), "Expected error, but got no errors");
            assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

            var thirdColumnFailure = List.of(
                StringValue.of("property_three", "val")
            );

            err = service.addRow("_table_key_add_row", thirdColumnFailure);
            assertTrue(err.isPresent(), "Expected error, but got no errors");
            assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

            var secondColumnFailure = List.of(
                StringValue.of("property_two", "value123456")
            );

            err = service.addRow("_table_key_add_row", secondColumnFailure);
            assertTrue(err.isPresent(), "Expected error, but got no errors");
            assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);
        } finally {
            con.close();
        }

    }

    @Test
    void testMinMaxValidationForIntegerProperties() throws SQLException {

        var newTable = NewTable.builder()
            .key("_table_key_integer_add_row")
            .name("Table table")
            .description("This is a test table")
            .properties(List.of(
                IntegerProperty.builder()
                    .key("property_two")
                    .name("Property 2")
                    .description("This is property 2")
                    .constraints(Set.of(PropertyConstraint.lessOrEqual(5L)))
                    .build(),
                IntegerProperty.builder()
                    .key("property_three")
                    .name("Property 3")
                    .description("This is property 3")
                    .constraints(Set.of(PropertyConstraint.greaterOrEqual(5L)))
                    .build(),
                IntegerProperty.builder()
                    .key("property_four")
                    .name("Property 4")
                    .description("This is property four")
                    .constraints(Set.of(
                        PropertyConstraint.greaterOrEqual(7L),
                        PropertyConstraint.lessOrEqual(10L))
                    )
                    .build()
            )).build();

        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Table must be created without issues, but found " + err.orElse(null));

        err = service.addRow(
            "_table_key_integer_add_row",
            List.of(
                IntegerValue.of("property_two", 6L)
            )
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_integer_add_row",
            List.of(
                IntegerValue.of("property_three", 4L)
            )
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_integer_add_row",
            List.of(
                IntegerValue.of("property_four", 11L)
            )
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_integer_add_row",
            List.of(
                IntegerValue.of("property_four", 4L)
            )
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_integer_add_row",
            List.of(
                IntegerValue.of("property_four", 10L),
                IntegerValue.of("property_three", 6L),
                IntegerValue.of("property_two", 2L)
            )
        );

        assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (
            var connection = createConnection();
            var assertStatement = connection.createStatement()
        ) {
            assertStatement.execute("""
                SELECT 1 WHERE EXISTS(
                    SELECT * FROM custom._table_key_integer_add_row
                    WHERE "property_two" = 2 AND "property_three" = 6 AND "property_four" = 10)""");
            boolean hasNext = assertStatement.getResultSet().next();
            assertTrue(hasNext, "Expected the row to be added to the virtual table");
        }
    }


    @Test
    void testMinMaxValidationForDecimalProperties() throws SQLException {
        var newTable = NewTable
            .builder()
            .key("_table_key_decimal_add_row")
            .name("Table table")
            .description("This is a test table")
            .properties(
                List.of(
                    DecimalProperty.builder()
                        .key("property_two")
                        .name("Property 2")
                        .description("This is property 2")
                        .constraints(Set.of(PropertyConstraint.lessOrEqual(BigDecimal.valueOf(2.5))))
                        .precision(4)
                        .scale(3)
                        .build(),
                    DecimalProperty.builder()
                        .key("property_three")
                        .name("Property 3")
                        .description("This is property 3")
                        .constraints(Set.of(PropertyConstraint.greaterOrEqual(BigDecimal.valueOf(1.125))))
                        .precision(4)
                        .scale(3)
                        .build(),
                    DecimalProperty.builder()
                        .key("property_four")
                        .name("Property 4")
                        .description("This is property four")
                        .precision(4)
                        .scale(3)
                        .constraints(Set.of(
                                PropertyConstraint.greaterOrEqual(BigDecimal.valueOf(1.125)),
                                PropertyConstraint.lessOrEqual(BigDecimal.valueOf(2.5))
                            )
                        )
                        .build()
                ))
            .build();

        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Table must be created without issues");

        err = service.addRow(
            "_table_key_decimal_add_row",
            List.of(
                DecimalValue.of("property_two", BigDecimal.valueOf(6))
            )
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_decimal_add_row",
            List.of(
                DecimalValue.of("property_three", BigDecimal.valueOf(1))
            )
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_decimal_add_row",
            List.of(
                DecimalValue.of("property_four", BigDecimal.valueOf(10)))
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_decimal_add_row",
            List.of(
                DecimalValue.of("property_four", BigDecimal.valueOf(1)))
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), PROPERTY_CHECK_VIOLATED);

        err = service.addRow(
            "_table_key_decimal_add_row",
            List.of(
                DecimalValue.of("property_four", BigDecimal.valueOf(2.5)),
                DecimalValue.of("property_three", BigDecimal.valueOf(1.2453)),
                DecimalValue.of("property_two", BigDecimal.valueOf(1.126)))
        );

        assertFalse(err.isPresent(),
            "Expected no error when adding row to the virtual table");

        try (
            final var connection = createConnection();
            var assertStatement = connection.createStatement()
        ) {
            assertStatement.execute("""
                SELECT 1 WHERE EXISTS(
                    SELECT * FROM custom._table_key_decimal_add_row
                    WHERE "property_four" = 2.5 AND "property_three" = 1.245 AND "property_two" = 1.126)""");
            boolean hasNext = assertStatement.getResultSet().next();
            assertTrue(hasNext, "Expected the row to be added to the virtual table");
        }
    }

    @Test
    void testRowDeletionFromTheVirtualTable() throws SQLException {
        var newTable = NewTable
            .builder()
            .key("_table_key_delete_row")
            .name("Table table")
            .description("This is a test table")
            .properties(
                List.of(
                    StringProperty.builder()
                        .key("property_one")
                        .name("Property 1")
                        .description("This is property 1")
                        .defaultValue("default_value_1")
                        .notNull(true)
                        .build()
                )
            ).build();

        var err = service.createTable(newTable);
        assertFalse(err.isPresent(), "Expected no error when creating table");

        try (
            final var connection = createConnection();
            var statement = connection.createStatement()
        ) {
            statement.execute("""
                INSERT INTO custom._table_key_delete_row ("property_one") VALUES ('value1');""");
            statement.execute(
                """
                    SELECT _id FROM custom._table_key_delete_row WHERE "property_one" = 'value1';""");
            var resultSet = statement.getResultSet();
            var hasNext = resultSet.next();
            assertTrue(hasNext, "Expected the row to be added to the virtual table");
            int rowId = resultSet.getInt("_id");

            err = service.deleteRow(DeleteRow.of("_table_key_delete_row", rowId));
            assertFalse(err.isPresent(), "Expected no error when deleting row from the virtual table");
            try (var assertStatement = connection.createStatement()) {
                assertStatement.execute("""
                    SELECT 1 WHERE EXISTS(
                        SELECT "property_one" FROM custom._table_key_delete_row WHERE "property_one" = 'value1')""");
                hasNext = assertStatement.getResultSet().next();
                assertFalse(hasNext,
                    "Expected the row to be deleted from the virtual table");
            }
        }
    }

    @Test
    void testTableCreationWithReferenceProperty() throws SQLException {
        var err = service.createTable(
            NewTable
                .builder()
                .key("_test_table_creation_with_reference_property")
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
        assertFalse(err.isPresent(), "Expected no error when creating table");
        err = service.addRow("_test_table_creation_with_reference_property", List.of());
        assertFalse(err.isPresent(), "Expected no error when creating table");

        err = service.createTable(
            NewTable
                .builder()
                .key("_test_table_creation_with_reference_property_having_ref")
                .name("test_table_creation_with_reference_property_having_ref")
                .description("test_table_creation_with_reference_property_having_ref_description")
                .properties(List.of(
                        ReferenceProperty.builder()
                            .key("test_table_creation_with_reference_property_ref_property")
                            .name("test_table_creation_with_reference_property_ref_property")
                            .description("test_table_creation_with_reference_property_ref_description")
                            .refTableKey("_test_table_creation_with_reference_property")
                            .notNull(true)
                            .build()
                    )
                )
                .build()
        );
        assertFalse(err.isPresent(), "Expected no error when creating table");

        err = service.addRow(
            "_test_table_creation_with_reference_property_having_ref",
            List.of(
                ReferenceValue.of("test_table_creation_with_reference_property_ref_property",
                    42))
        );
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);

        try (
            final var connection = createConnection();
            final var statement = connection.createStatement()
        ) {
            statement.execute("SELECT _id FROM custom._test_table_creation_with_reference_property");
            statement.getResultSet().next();
            var parentId = statement.getResultSet().getInt(1);

            err = service.addRow(
                "_test_table_creation_with_reference_property_having_ref",
                List.of(ReferenceValue.of("test_table_creation_with_reference_property_ref_property", parentId))
            );

            assertFalse(err.isPresent(), "Expected no error when creating table, but got " + err.orElse(null));

            statement.clearBatch();
            statement.execute("""
                SELECT _id
                FROM custom._test_table_creation_with_reference_property_having_ref
                WHERE "test_table_creation_with_reference_property_ref_property" = %d""".formatted(
                parentId));
            assertTrue(statement.getResultSet().next(),
                "Expected the row being returned with reference");
        } catch (SQLException ex) {
            fail("expected no errors for; got " + ex.getMessage());
        }
    }

    @Test
    void testTableRowUpdate() throws SQLException {
        var err = service.createTable(
            NewTable
                .builder()
                .key("_test_table_row_update_base_table")
                .name("base_table_name")
                .description("base_table_description")
                .properties(List.of(
                    IntegerProperty
                        .builder()
                        .key("int_prop")
                        .name("int_prop_name")
                        .description("int_prop_description")
                        .build()
                ))
                .build()
        );

        err = service.addRow("_test_table_row_update_base_table", List.of(IntegerValue.of("int_prop", 1L)));
        assertFalse(err.isPresent(), "Expected no error when creating table, but got " + err.orElse(null));
        err = service.addRow("_test_table_row_update_base_table", List.of(IntegerValue.of("int_prop", 2L)));
        assertFalse(err.isPresent(), "Expected no error when creating table, but got " + err.orElse(null));


        err = service.createTable(
            NewTable
                .builder()
                .key("_test_table_row_update")
                .description("_test_table_row_update_description")
                .name("_test_table_row_update_name")
                .properties(List.of(
                    IntegerProperty
                        .builder()
                        .key("int_prop")
                        .name("int_prop_name")
                        .description("int_prop_description")
                        .build(),
                    DecimalProperty
                        .builder()
                        .key("decimal_prop")
                        .name("decimal_prop_name")
                        .description("decimal_prop_name")
                        .precision(10)
                        .scale(2)
                        .build(),
                    StringProperty.builder()
                        .key("string_prop")
                        .name("string_prop")
                        .description("string_prop")
                        .build(),
                    BooleanProperty.builder()
                        .key("boolean_prop")
                        .name("boolean_prop")
                        .description("boolean_prop")
                        .build(),
                    ReferenceProperty.builder()
                        .key("ref_prop")
                        .name("ref_name")
                        .description("ref_description")
                        .refTableKey("_test_table_row_update_base_table")
                        .build()
                ))
                .build()
        );
        assertFalse(err.isPresent());

        err = service.addRow(
            "_test_table_row_update",
            List.of(
                IntegerValue.of("int_prop", 1L),
                DecimalValue.of("decimal_prop", new BigDecimal("25.5")),
                StringValue.of("string_prop", "123"),
                BooleanValue.of("boolean_prop", true),
                ReferenceValue.of("ref_prop", 1))
        );

        assertFalse(err.isPresent());

        err = service.updateRow(
            UpdateRow.of("_test_table_row_update", 1, List.of(
                IntegerValue.of("int_prop", 2L),
                DecimalValue.of("decimal_prop", new BigDecimal("35.5")),
                StringValue.of("string_prop", "321"),
                BooleanValue.of("boolean_prop", false),
                ReferenceValue.of("ref_prop", 2)
            ))
        );

        assertFalse(err.isPresent());

        try (
            final var connection = createConnection();
            final var statement = connection.createStatement()) {
            statement.execute("""
                SELECT _id FROM custom._test_table_row_update\
                 WHERE "int_prop" = 2 AND "decimal_prop" = 35.5 AND "string_prop" = '321' AND "boolean_prop" = FALSE AND "ref_prop" = 2"""
            );
            assertTrue(statement.getResultSet().next());
            assertEquals(statement.getResultSet().getInt(1), 1);
        }
    }

    @DataProvider(name = "selectParameters")
    Object[][] selectParameters() {
        return new Object[][] {
            {
                SelectQuery.wildcard("_test_table_select_faculty"),
                List.of(
                    List.of(IntegerValue.of("_id", 1L), IntegerValue.of("_created", System.currentTimeMillis()),
                        StringValue.of("name", "Law school")),
                    List.of(IntegerValue.of("_id", 2L), IntegerValue.of("_created", System.currentTimeMillis()),
                        StringValue.of("name", "Computer science"))
                )
            },
            {
                SelectQuery.of("_test_table_select_faculty", List.of(SelectProperty.of("name"))),
                List.of(
                    List.of(StringValue.of("name", "Law school")),
                    List.of(StringValue.of("name", "Computer science"))
                )
            },
            {
                SelectQuery.of("_test_table_select_faculty", List.of(SelectProperty.of("_id"), SelectProperty.of("name"))),
                List.of(
                    List.of(IntegerValue.of("_id", 1L), StringValue.of("name", "Law school")),
                    List.of(IntegerValue.of("_id", 2L), StringValue.of("name", "Computer science"))
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_faculty",
                    List.of(SelectProperty.of("_id"), SelectProperty.of("name"))
                ),
                List.of(
                    List.of(IntegerValue.of("_id", 1L), StringValue.of("name", "Law school")),
                    List.of(IntegerValue.of("_id", 2L), StringValue.of("name", "Computer science"))
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("married"),
                        SelectProperty.of("faculty")
                    )
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        BooleanValue.of("married", null),
                        IntegerValue.of("faculty", 1L)
                    ),
                    List.of(
                        StringValue.of("name", "Donald Knuth"),
                        IntegerValue.of("age", 85L),
                        DecimalValue.of("salary", new BigDecimal("270000.000")),
                        BooleanValue.of("married", true),
                        IntegerValue.of("faculty", 2L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("married"),
                        SelectProperty.of("faculty")
                    ),
                    ReferencePredicate.in("faculty", List.of(2, 3, 4))
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Donald Knuth"),
                        IntegerValue.of("age", 85L),
                        DecimalValue.of("salary", new BigDecimal("270000.000")),
                        BooleanValue.of("married", true),
                        IntegerValue.of("faculty", 2L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("married"),
                        SelectProperty.of("faculty")
                    ),
                    ReferencePredicate.notIn("faculty", List.of(2, 3, 4))
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        BooleanValue.of("married", null),
                        IntegerValue.of("faculty", 1L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("married"),
                        SelectProperty.of("faculty")
                    ),
                    ReferencePredicate.in("faculty", List.of(3, 4))
                ),
                List.of()
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("married"),
                        SelectProperty.of("faculty")
                    ),
                    IntegerPredicate.lse("age", 60L)
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        BooleanValue.of("married", null),
                        IntegerValue.of("faculty", 1L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("married"),
                        SelectProperty.of("faculty")
                    ),
                    DecimalPredicate.gt("salary", new BigDecimal("280000"))
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        BooleanValue.of("married", null),
                        IntegerValue.of("faculty", 1L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("faculty")
                    ),
                    BooleanPredicate.eq("married", true)
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Donald Knuth"),
                        IntegerValue.of("age", 85L),
                        DecimalValue.of("salary", new BigDecimal("270000.000")),
                        IntegerValue.of("faculty", 2L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("faculty")
                    ),
                    DecimalPredicate.gt("salary", new BigDecimal("270000"))
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        IntegerValue.of("faculty", 1L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("faculty")
                    ),
                    DecimalPredicate.gt("salary", new BigDecimal("270000"))
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        IntegerValue.of("faculty", 1L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("faculty")
                    ),
                    StringPredicate.like("name", "%King%")
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Charles Kingsfield"),
                        IntegerValue.of("age", 60L),
                        DecimalValue.of("salary", new BigDecimal("300000.595")),
                        IntegerValue.of("faculty", 1L)
                    )
                )
            },
            {
                SelectQuery.of(
                    "_test_table_select_teacher",
                    List.of(
                        SelectProperty.of("name"),
                        SelectProperty.of("age"),
                        SelectProperty.of("salary"),
                        SelectProperty.of("faculty")
                    ),
                    StringPredicate.notLike("name", "%King%")
                ),
                List.of(
                    List.of(
                        StringValue.of("name", "Donald Knuth"),
                        IntegerValue.of("age", 85L),
                        DecimalValue.of("salary", new BigDecimal("270000.000")),
                        IntegerValue.of("faculty", 2L)
                    )
                )
            }
        };
    }

    @BeforeGroups(value = {"testTableSelect"})
    void selectTestsSetup() throws SQLException {
        var err = service.createTable(
            NewTable.builder()
                .key("_test_table_select_faculty")
                .name("Faculty")
                .description("University faculty")
                .properties(List.of(StringProperty.builder().key("name").name("name").notNull(true).build()))
                .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation faculty, got " + err.orElse(null));
        err = service.createTable(
            NewTable.builder()
                .key("_test_table_select_teacher")
                .name("Teacher")
                .description("Teachers")
                .properties(List.of(
                    StringProperty.builder().key("name").name("name")
                        .constraints(Set.of(PropertyConstraint.maxLength(100), PropertyConstraint.minLength(2)))
                        .build(),
                    IntegerProperty.builder().key("age").name("age")
                        .constraints(Set.of(PropertyConstraint.greaterOrEqual(16L))).build(),
                    DecimalProperty.builder().key("salary").name("salary").precision(10).scale(3).build(),
                    BooleanProperty.builder().key("married").name("married").build(),
                    ReferenceProperty.builder().key("faculty").name("faculty").refTableKey("_test_table_select_faculty")
                        .notNull(true).name("Faculty teacher belongs to").build()
                ))
                .build()
        );
        assertFalse(err.isPresent(), "Expected no error during teacher creation, got " + err.orElse(null));

        err = service.addRow("_test_table_select_faculty", List.of(StringValue.of("name", "Law school")));
        assertFalse(err.isPresent(), "Expected no error during adding faculty, got " + err.orElse(null));
        err = service.addRow("_test_table_select_faculty", List.of(StringValue.of("name", "Computer science")));
        assertFalse(err.isPresent(), "Expected no error during adding faculty, got " + err.orElse(null));

        err = service.addRow("_test_table_select_teacher", List.of(
            StringValue.of("name", "Charles Kingsfield"),
            IntegerValue.of("age", 60L),
            DecimalValue.of("salary", new BigDecimal("300000.595")),
            ReferenceValue.of("faculty", 1)
        ));
        assertFalse(err.isPresent(), "Expected no error during adding teacher, got " + err.orElse(null));

        err = service.addRow("_test_table_select_teacher", List.of(
            StringValue.of("name", "Donald Knuth"),
            IntegerValue.of("age", 85L),
            BooleanValue.of("married", true),
            DecimalValue.of("salary", new BigDecimal("270000")),
            ReferenceValue.of("faculty", 2)
        ));
        assertFalse(err.isPresent(), "Expected no error during adding teacher, got " + err.orElse(null));
    }

    @Test(dataProvider = "selectParameters", groups = {"testTableSelect"})
    void testTableSelect(SelectQuery selectQuery, List<List<ColumnValue<?>>> expectedResult) throws SQLException {
        var selectRes = service.select(selectQuery);

        assertFalse(
            selectRes.error().isPresent(),
            "Expected no error during selecting, got " + selectRes.error().orElse(null)
        );
        assertEquals(selectRes.value().size(), expectedResult.size());
        for (int i = 0; i < selectRes.value().size(); i += 1) {
            var actualRow = selectRes.value().get(i);
            assertEquals(actualRow.size(), expectedResult.get(i).size());

            for (int j = 0; j < actualRow.size(); j += 1) {
                if (actualRow.get(j).key().equals("_created")) {
                    // do not want to spoil the query generator by clocking timestamps via the APP (though, mby I should)
                    // TODO: mby mock somehow the time outside of the app?
                    continue;
                }

                assertEquals(actualRow.get(j).value(), expectedResult.get(i).get(j).value());
            }
        }
    }

    @Test
    void testReferencedRowCantBeDroppedWhenOtherTableRequires() throws SQLException {
        var err = service.createTable(NewTable
            .builder()
            .key("_test_referenced_row_can_not_be_dropped")
            .name("test_name")
            .description("test_description")
            .properties(List.of(StringProperty.builder().key("name").name("name").notNull(true).build()))
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow("_test_referenced_row_can_not_be_dropped", List.of(StringValue.of("name", "123")));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.createTable(NewTable
            .builder()
            .key("_test_referenced_table_with_reference")
            .name("with_reference")
            .description("with_reference")
            .properties(List.of(
                    ReferenceProperty
                        .builder()
                        .key("name")
                        .name("name")
                        .refTableKey("_test_referenced_row_can_not_be_dropped")
                        .notNull(true)
                        .defaultValue(0)
                        .build()
                )
            )
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow("_test_referenced_table_with_reference", List.of(ReferenceValue.of("name", 1)));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.deleteRow(DeleteRow.of("_test_referenced_row_can_not_be_dropped", 1));
        assertTrue(err.isPresent(), "Expected error, but got no error");
        assertEquals(((OperationError) err.get()).code(), PROPERTY_CHECK_VIOLATED);
    }

    @Test
    void testReferencedRowDeleteSetsToNullWhenNotRequired() throws SQLException {
        var err = service.createTable(NewTable
            .builder()
            .key("_reference_row_delete_sets_to_null")
            .name("test_name")
            .description("test_description")
            .properties(List.of(StringProperty.builder().key("name").name("name").notNull(true).build()))
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow(
            "_reference_row_delete_sets_to_null",
            List.of(StringValue.of("name", "123"))
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.createTable(NewTable
            .builder()
            .key("_reference_row_delete_sets_to_null_reference")
            .name("with_reference")
            .description("with_reference")
            .properties(List.of(
                    ReferenceProperty
                        .builder()
                        .key("name")
                        .name("name")
                        .refTableKey("_reference_row_delete_sets_to_null")
                        .build()
                )
            )
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow(
            "_reference_row_delete_sets_to_null_reference",
            List.of(ReferenceValue.of("name", 1))
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.deleteRow(DeleteRow.of("_reference_row_delete_sets_to_null", 1));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        try (
            final var connection = createConnection();
            final var statement = connection.createStatement()) {
            statement.execute("""
                SELECT *
                FROM custom._reference_row_delete_sets_to_null_reference
                WHERE _id=1 AND name IS NULL"""
            );
            assertTrue(statement.getResultSet().next());
            assertEquals(statement.getResultSet().getInt(1), 1);
        }
    }

    // when property is not required and yet it has a default value, delete of the referenced row should set
    // value on the table that has reference to default value
    @Test
    void testReferencedRowDeleteSetsToDefaultWhenDefaultValueSpecified() throws SQLException {
        var err = service.createTable(NewTable
            .builder()
            .key("_reference_row_delete_sets_to_default")
            .name("test_name")
            .description("test_description")
            .properties(List.of(StringProperty.builder().key("name").name("name").notNull(true).build()))
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow("_reference_row_delete_sets_to_default",
            List.of(StringValue.of("name", "123")));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));
        err = service.addRow("_reference_row_delete_sets_to_default",
            List.of(StringValue.of("name", "321")));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.createTable(NewTable
            .builder()
            .key("_reference_row_delete_sets_to_default_referencing")
            .name("with_reference")
            .description("with_reference")
            .properties(List.of(
                    ReferenceProperty
                        .builder()
                        .key("name")
                        .name("name")
                        .defaultValue(1)
                        .refTableKey("_reference_row_delete_sets_to_default")
                        .build()
                )
            )
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));
        err = service.addRow(
            "_reference_row_delete_sets_to_default_referencing",
            List.of(ReferenceValue.of("name", 2))
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.deleteRow(DeleteRow.of("_reference_row_delete_sets_to_default", 2));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        try (
            final var connection = createConnection();
            final var statement = connection.createStatement()
        ) {
            statement.execute("""
                SELECT *
                FROM custom._reference_row_delete_sets_to_default_referencing
                WHERE _id=1 AND name=1"""
            );
            assertTrue(statement.getResultSet().next());
            assertEquals(statement.getResultSet().getInt(1), 1);
        }
    }

    @Test
    void testUpdateOfReferencedRowIdIsRestricted() throws SQLException {
        var err = service.createTable(NewTable
            .builder()
            .key("_update_of_referenced_row_id_is_restricted")
            .name("test_name")
            .description("test_description")
            .properties(List.of(StringProperty.builder().key("name").name("name").notNull(true).build()))
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow("_update_of_referenced_row_id_is_restricted",
            List.of(StringValue.of("name", "123")));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.createTable(NewTable
            .builder()
            .key("_update_of_referenced_row_id_is_restricted_referencing")
            .name("with_reference")
            .description("with_reference")
            .properties(List.of(
                    ReferenceProperty
                        .builder()
                        .key("name")
                        .name("name")
                        .defaultValue(1)
                        .refTableKey("_update_of_referenced_row_id_is_restricted")
                        .build()
                )
            )
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));
        err = service.addRow(
            "_update_of_referenced_row_id_is_restricted_referencing",
            List.of(ReferenceValue.of("name", 1))
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        try (final var connection = createConnection();
             final var statement = connection.createStatement()
        ) {
            statement.executeUpdate("""
                UPDATE _update_of_referenced_row_id_is_restricted
                SET _id=2
                WHERE _id=1;"""
            );
            statement.close();
            fail();
        } catch (SQLException ex) {
            // expected exception
        }
    }

    @Test
    void testFailsToDropNonExistingTable() {
        final var err = service.dropTable(DropTable.of("_does_not_exist"));
        assertTrue(err.isPresent(), "Expected error, but got no errors");
        assertEquals(((OperationError) err.get()).code(), TABLE_DOES_NOT_EXIST);
    }

    @Test
    void testAddingPropertyToTable() {
        var err = service.createTable(NewTable
            .builder()
            .key("_add_property_to_table")
            .name("test_name")
            .description("test_description")
            .properties(List.of(StringProperty.builder().key("name").name("name").build()))
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addProperty(
            AddProperty
                .builder()
                .tableKey("_add_property_to_table")
                .property(IntegerProperty.builder().key("added_prop").name("added_prop").notNull(true)
                    .constraints(Set.of(PropertyConstraint.lessOrEqual(25L))).build())
                .build()
        );

        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow(InsertRow.of("_add_property_to_table", List.of(IntegerValue.of("added_prop", 20L))));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        var res = service.select(SelectQuery.of("_add_property_to_table", List.of(SelectProperty.of("added_prop"))));
        assertFalse(res.error().isPresent(), "Expected no error during creation, got " + res.error().orElse(null));

        assertEquals(res.value().size(), 1);
        assertEquals(res.value().get(0).size(), 1);
        assertEquals(res.value().get(0).get(0).key(), "added_prop");
        assertEquals(res.value().get(0).get(0).value(), 20L);
    }

    @Test
    void testDroppingPropertyFromTable() {
        var err = service.createTable(NewTable
            .builder()
            .key("_drop_property_from_table")
            .name("test_name")
            .description("test_description")
            .properties(List.of(StringProperty.builder().key("name").name("name").build()))
            .build()
        );
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.addRow(InsertRow.of("_drop_property_from_table", List.of(StringValue.of("name", "Johm"))));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        err = service.dropProperty(DropProperty.of("_drop_property_from_table", "name"));
        assertFalse(err.isPresent(), "Expected no error during creation, got " + err.orElse(null));

        var res = service.select(SelectQuery.wildcard("_drop_property_from_table"));
        assertFalse(res.error().isPresent(), "Expected no error during creation, got " + res.error().orElse(null));

        assertEquals(res.value().size(), 1);
        assertEquals(res.value().get(0).size(), 2);
        assertFalse(res.value().get(0).stream().map(ColumnValue::key).anyMatch(k -> k.equals("name")));
    }

    private Connection createConnection() {
        try {
            String url =
                "jdbc:postgresql://localhost:%d/%s".formatted(container.getMappedPort(5432), DB_NAME);
            Properties props = new Properties();
            props.setProperty("user", "postgres");
            props.setProperty("password", "test");
            return DriverManager.getConnection(url, props);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
