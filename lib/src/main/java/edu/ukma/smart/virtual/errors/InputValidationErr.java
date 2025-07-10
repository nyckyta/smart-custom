package edu.ukma.smart.virtual.errors;

public record InputValidationErr(String msg) implements Err {

    public static InputValidationErr error(String err) {
        return new InputValidationErr(err);
    }
}
