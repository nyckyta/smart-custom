package edu.ukma.smart.virtual.ddl.constraints;

import java.math.BigDecimal;

public sealed interface PropertyConstraint extends Constraint
    permits MaxLengthConstraint, MinLengthConstraint,
            LongGreaterThanConstraint, DecimalGreaterThanConstraint, StringGreaterThanConstraint,
            LongLessThanConstraint, DecimalLessThanConstraint, StringLessThanConstraint,
            LongGreaterOrEqualConstraint, DecimalGreaterOrEqualConstraint, StringGreaterOrEqualConstraint,
            LongLessOrEqualConstraint, DecimalLessOrEqualConstraint, StringLessOrEqualConstraint,
            LongInConstraint, DecimalInConstraint, StringInConstraint,
            LongNotInConstraint, DecimalNotInConstraint, StringNotInConstraint {

    static MaxLengthConstraint maxLength(int value) {
        return new MaxLengthConstraint(value);
    }

    static MinLengthConstraint minLength(int value) {
        return new MinLengthConstraint(value);
    }

    static LongGreaterThanConstraint greaterThan(long value) {
        return new LongGreaterThanConstraint(value);
    }

    static DecimalGreaterThanConstraint greaterThan(BigDecimal value) {
        return new DecimalGreaterThanConstraint(value);
    }

    static StringGreaterThanConstraint greaterThan(String value) {
        return new StringGreaterThanConstraint(value);
    }

    static LongLessThanConstraint lessThan(long value) {
        return new LongLessThanConstraint(value);
    }

    static DecimalLessThanConstraint lessThan(BigDecimal value) {
        return new DecimalLessThanConstraint(value);
    }

    static StringLessThanConstraint lessThan(String value) {
        return new StringLessThanConstraint(value);
    }

    static LongGreaterOrEqualConstraint greaterOrEqual(long value) {
        return new LongGreaterOrEqualConstraint(value);
    }

    static DecimalGreaterOrEqualConstraint greaterOrEqual(BigDecimal value) {
        return new DecimalGreaterOrEqualConstraint(value);
    }

    static StringGreaterOrEqualConstraint greaterOrEqual(String value) {
        return new StringGreaterOrEqualConstraint(value);
    }

    static LongLessOrEqualConstraint lessOrEqual(long value) {
        return new LongLessOrEqualConstraint(value);
    }

    static DecimalLessOrEqualConstraint lessOrEqual(BigDecimal value) {
        return new DecimalLessOrEqualConstraint(value);
    }

    static StringLessOrEqualConstraint lessOrEqual(String value) {
        return new StringLessOrEqualConstraint(value);
    }

    static LongInConstraint in(Long[] values) {
        return new LongInConstraint(values);
    }

    static DecimalInConstraint in(BigDecimal[] values) {
        return new DecimalInConstraint(values);
    }

    static StringInConstraint in(String[] values) {
        return new StringInConstraint(values);
    }

    static LongNotInConstraint notIn(Long[] values) {
        return new LongNotInConstraint(values);
    }

    static DecimalNotInConstraint notIn(BigDecimal[] values) {
        return new DecimalNotInConstraint(values);
    }

    static StringNotInConstraint notIn(String[] values) {
        return new StringNotInConstraint(values);
    }
}
