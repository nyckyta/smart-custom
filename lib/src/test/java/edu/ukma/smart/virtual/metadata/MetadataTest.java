package edu.ukma.smart.virtual.metadata;

import edu.ukma.smart.virtual.DefaultVirtualTableService;
import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.constraints.UniqueConstraint;
import edu.ukma.smart.virtual.ddl.create.BooleanProperty;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.IntegerProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
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

    @Test
    void testGetPropertiesReturnFullInformationAboutTable() {
        var service = new DefaultVirtualTableService(connection);
        var companyProperties = List.of(
            StringProperty
                .builder()
                .key("company_name")
                .name("Name of the company")
                .notNull(true)
                .addConstraint(PropertyConstraint.minLength(3))
                .addConstraint(PropertyConstraint.maxLength(100))
                .addConstraint(UniqueConstraint.of("company_name"))
                .build(),
            DecimalProperty
                .builder()
                .key("contract_sum")
                .name("Contract summary")
                .notNull(true)
                .precision(20)
                .scale(3)
                .addConstraint(PropertyConstraint.greaterOrEqual(BigDecimal.valueOf(0.001)))
                .build(),
            BooleanProperty
                .builder()
                .key("has_contract_expired")
                .name("Contract expired")
                .defaultValue(false)
                .notNull(true)
                .build()
        );
        var err = service.createTable(
            NewTable.builder()
                .key("_client_companies")
                .name("Companies")
                .properties(companyProperties)
                .build()
        );
        Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));
        var properties = service.getProperties("_client_companies");
        Assert.assertTrue(properties.error().isEmpty(), "Expected no errors, but got " + properties.error().orElse(null));
        Assert.assertEquals(properties.value(), companyProperties);

        var communicatorProperties = List.of(
            IntegerProperty
                .builder()
                .key("birth_year")
                .name("birth year")
                .description("Year when client born")
                .notNull(true)
                .addConstraint(PropertyConstraint.greaterOrEqual(1920L))
                .addConstraint(PropertyConstraint.lessOrEqual(2010L))
                .build(),
            ReferenceProperty
                .builder()
                .key("company")
                .name("Client company")
                .description("Company the client belongs to")
                .refTableKey("_client_companies")
                .addConstraint(UniqueConstraint.of("company", "name" /*,"job_position"*/))
                .build(),
            StringProperty
                .builder()
                .key("job_position")
                .name("Position in the client company")
                .defaultValue("manager")
                .notNull(true)
                .addConstraint(PropertyConstraint.in(new String[] {"manager", "executive", "IT"}))
                .build(),
            StringProperty
                .builder()
                .key("name")
                .name("Name")
                .description("Full name of the client")
                .notNull(true)
                .addConstraint(PropertyConstraint.maxLength(100))
                .addConstraint(PropertyConstraint.minLength(2))
                .build()
        );

        err = service.createTable(
            NewTable
                .builder()
                .key("_communicators")
                .name("Communication channels")
                .description("List of all people per company we can communicate with")
                .properties(communicatorProperties)
                .build()
        );
        Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));
        var propResult = service.getProperties("_communicators");
        Assert.assertTrue(propResult.error().isEmpty(), "Expected no errors, but got " + propResult.error().orElse(null));
        Assert.assertEquals(propResult.value(), communicatorProperties);
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
