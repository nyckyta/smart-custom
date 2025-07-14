package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.Err;
import java.util.Optional;

/**
 * This interface is indent for types that have context independent validation logic.
 * Context independent basically means that the state of the type does not depend on the context it is called i.e.
 * if {@link Validated#validate()} returns error, then no matter what method process the data, it is in invalid state
 * and continuing of processing must not happen.
 *
 * <p>Context independence can be set differently. If the type contains all context about the way it is used, then it may be
 * reliably validated outside any service.
 *
 * <p>The main purpose of this interface is to clean repetitive validation logic from other classes (potentially reducing bugs
 * from missing checks) and entity multiplying by introducing "Validator" services for each type. It seems intuitively clear
 * and reasonable to keep state validation near the data that it validates.
 *
 * <p>Implementation should make shallow validation (analog to shallow copying). It is better to avoid iterating through
 * collections. Doing 'deep validation' may lead to unnecessary two times iteration when collection actually used. Instead,
 * all collection/complex type iteration should be done on site.
 */
public interface Validated {

    Optional<Err> validate();
}
