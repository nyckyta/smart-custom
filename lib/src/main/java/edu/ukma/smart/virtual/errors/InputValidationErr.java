package edu.ukma.smart.virtual.errors;

public class InputValidationErr implements Err {

  public final String msg;

  private InputValidationErr(String err) {
    msg = err;
  }

  public static InputValidationErr error(String err) {
    return new InputValidationErr(err);
  }
}
