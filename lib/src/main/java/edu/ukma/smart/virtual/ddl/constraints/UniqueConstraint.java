package edu.ukma.smart.virtual.ddl.constraints;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UniqueConstraint implements Constraint {

    public final List<String> properties;

    private UniqueConstraint(List<String> properties) {
        this.properties = Collections.unmodifiableList(properties);
    }

    public static UniqueConstraint of(List<String> properties) {
        return new UniqueConstraint(properties);
    }

    public static UniqueConstraint of(String... properties) {
        return new UniqueConstraint(List.of(properties));
    }

    @Override
    public Type type() {
        return Type.UNIQUE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UniqueConstraint that)) {
            return false;
        }
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }
}
