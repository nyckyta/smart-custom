package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_PRECISION_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_SCALE_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.FORBIDDEN_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_ROW_ID_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.create.DecimalProperty;
import edu.ukma.smart.virtual.create.NewTable;
import edu.ukma.smart.virtual.create.Property;
import edu.ukma.smart.virtual.create.ReferenceProperty;
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
import java.util.Set;
import java.util.regex.Pattern;

public class PostgreInputValidator implements InputValidator {

    // pg should not be the first two letter of the table, since it can be confused with postgre system tables.
    //
    private static final Pattern TABLE_KEY_REGEXP = Pattern.compile("^_[a-z][a-z_]{1,62}$");
    private static final Pattern PROPERTY_KEY_REGEXP = Pattern.compile("^[a-z][a-z_]{1,62}$");
    private static final Set<String> STATIC_FIELDS = Set.of("_id", "_created");
    private static final Set<String> SYSTEM_EXCLUDED_FIELDS = Set.of(
        "tableoid",
        "xmin",
        "cmin",
        "xmax",
        "cmax",
        "ctid"
    );

    // max allowed scale and precisions for postgres numeric type
    private static final int MAX_PRECISION = 131072;
    private static final int MAX_SCALE = 16383;

    @Override
    public Optional<InputValidationErr> validateNewTable(NewTable newTable) {
        var err = InputValidator.super.validateNewTable(newTable);
        if (err.isPresent()) {
            return err;
        }

        if (!TABLE_KEY_REGEXP.matcher(newTable.key()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateDeleteRow(DeleteRow deleteRow) {
        var err = InputValidator.super.validateDeleteRow(deleteRow);
        if (err.isPresent()) {
            return err;
        }

        if (!TABLE_KEY_REGEXP.matcher(deleteRow.tableKey()).matches()) {
            return Optional.of(InputValidationErr.of(InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT));
        }

        if (deleteRow.rowId() < 1) {
            return Optional.of(InputValidationErr.of(WRONG_ROW_ID_FORMAT));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateDeleteTable(DeleteTable deleteTable) {
        var err = InputValidator.super.validateDeleteTable(deleteTable);
        if (err.isPresent()) {
            return err;
        }

        if (!TABLE_KEY_REGEXP.matcher(deleteTable.tableKey()).matches()) {
            return Optional.of(InputValidationErr.of(InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateSelectQuery(SelectQuery selectQuery) {
        var err = InputValidator.super.validateSelectQuery(selectQuery);
        if (err.isPresent()) {
            return err;
        }

        if (!TABLE_KEY_REGEXP.matcher(selectQuery.tableKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateInsertRow(InsertRow insertRow) {
        var err = InputValidator.super.validateInsertRow(insertRow);
        if (err.isPresent()) {
            return err;
        }

        if (!TABLE_KEY_REGEXP.matcher(insertRow.tableKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateUpdateRow(UpdateRow updateRow) {
        var err = InputValidator.super.validateUpdateRow(updateRow);
        if (err.isPresent()) {
            return err;
        }

        if (!TABLE_KEY_REGEXP.matcher(updateRow.tableKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        if (updateRow.rowId() < 1) {
            return Optional.of(InputValidationErr.of(WRONG_ROW_ID_FORMAT));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateColumnValue(ColumnValue<?> columnValue) {
        var err = InputValidator.super.validateColumnValue(columnValue);
        if (err.isPresent()) {
            return err;
        }

        if (!PROPERTY_KEY_REGEXP.matcher(columnValue.key()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(columnValue.key())) {
            return Optional.of(InputValidationErr.of(FORBIDDEN_PROPERTY_KEY));
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<InputValidationErr> validateProperty(Property<T> property) {
        var err = InputValidator.super.validateProperty(property);
        if (err.isPresent()) {
            return err;
        }

        if (!PROPERTY_KEY_REGEXP.matcher(property.key()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (STATIC_FIELDS.contains(property.key()) || SYSTEM_EXCLUDED_FIELDS.contains(property.key())) {
            return Optional.of(InputValidationErr.of(FORBIDDEN_PROPERTY_KEY));
        }

        err = switch (property) {
            case DecimalProperty d -> validateDecimal(d);
            case ReferenceProperty r -> validateReference(r);
            default -> Optional.empty();
        };

        return err;
    }

    @Override
    public Optional<InputValidationErr> validateSelectProperty(SelectProperty p) {
        var err = InputValidator.super.validateSelectProperty(p);
        if (err.isPresent()) {
            return err;
        }

        if (!STATIC_FIELDS.contains(p.propertyKey()) && !PROPERTY_KEY_REGEXP.matcher(p.propertyKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(p.propertyKey())) {
            return Optional.of(InputValidationErr.of(FORBIDDEN_PROPERTY_KEY));
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<InputValidationErr> validatePredicate(RawPredicate<T> p) {
        var err = InputValidator.super.validatePredicate(p);
        if (err.isPresent()) {
            return err;
        }

        if (!PROPERTY_KEY_REGEXP.matcher(p.propertyKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(p.propertyKey())) {
            return Optional.of(
                InputValidationErr.of(FORBIDDEN_PROPERTY_KEY)
            );
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateCompoundPredicate(CompoundPredicate p) {
        return InputValidator.super.validateCompoundPredicate(p);
    }

    private Optional<InputValidationErr> validateDecimal(DecimalProperty decimalProperty) {
        if (decimalProperty instanceof DecimalProperty d) {
            if (d.precision() < 1 || d.precision() > MAX_PRECISION) {
                return Optional.of(InputValidationErr.of(DECIMAL_PRECISION_IS_INVALID));
            }

            if (d.scale() < 1 || d.scale() > MAX_SCALE) {
                return Optional.of(InputValidationErr.of(DECIMAL_SCALE_IS_INVALID));
            }
        }

        return Optional.empty();
    }

    private Optional<InputValidationErr> validateReference(ReferenceProperty r) {
        if (!TABLE_KEY_REGEXP.matcher(r.refTableKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
