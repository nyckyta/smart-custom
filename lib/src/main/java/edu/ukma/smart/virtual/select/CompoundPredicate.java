package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_OPERATOR_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY;

import edu.ukma.smart.virtual.Validated;
import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public record CompoundPredicate(
    Predicate left, Predicate right, Operator op
) implements Predicate, Validated {

    public CompoundPredicate(Predicate left, Predicate right, Operator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public static CompoundPredicate and(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.AND);
    }

    public static CompoundPredicate or(Predicate left, Predicate right) {
        return new CompoundPredicate(left, right, Operator.OR);
    }

    @Override
    public Optional<Err> validate() {
        if (left == null) {
            return Optional.of(InputValidationErr.error(COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY));
        }

        if (right == null) {
            return Optional.of(InputValidationErr.error(COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY));
        }

        if (op == null) {
            return Optional.of(InputValidationErr.error(COMPOUND_PREDICATE_OPERATOR_IS_EMPTY));
        }

        return Optional.empty();
    }

    public enum Operator {
        AND,
        OR
    }
}
