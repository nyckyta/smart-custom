package edu.ukma.smart.virtual.metadata;

import edu.ukma.smart.virtual.DefaultVirtualTableService;
import edu.ukma.smart.virtual.Utils;
import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.testcontainers.containers.GenericContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CircularReferencesDetection {

    private static final String DB_NAME = "circular_references_detection";

    private GenericContainer<?> container;

    @BeforeClass
    void startContainer() throws IOException, InterruptedException, SQLException {
        container = new GenericContainer<>("postgres:latest")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_PASSWORD", "test");
        container.start();
        container.execInContainer("psql",
            "-U", "postgres",
            "-c", "CREATE DATABASE %s;".formatted(DB_NAME));
    }

    @Test
    void testTwoTablesCantHaveDirectReferencesOnEachOtherSimultaneously() {
        var service = new DefaultVirtualTableService(this::createConnection);
        var firstTableKey = "_" + Utils.generateRandomString(25);
        var secondTableKey = "_" + Utils.generateRandomString(25);
        var err = service.createTable(NewTable
            .builder()
            .key(firstTableKey)
            .name("first")
            .description("first table")
            .properties(List.of(StringProperty.builder().key("property_one").name("property_1").build()))
            .build());

        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));

        err = service.createTable(NewTable
            .builder()
            .key(secondTableKey)
            .name("second")
            .description("second table")
            .properties(List.of(ReferenceProperty.builder().key("referencing_first").name("referencing_first").refTableKey(firstTableKey).build()))
            .build());

        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));


        err = service.addProperty(
            AddProperty
                .builder()
                .tableKey(firstTableKey)
                .property(ReferenceProperty.builder().key("referencing_second").name("referencing_second").refTableKey(secondTableKey).build())
                .build()
        );

        Assert.assertTrue(err.isPresent(), "Expected error, but got no error");
        Assert.assertEquals(((InputValidationErr)err.get()).code(), InputValidationErr.ErrorCode.CIRCULAR_REFERENCE_DETECTED);

        var properties = service.getProperties(firstTableKey);
        Assert.assertFalse(properties.error().isPresent(), "Expected no error, but got " + properties.error().orElse(null));
        Assert.assertEquals(properties.value().size(), 1);
        Assert.assertEquals(properties.value().get(0).key(), "property_one");
    }

    @Test
    void testDeepCircularReferencesAreDetected() {
        var service = new DefaultVirtualTableService(this::createConnection);
        var firstTableKey = "_" + Utils.generateRandomString(25);
        var err = service.createTable(NewTable
            .builder()
            .key(firstTableKey)
            .name("first")
            .description("first table")
            .properties(List.of(StringProperty.builder().key("property_one").name("property_1").build()))
            .build());

        Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));
        var lastTableKey = firstTableKey;
        for (int i = 0; i < 99; i += 1) {
            var tableKey = "_" + Utils.generateRandomString(25);
            err = service.createTable(NewTable
                .builder()
                .key(tableKey)
                .name(tableKey)
                .description(tableKey)
                .properties(List.of(ReferenceProperty.builder().key("referencing").name("referencing_first").refTableKey(lastTableKey).build()))
                .build());
            lastTableKey = tableKey;
            Assert.assertFalse(err.isPresent(), "Expected no error, but got " + err.orElse(null));
        }

        err = service.addProperty(
            AddProperty
                .builder()
                .tableKey(firstTableKey)
                .property(ReferenceProperty.builder().key("referencing_second").name("referencing_second").refTableKey(lastTableKey).build())
                .build()
        );

        Assert.assertTrue(err.isPresent(), "Expected error, but got no error");
        Assert.assertEquals(((InputValidationErr)err.get()).code(), InputValidationErr.ErrorCode.CIRCULAR_REFERENCE_DETECTED);
        var properties = service.getProperties(firstTableKey);
        Assert.assertFalse(properties.error().isPresent(), "Expected no error, but got " + properties.error().orElse(null));
        Assert.assertEquals(properties.value().size(), 1);
        Assert.assertEquals(properties.value().get(0).key(), "property_one");
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
