package edu.ukma.smart.virtual.select;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.EMPTY_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.SELECT_PREDICATE_VALUE_IS_EMPTY;

import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

public interface RawPredicate<V> extends Predicate {

    String propertyKey();

    V value();

    @Override
    default Optional<InputValidationErr> validate() {
        if (propertyKey() == null) {
            return Optional.of(InputValidationErr.of(EMPTY_PROPERTY_KEY));
        }

        if (value() == null) {
            return Optional.of(InputValidationErr.of(SELECT_PREDICATE_VALUE_IS_EMPTY));
        }

        return Optional.empty();
    }
}
