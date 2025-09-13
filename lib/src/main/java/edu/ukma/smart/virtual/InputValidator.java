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
}
