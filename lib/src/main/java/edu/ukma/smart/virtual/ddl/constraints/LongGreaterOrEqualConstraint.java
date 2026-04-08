package edu.ukma.smart.virtual.ddl.constraints;

public record LongGreaterOrEqualConstraint(long value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.GREATER_OR_EQUAL_THAN_VALUE;
    }
}
