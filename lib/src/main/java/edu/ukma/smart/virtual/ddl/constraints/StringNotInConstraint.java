package edu.ukma.smart.virtual.ddl.constraints;

import java.util.Arrays;

public record StringNotInConstraint(String[] values) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.NOT_IN;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StringNotInConstraint that)) return false;
        return Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "StringNotInConstraint[values=" + Arrays.toString(values) + "]";
    }
}
