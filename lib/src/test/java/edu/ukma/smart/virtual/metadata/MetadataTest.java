package edu.ukma.smart.virtual.metadata;

import edu.ukma.smart.virtual.DefaultVirtualTableService;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MetadataTest {

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

    @Test
    void testGetTablesWhenNoExists() {
        var service = new DefaultVirtualTableService(connection);
        var ret = service.getTables();
        Assert.assertTrue(ret.error().isEmpty(), "Expected no errors, but got " + ret.error().orElse(null));
        Assert.assertEquals(ret.value().size(), 0);
    }

    @Test
    void testGetTablesReturnMetadataAboutTables() {
        var service = new DefaultVirtualTableService(connection);
        var err = service.createTable(
            NewTable.builder().key("_clients").name("Company clients").description("List of all clients we have").build());
        Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));
        err = service.createTable(NewTable.builder().key("_orders").name("My orders").build());
        Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));

        var ret = service.getTables();
        Assert.assertTrue(ret.error().isEmpty(), "Expected no errors, but got " + ret.error().orElse(null));
        Assert.assertEquals(ret.value().size(), 2);
        Assert.assertEquals(ret.value().get(0), new Table("_clients", "Company clients", "List of all clients we have"));
        Assert.assertEquals(ret.value().get(1), new Table("_orders", "My orders", null));

        service.dropTable(DropTable.of("_clients"));
        service.dropTable(DropTable.of("_orders"));
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
