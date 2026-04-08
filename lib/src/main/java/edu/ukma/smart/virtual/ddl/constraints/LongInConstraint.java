package edu.ukma.smart.virtual.ddl.constraints;

import java.util.Arrays;

public record LongInConstraint(Long[] values) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.IN;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LongInConstraint that)) return false;
        return Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "LongInConstraint[values=" + Arrays.toString(values) + "]";
    }
}
