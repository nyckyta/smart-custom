package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.OperationError.ErrorCode.COLUMN_DOES_NOT_EXIST;
import static edu.ukma.smart.virtual.errors.OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED;
import static edu.ukma.smart.virtual.errors.OperationError.ErrorCode.TABLE_DOES_NOT_EXIST;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.FatalError;
import edu.ukma.smart.virtual.errors.OperationError;
import edu.ukma.smart.virtual.errors.SQLExceptionsHandler;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreSQLExceptionHandler implements SQLExceptionsHandler {

    private static Logger logger = LoggerFactory.getLogger(PostgreSQLExceptionHandler.class);

    // TODO: Make more sophisticated error check, not just all 23* errors
    private static final String CHECK_VIOLATION = "23";
    private static final String UNDEFINED_TABLE = "42P01";
    private static final String NUMERIC_VALUE_OUT_OF_RANGE = "22003";
    private static final String INVALID_COLUMN = "42703";

    @Override
    public Err handle(SQLException e) {
        logger.debug(e.getMessage(), e);
        if (e.getSQLState().startsWith(CHECK_VIOLATION)) {
            return OperationError.of(OperationError.ErrorCode.PROPERTY_CHECK_VIOLATED);
        }
        return switch (e.getSQLState()) {
            case UNDEFINED_TABLE -> OperationError.of(TABLE_DOES_NOT_EXIST);
            case NUMERIC_VALUE_OUT_OF_RANGE -> OperationError.of(PROPERTY_CHECK_VIOLATED);
            case INVALID_COLUMN -> OperationError.of(COLUMN_DOES_NOT_EXIST);
            default -> throw new FatalError(e);
        };
    }
}
