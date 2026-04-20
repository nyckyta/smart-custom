# smart-custom

A Java library for managing dynamic, virtual database tables at runtime without application restarts or redeployment. Built on top of PostgreSQL for now, it provides a type-safe API for creating schemas, defining columns with constraints, and performing full CRUD operations on user-defined tables.

## Overview

`smart-custom` solves the problem of dynamic schema management: situations where end-users or integrations need to define custom data structures at runtime, without touching the codebase or database migrations. The library manages the full lifecycle of virtual tables — from creation and schema evolution to querying and deletion.

## Features

- **Dynamic table management** — create, modify, and drop tables at runtime
- **Rich type system** — INTEGER, STRING, BOOLEAN, DECIMAL, REFERENCE (foreign keys) types
- **Constraints** — min/max values, string length limits, nullable/non-null, unique constraints
- **Full CRUD** — insert, update, delete, and flexible select with predicate-based filtering
- **SQL injection protection** — parameterized queries and input escaping
- **Circular reference detection** — prevents circular foreign key chains
- **Transaction safety** — SERIALIZABLE isolation with configurable retry logic
- **Result-based error handling** — uses `Return<T>` type instead of exceptions

## Requirements

- Java 21+
- PostgreSQL

## Build

```bash
./gradlew build
```

Run tests (requires Docker for TestContainers):

```bash
./gradlew test
```