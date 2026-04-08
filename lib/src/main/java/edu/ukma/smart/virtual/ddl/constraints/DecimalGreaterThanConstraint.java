package edu.ukma.smart.virtual.ddl.constraints;

import java.math.BigDecimal;
import java.util.Objects;

public record DecimalGreaterThanConstraint(BigDecimal value) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.GREATER_THAN_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DecimalGreaterThanConstraint that)) return false;
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
