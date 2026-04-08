package edu.ukma.smart.virtual.metadata;

import edu.ukma.smart.virtual.Config;
import edu.ukma.smart.virtual.DefaultVirtualTableService;
import edu.ukma.smart.virtual.VirtualTableService;
import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.constraints.UniqueConstraint;
import edu.ukma.smart.virtual.ddl.create.*;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MetadataTest {

    private static final String DB_NAME = "metadata_db";
    private static final Config CONFIG = new Config();

    private GenericContainer<?> container;

    @BeforeClass
    void startContainer() throws IOException, InterruptedException {
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

    @AfterClass(alwaysRun = true)
    void stopContainer() {
        container.stop();
    }

    @Test
    void testGetTablesWhenNoExists() {
        var service = new DefaultVirtualTableService(this::createConnection, CONFIG);
        var ret = service.getTables();
        Assert.assertTrue(ret.error().isEmpty(), "Expected no errors, but got " + ret.error().orElse(null));
        Assert.assertEquals(ret.value().size(), 0);
    }

    @Test
    void testGetTablesReturnMetadataAboutTables() {
        var service = new DefaultVirtualTableService(this::createConnection, CONFIG);
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

        dropTables(service, "_clients", "_orders");
    }

    @Test
    void testGetPropertiesReturnFullInformationAboutTable() {
        var service = new DefaultVirtualTableService(this::createConnection, CONFIG);
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
        dropTables(service, "_communicators", "_client_companies");
    }

    @Test(dataProvider = "addPropertyWithConstraintProvider")
    void testAddPropertyWithConstraint(Object propertyToAdd) {
        var service = new DefaultVirtualTableService(this::createConnection, CONFIG);
        try {
            var err = service.createTable(
                NewTable
                    .builder()
                    .key("_cathedra")
                    .name("Cathedra reference")
                    .description("Cathedra values")
                    .build()
            );
            Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));
            err = service.createTable(
                NewTable
                    .builder()
                    .key("_communicators")
                    .name("Communication channels")
                    .description("List of all people per company we can communicate with")
                    .properties(List.of())
                    .build()
            );

            Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));
            err = service.addProperty(
                AddProperty
                    .builder()
                    .tableKey("_communicators")
                    .property((Property) propertyToAdd)
                    .build()
            );
            Assert.assertFalse(err.isPresent(), "Expected no errors, but got " + err.orElse(null));

            var props = service.getProperties("_communicators");
            Assert.assertFalse(props.error().isPresent());
            Assert.assertEquals(props.value().size(), 1);
            Assert.assertEquals(props.value().getFirst(), propertyToAdd);
        } finally {
            dropTables(service, "_communicators");
            dropTables(service, "_cathedra");
        }
    }

    @DataProvider(name = "addPropertyWithConstraintProvider")
    public static Object[] addPropertyWithConstraintTest() {
        var strPropBuilder = StringProperty
            .builder()
            .key("str_prop")
            .name("str prop");
        var intPropBuilder = IntegerProperty
            .builder()
            .key("int_prop")
            .name("int prop");
        var boolPropBuilder = BooleanProperty
            .builder()
            .key("bool_prop")
            .name("bool prop");
        var decimalPropBuilder = DecimalProperty
            .builder()
            .key("decimal_prop")
            .name("decimal prop")
            .precision(3)
            .scale(3);
        var referencePropBuilder = ReferenceProperty
            .builder()
            .key("reference_prop")
            .name("reference prop")
            .refTableKey("_cathedra");

        return new Object[]{
            strPropBuilder.constraints(
                Set.of(PropertyConstraint.notIn(List.of("2L", "5L", "10L").toArray(String[]::new)))).build(),
            strPropBuilder.constraints(
                Set.of(PropertyConstraint.in(List.of("2L", "5L", "10L").toArray(String[]::new)))).build(),
            strPropBuilder.constraints(Set.of(PropertyConstraint.greaterThan("2232"))).build(),
            strPropBuilder.constraints(Set.of(PropertyConstraint.greaterOrEqual("2323"))).build(),
            strPropBuilder.constraints(Set.of(PropertyConstraint.lessThan("252432"))).build(),
            strPropBuilder.constraints(Set.of(PropertyConstraint.lessOrEqual("252432"))).build(),
            strPropBuilder.constraints(Set.of(PropertyConstraint.maxLength(25))).build(),
            strPropBuilder.constraints(Set.of(PropertyConstraint.minLength(20))).build(),
            strPropBuilder.constraints(Set.of(UniqueConstraint.of("str_prop"))).build(),
            // int property
            intPropBuilder.constraints(
                Set.of(PropertyConstraint.notIn(List.of(2L, 5L, 10L).toArray(Long[]::new)))).build(),
            intPropBuilder.constraints(
                Set.of(PropertyConstraint.in(List.of(2L, 5L, 10L).toArray(Long[]::new)))).build(),
            intPropBuilder.constraints(Set.of(PropertyConstraint.greaterThan(2232))).build(),
            intPropBuilder.constraints(Set.of(PropertyConstraint.greaterOrEqual(2323))).build(),
            intPropBuilder.constraints(Set.of(PropertyConstraint.lessThan(252432))).build(),
            intPropBuilder.constraints(Set.of(PropertyConstraint.lessOrEqual(252432))).build(),
            intPropBuilder.constraints(Set.of(UniqueConstraint.of("int_prop"))).build(),
            // boolean property
            boolPropBuilder.constraints(Set.of(UniqueConstraint.of("bool_prop"))).build(),
            // decimal properties
            decimalPropBuilder.constraints(
                Set.of(PropertyConstraint.notIn(List.of(BigDecimal.valueOf(2), BigDecimal.valueOf(5L), BigDecimal.valueOf(10L)).toArray(BigDecimal[]::new)))).build(),
            decimalPropBuilder.constraints(
                Set.of(PropertyConstraint.notIn(List.of(BigDecimal.valueOf(0.2), BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.11)).toArray(BigDecimal[]::new)))).build(),
            decimalPropBuilder.constraints(
                Set.of(PropertyConstraint.in(List.of(BigDecimal.valueOf(2L), BigDecimal.valueOf(5L), BigDecimal.valueOf(10L)).toArray(BigDecimal[]::new)))).build(),
            decimalPropBuilder.constraints(
                Set.of(PropertyConstraint.in(List.of(BigDecimal.valueOf(0.2), BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.11)).toArray(BigDecimal[]::new)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.greaterThan(BigDecimal.valueOf(2232)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.greaterThan(BigDecimal.valueOf(22.32)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.greaterOrEqual(BigDecimal.valueOf(2323)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.greaterOrEqual(BigDecimal.valueOf(23.23)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.lessThan(BigDecimal.valueOf(252432)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.lessThan(BigDecimal.valueOf(252.432)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.lessOrEqual(BigDecimal.valueOf(252432)))).build(),
            decimalPropBuilder.constraints(Set.of(PropertyConstraint.lessOrEqual(BigDecimal.valueOf(252.432)))).build(),
            decimalPropBuilder.constraints(Set.of(UniqueConstraint.of("decimal_prop"))).build(),
            //reference
            referencePropBuilder.constraints(
                Set.of(PropertyConstraint.notIn(List.of(2L, 5L, 10L).toArray(Long[]::new)))).build(),
            referencePropBuilder.constraints(
                Set.of(PropertyConstraint.in(List.of(2L, 5L, 10L).toArray(Long[]::new)))).build(),
            referencePropBuilder.constraints(Set.of(PropertyConstraint.greaterThan(2232))).build(),
            referencePropBuilder.constraints(Set.of(PropertyConstraint.greaterOrEqual(2323))).build(),
            referencePropBuilder.constraints(Set.of(PropertyConstraint.lessThan(252432))).build(),
            referencePropBuilder.constraints(Set.of(PropertyConstraint.lessOrEqual(252432))).build(),
            referencePropBuilder.constraints(Set.of(UniqueConstraint.of("reference_prop"))).build(),
        };
    }

    private Connection createConnection() {
        try {
            String url =
                "jdbc:postgresql://localhost:%d/%s".formatted(container.getMappedPort(5432), DB_NAME);
            Properties props = new Properties();
            props.setProperty("user", "postgres");
            props.setProperty("password", "test");
            return DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void dropTables(VirtualTableService service, String... tableNames) {
        for (String tableName : tableNames) {
            service.dropTable(DropTable.of(tableName));
        }
    }

}
