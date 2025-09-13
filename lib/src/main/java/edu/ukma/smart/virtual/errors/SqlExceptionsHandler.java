package edu.ukma.smart.virtual.errors;

import java.sql.SQLException;

public interface SqlExceptionsHandler {

    Err handle(SQLException e);
}
