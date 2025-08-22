package edu.ukma.smart.virtual.errors;

public record OperationError(ErrorCode code) implements Err {

    public static OperationError of(ErrorCode errorCode) {
        return new OperationError(errorCode);
    }

    public enum ErrorCode {
        // TODO: make more detailed error codes, potentially for every kind of the use case
        // operation violates rules of the current schema i.e. violates notNull, value is out of the range etc.
        PROPERTY_CHECK_VIOLATED,
        // operation on table that does not exist happens
        TABLE_DOES_NOT_EXIST,
        COLUMN_DOES_NOT_EXIST,

        REPEAT,
        REPEAT_EXHAUSTED,

        // failed to acquire connection
        FAILED_TO_ACQUIRE_CONNECTION,
        FAILED_TO_ACQUIRE_CONNECTION_TIMEOUT,
        FAILED_TO_RELEASE_CONNECTION_TIMEOUT,
        FAILED_TO_RELEASE_CONNECTION,

    }
}
