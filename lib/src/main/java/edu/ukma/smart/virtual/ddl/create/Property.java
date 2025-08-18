package edu.ukma.smart.virtual.ddl.create;

import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import java.util.Set;

public interface Property {

    String key();

    String name();

    String description();

    boolean notNull();

    Set<Constraint> constraints();

    Type type();

    interface Builder<T extends Property> {
        Builder<T> key(String key);

        Builder<T> name(String name);

        Builder<T> description(String description);

        Builder<T> notNull(boolean notNull);

        Builder<T> constraints(Set<Constraint> constraints);

        Builder<T> addConstraint(Constraint constraint);

        T build();

    }

    enum Type {
        STRING,
        INTEGER,
        DECIMAL,
        REFERENCE,
        BOOLEAN
    }
}
