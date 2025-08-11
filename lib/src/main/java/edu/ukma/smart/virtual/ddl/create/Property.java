package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.List;

public interface Property {

    String key();

    String name();

    String description();

    boolean notNull();

    List<Constraint> constraints();

    Type type();

    enum Type {
        STRING,
        INTEGER,
        DECIMAL,
        REFERENCE,
        BOOLEAN
    }
}
