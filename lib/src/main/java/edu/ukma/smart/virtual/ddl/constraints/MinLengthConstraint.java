package edu.ukma.smart.virtual.ddl.constraints;

public record MinLengthConstraint(int value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.STRING_MIN_LENGTH;
    }
}
