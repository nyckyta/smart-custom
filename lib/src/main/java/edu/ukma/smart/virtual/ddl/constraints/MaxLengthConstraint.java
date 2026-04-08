package edu.ukma.smart.virtual.ddl.constraints;

public record MaxLengthConstraint(int value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.STRING_MAX_LENGTH;
    }
}
