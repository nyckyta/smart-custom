package edu.ukma.smart.virtual.errors;

import java.sql.SQLException;

public class FatalError extends RuntimeException {

    public FatalError(SQLException ex) {
        super(ex);
    }

    public FatalError(String message) {
        super(message);
    }
}
