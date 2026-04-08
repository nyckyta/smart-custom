package edu.ukma.smart.virtual.ddl.constraints;

public record LongLessThanConstraint(long value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.LESS_THAN_VALUE;
    }
}
