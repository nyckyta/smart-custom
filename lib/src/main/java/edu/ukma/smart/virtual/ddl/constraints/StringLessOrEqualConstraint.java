package edu.ukma.smart.virtual.ddl.constraints;

public record StringLessOrEqualConstraint(String value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.LESS_OR_EQUAL_THAN_VALUE;
    }
}
