package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.create.NewTable;
import edu.ukma.smart.virtual.create.Property;
import edu.ukma.smart.virtual.delete.DeleteRow;
import edu.ukma.smart.virtual.delete.DeleteTable;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.insert.InsertRow;
import edu.ukma.smart.virtual.select.CompoundPredicate;
import edu.ukma.smart.virtual.select.RawPredicate;
import edu.ukma.smart.virtual.select.SelectProperty;
import edu.ukma.smart.virtual.select.SelectQuery;
import edu.ukma.smart.virtual.update.UpdateRow;
import edu.ukma.smart.virtual.values.ColumnValue;
import java.util.Optional;

/**
 * Provides vendor specific validation. Context independent validation happens inside {@link Validated} implementations for each
 * data type. This interface is supposed to be used with context dependent validation i.e. validation that is specific to
 * each vendor. Good example of such case can be check that table key does not intersect with system tables specific to vendor
 *
 * <p>Implementation of this interface should do shallow checks. It can call {@link Validated#validate()} for an object that
 * implements mentioned interface and directly passed to the method, but it SHOULD NOT call validation on subsequent objects e.g.
 * when validating {@link NewTable}, implementation should not call {@link this#validateProperty(Property)}
 * on {@link NewTable#properties()}.
 */
public interface InputValidator {

    Optional<InputValidationErr> validateNewTable(NewTable newTable);

    Optional<InputValidationErr> validateDeleteRow(DeleteRow deleteRow);

    Optional<InputValidationErr> validateDeleteTable(DeleteTable deleteTable);

    Optional<InputValidationErr> validateSelectQuery(SelectQuery selectQuery);

    Optional<InputValidationErr> validateInsertRow(InsertRow insertRow);

    Optional<InputValidationErr> validateUpdateRow(UpdateRow updateRow);

    Optional<InputValidationErr> validateColumnValue(ColumnValue<?> columnValue);

    <T> Optional<InputValidationErr> validateProperty(Property<T> p);

    <T> Optional<InputValidationErr> validateSelectProperty(SelectProperty p);

    <T> Optional<InputValidationErr> validatePredicate(RawPredicate<T> p);

    default <T> Optional<InputValidationErr> validateCompoundPredicate(CompoundPredicate p) {
        return p.validate();
    }
}
