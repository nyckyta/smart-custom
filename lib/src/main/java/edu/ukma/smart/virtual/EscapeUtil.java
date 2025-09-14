package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.FatalError;
import java.sql.SQLException;
import org.postgresql.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EscapeUtil {

    private static final Logger log = LoggerFactory.getLogger(EscapeUtil.class);

    public static String escapeStringLiteral(String literal) {
        if (literal == null) {
            return null;
        }

        // the method is just wrapper around vendor specific escaping strategy
        // here, we basically do nothing except acting as proxy for postgres jdbc provided escaping
        // The below exception in our particular case should occur only when the string contain null terminator byte.
        try {
            return Utils.escapeLiteral(null, literal, true).toString();
        } catch (SQLException e) {
            log.error("Failure escaping literal", e);
            throw new FatalError("Failure escaping literal");
        }
    }

    public static String escapeStringIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        // the method is just wrapper around vendor specific escaping strategy
        // here, we basically do nothing except acting as proxy for postgres jdbc provided escaping
        // The below exception in our particular case should occur only when the string contain null terminator byte.
        try {
            return Utils.escapeIdentifier(null, identifier).toString();
        } catch (SQLException e) {
            log.error("Failure escaping literal", e);
            throw new FatalError("Failure escaping literal");
        }
    }
}
