package edu.ukma.smart.virtual.errors;

import java.sql.SQLException;

public interface SQLExceptionsHandler {

    Err handle(SQLException e);
}
