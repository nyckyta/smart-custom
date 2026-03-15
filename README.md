# smart-custom

A Java library for managing dynamic, virtual database tables at runtime without application restarts or redeployment. Built on top of PostgreSQL for now, it provides a type-safe API for creating schemas, defining columns with constraints, and performing full CRUD operations on user-defined tables.

## Overview

`smart-custom` solves the problem of dynamic schema management: situations where end-users or integrations need to define custom data structures at runtime, without touching the codebase or database migrations. The library manages the full lifecycle of virtual tables — from creation and schema evolution to querying and deletion.

## Features

- **Dynamic table management** — create, modify, and drop tables at runtime
- **Rich type system** — INTEGER, STRING, BOOLEAN, DECIMAL, REFERENCE (foreign keys), and LIST types
- **Constraints** — min/max values, string length limits, nullable/non-null, unique constraints
- **Full CRUD** — insert, update, delete, and flexible select with predicate-based filtering
- **SQL injection protection** — parameterized queries and input escaping
- **Circular reference detection** — prevents circular foreign key chains
- **Transaction safety** — SERIALIZABLE isolation with configurable retry logic
- **Result-based error handling** — uses `Return<T>` type instead of exceptions

## Requirements

- Java 21+
- PostgreSQL

## Usage

```java
Config config = new Config(
    "custom",   // schema name
    100,        // connection pool size
    5000,       // network timeout (ms)
    15          // transaction retry attempts
);

VirtualTableService service = new DefaultVirtualTableService(connectionSupplier, config);

// Create a table
Return<Void> result = service.createTable(
    new NewTable("products")
        .withProperty(new StringProperty("name", new PropertyConstraint(false, 1, 255)))
        .withProperty(new IntegerProperty("stock", new PropertyConstraint(false, 0, null)))
        .withProperty(new DecimalProperty("price", new PropertyConstraint(false, null, null)))
);

// Insert a row
service.addRow(new InsertRow("products")
    .withValue("name", new StringValue("Widget"))
    .withValue("stock", new IntegerValue(42))
    .withValue("price", new DecimalValue(new BigDecimal("9.99")))
);

// Query rows
Return<List<Map<String, Object>>> rows = service.select(
    new SelectQuery("products")
        .where(new IntegerPredicate("stock", IntegerPredicate.Op.GT, 0))
);
```

## Build

```bash
./gradlew build
```

Run tests (requires Docker for TestContainers):

```bash
./gradlew test
```