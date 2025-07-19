package edu.ukma.smart.virtual.errors;

public record OperationError(ErrorCode code) implements Err {

    public static OperationError of(ErrorCode errorCode) {
        return new OperationError(errorCode);
    }

    public enum ErrorCode {
        // TODO: make more detailed error codes, potentially for every kind of the use case
        // operation violates rules of the current schema i.e. violates constraints, value is out of the range etc.
        PROPERTY_CHECK_VIOLATED,
        // operation on table that does not exist happens
        TABLE_DOES_NOT_EXIST,
    }
}
