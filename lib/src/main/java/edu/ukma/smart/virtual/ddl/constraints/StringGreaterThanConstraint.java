package edu.ukma.smart.virtual.ddl.constraints;

public record StringGreaterThanConstraint(String value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.GREATER_THAN_VALUE;
    }
}
