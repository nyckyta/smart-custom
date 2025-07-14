package edu.ukma.smart.virtual.select;

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
            return Optional.of(InputValidationErr.error("Left predicate must be specified"));
        }

        if (right == null) {
            return Optional.of(InputValidationErr.error("Right predicate must be specified"));
        }

        if (op == null) {
            return Optional.of(InputValidationErr.error("Operator must be specified"));
        }

        return Optional.empty();
    }

    public enum Operator {
        AND,
        OR
    }
}
