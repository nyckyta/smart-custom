package edu.ukma.smart.virtual.ddl.constraints;

public record LongGreaterThanConstraint(long value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.GREATER_THAN_VALUE;
    }
}
