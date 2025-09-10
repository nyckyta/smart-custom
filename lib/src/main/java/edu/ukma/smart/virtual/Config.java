package edu.ukma.smart.virtual;

public record Config(
    int connectionPoolLimit,
    int connectionNetworkTimeoutMs,
    int connectionAcquireRetryAttempts,
    int connectionAcquireReleaseTimeoutMs,
    String schema,
    int transactionRetryAttempts
) {

    public Config() {
        this(100, 5000, 5, 10000, "custom", 15);
    }

    public Config(
        int connectionPoolLimit,
        int connectionNetworkTimeoutMs,
        int connectionAcquireRetryAttempts,
        int connectionAcquireReleaseTimeoutMs,
        String schema,
        int transactionRetryAttempts) {
        if (connectionPoolLimit < 1) {
            throw new IllegalArgumentException("connectionPoolLimit must be greater than 0");
        }
        this.connectionPoolLimit = connectionPoolLimit;
        // JDBC API uses 4 bytes int milliseconds, and we internally use progressive pause between pauses. So the below check is here
        // to avoid unexpected integer overflow
        int potentiallyOverflowedValue = connectionAcquireRetryAttempts * connectionAcquireReleaseTimeoutMs;
        if (connectionNetworkTimeoutMs < 1 || potentiallyOverflowedValue < 1) {
            throw new IllegalArgumentException(
                "connectionNetworkTimeoutMs must be greater than 0 or, " +
                "connectionAcquireRetryAttempts * connectionAcquireReleaseTimeoutMs should not cause overflow of 4 bytes int");
        }
        this.connectionNetworkTimeoutMs = connectionNetworkTimeoutMs;

        if (connectionAcquireRetryAttempts < 0) {
            throw new IllegalArgumentException("connectionAcquireRetryAttempts must be equal to 0 or greater");
        }
        this.connectionAcquireRetryAttempts = connectionAcquireRetryAttempts;

        if (connectionAcquireReleaseTimeoutMs < 1) {
            throw new IllegalArgumentException("connectionAcquireReleaseTimeoutMs must be greater than 0");
        }
        this.connectionAcquireReleaseTimeoutMs = connectionAcquireReleaseTimeoutMs;

        if (schema == null) {
            this.schema = "custom";
        } else if (schema.startsWith("pg_")) {
            throw new IllegalArgumentException("schema must not start with 'pg_'");
        } else if (schema.equalsIgnoreCase("public") && !System.getenv("ALLOW_PUBLIC_SCHEMA").equalsIgnoreCase("true")) {
            throw new IllegalArgumentException(
                "Public schema is not allowed, it is not recommended to use it, instead keep dynamic tables in separate schema." +
                " If you insist, set environment variable \"ALLOW_PUBLIC_SCHEMA=true\"");
        } else {
            this.schema = schema;
        }

        if (transactionRetryAttempts < 0) {
            throw new IllegalArgumentException("transactionRetryAttempts must be equal to 0 or greater");
        }

        this.transactionRetryAttempts = transactionRetryAttempts;
    }
}
