package edu.ukma.smart.virtual.ddl.constraints;

import java.util.Arrays;

public record LongNotInConstraint(Long[] values) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.NOT_IN;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LongNotInConstraint that)) return false;
        return Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "LongNotInConstraint[values=" + Arrays.toString(values) + "]";
    }
}
