package edu.ukma.smart.virtual.metadata;

import static edu.ukma.smart.virtual.Utils.generateRandomString;

import edu.ukma.smart.virtual.Config;
import edu.ukma.smart.virtual.DefaultVirtualTableService;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.SelectProperty;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.IntegerValue;
import edu.ukma.smart.virtual.dml.values.StringValue;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CrudTest {

    private static final String DB_NAME = "crud_db";
    private static final Config CONFIG = new Config();

    private GenericContainer<?> container;

    @BeforeClass
    void startContainer() throws IOException, InterruptedException, SQLException {
        container = new GenericContainer<>("postgres:17")
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
    }

    // 10 seconds timeout
    @Test(timeOut = 10000)
    void testConcurrentUpdatesDoNotCauseErrors() throws InterruptedException {
        var service = new DefaultVirtualTableService(() -> {
            try {
                return createConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, CONFIG);
        var tableName = "_" + generateRandomString(15);
        var err = service.createTable(
            NewTable.builder()
                .key(tableName)
                .name("users")
                .description("Description")
                .properties(List.of(
                    StringProperty.builder().key("name").name("name").build(),
                    IntegerProperty.builder().key("age").name("age").build()
                )).build()
        );

        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));
        err = service.addRow(InsertRow.of(tableName, List.of(StringValue.of("name", "Bob"), IntegerValue.of("age", 42L))));
        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));

        var tasks = new ArrayList<Future<?>>();
        var executor = Executors.newFixedThreadPool(4);
        for (var i = 0; i < 12; i++) {
            tasks.add(executor.submit(() -> {
                var _err =
                    service.updateRow(
                        UpdateRow.of(tableName, 1, List.of(StringValue.of("name", "Bob-" + Thread.currentThread().getName()))));
                if (_err.isPresent()) {
                    throw new RuntimeException("Error updating user " + _err.get());
                }
            }));
        }

        for (var future : tasks) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Assert.fail("Execution failed with " + e.getMessage());
            }
        }

        err = service.dropTable(DropTable.of(tableName));
        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));
        executor.shutdown();
        executor.close();
    }

    // 10 seconds timeout
    @Test(timeOut = 10000)
    void testConcurrentReadsDoNotCauseErrors() throws InterruptedException {
        var service = new DefaultVirtualTableService(() -> {
            try {
                return createConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, CONFIG);
        var tableName = "_" + generateRandomString(15);
        var err = service.createTable(
            NewTable.builder()
                .key(tableName)
                .name("clients")
                .description("Description")
                .properties(
                    List.of(StringProperty.builder().key("name").name("name").build(), IntegerProperty.builder().key("age").name("age").build()))
                .build()
        );

        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));

        for (var i = 0; i < 30; i++) {
            err = service.addRow(InsertRow.of(tableName, List.of(StringValue.of("name", "Bob" + i), IntegerValue.of("age", (long) i))));
            Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));
        }

        var tasks = new ArrayList<Future<?>>();
        var executor = Executors.newFixedThreadPool(4);
        for (var i = 0; i < 1000; i++) {
            tasks.add(executor.submit(() -> {
                var ret = service.select(SelectQuery.of(tableName, List.of(SelectProperty.of("name"))));
                if (ret.error().isPresent()) {
                    throw new RuntimeException("Error updating user " + ret.error().get());
                }
            }));
        }

        for (var future : tasks) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Assert.fail("Execution failed with " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.close();
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
