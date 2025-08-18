package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;

/**
 * Provides basic entities validation.
 */
public interface InputValidator {

    Optional<InputValidationErr> validateTableKey(String tableKey);

    Optional<InputValidationErr> validateNewTable(NewTable newTable);

    Optional<InputValidationErr> validateAddProperty(AddProperty addProperty);

    Optional<InputValidationErr> validateDropProperty(DropProperty dropProperty);

    Optional<InputValidationErr> validateDeleteRow(DeleteRow deleteRow);

    Optional<InputValidationErr> validateDeleteTable(DropTable deleteTable);

    Optional<InputValidationErr> validateSelectQuery(SelectQuery selectQuery);

    Optional<InputValidationErr> validateInsertRow(InsertRow insertRow);

    Optional<InputValidationErr> validateUpdateRow(UpdateRow updateRow);

//
//
//    static Optional<InputValidationErr> validateStringConstraint(StringConstraint checkConstraint) {
//        if (
//            checkConstraint.propertyName() == null
//            || checkConstraint.propertyName().isBlank()
//            || checkConstraint.op() == null
//            || checkConstraint.value() == null
//        ) {
//            return Optional.of(InputValidationErr.of(CONSTRAINT_IS_INVALID));
//        }
//
//        return Optional.empty();
//    }
//
//    static Optional<InputValidationErr> validateDecimalProperty(DecimalProperty dp) {
//        boolean maxSet = dp.max() != null;
//        boolean minSet = dp.min() != null;
//        boolean defaultSet = dp.defaultValue() != null;
//
//        if (defaultSet) {
//            if (maxSet && dp.defaultValue().compareTo(dp.max()) > 0) {
//                return Optional.of(
//                    InputValidationErr.of(DECIMAL_DEFAULT_GREATER_MAX_VAL)
//                );
//            }
//
//            if (minSet && dp.defaultValue().compareTo(dp.min()) < 0) {
//                return Optional.of(InputValidationErr.of(DECIMAL_DEFAULT_LESS_MIN_VAL));
//            }
//        }
//
//        if (minSet && maxSet) {
//            if (dp.max().compareTo(dp.min()) < 0) {
//                return Optional.of(InputValidationErr.of(DECIMAL_MAX_VAL_LESS_MIN_VAL));
//            }
//        }
//
//        return Optional.empty();
//    }
//
//
//    Optional<InputValidationErr> validateReferenceProperty(ReferenceProperty rp) {
//        var err = validateTableKey(rp.refTableKey());
//        if (err.isPresent()) {
//            return Optional.of(InputValidationErr.of(WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT));
//        }
//
//        return Optional.empty();
//    }
}
