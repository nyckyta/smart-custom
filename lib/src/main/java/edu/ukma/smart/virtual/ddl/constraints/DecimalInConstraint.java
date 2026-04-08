package edu.ukma.smart.virtual.ddl.constraints;

import java.math.BigDecimal;
import java.util.Arrays;

public record DecimalInConstraint(BigDecimal[] values) implements PropertyConstraint {
    @Override
    public Constraint.Type type() {
        return Constraint.Type.IN;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DecimalInConstraint that)) return false;
        if (values.length != that.values.length) return false;
        for (int i = 0; i < values.length; i++) {
            if (values[i].compareTo(that.values[i]) != 0) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (BigDecimal bd : values) {
            result = 31 * result + bd.stripTrailingZeros().hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        return "DecimalInConstraint[values=" + Arrays.toString(values) + "]";
    }
}
