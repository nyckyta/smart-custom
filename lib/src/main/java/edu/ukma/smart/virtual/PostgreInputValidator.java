package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_PRECISION_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.DECIMAL_SCALE_IS_INVALID;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.FORBIDDEN_PROPERTY_KEY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_ROW_ID_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.constraints.UniqueConstraint;
import edu.ukma.smart.virtual.ddl.create.DecimalProperty;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.Property;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.RawPredicate;
import edu.ukma.smart.virtual.dml.select.SelectProperty;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class PostgreInputValidator implements InputValidator {

    private final BasicInputValidator basicValidator;
    // pg should not be the first two letter of the table, since it can be confused with postgre system tables.
    private static final Pattern TABLE_KEY_REGEXP = Pattern.compile("^_[a-z][a-z_]{1,62}$");
    private static final Pattern PROPERTY_KEY_REGEXP = Pattern.compile("^[a-z][a-z_]{1,62}$");
    public static final Set<String> STATIC_FIELDS = Set.of("_id", "_created");
    public static final Set<String> SYSTEM_EXCLUDED_FIELDS = Set.of(
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

    public PostgreInputValidator() {
        basicValidator = BasicInputValidator.BasicInputValidatorBuilder
            .builder()
            .addVendorTableKeyValidator(PostgreInputValidator::validateTableKeyFormat)
            .addVendorPropertyValidator(prop -> validatePropertyKey(prop.key()))
            .addVendorPropertyValidator(prop -> {
                if (prop instanceof ReferenceProperty rp) {
                    return validateReference(rp);
                }

                return Optional.empty();
            })
            .addVendorPropertyValidator(PostgreInputValidator::validateDecimalPrecisionAndScale)
            .addVendorConstraintValidator(cons -> {
                if (cons instanceof UniqueConstraint pc) {
                    return pc.properties()
                        .stream()
                        .map(PostgreInputValidator::validatePropertyKey)
                        .filter(Optional::isPresent)
                        .findAny()
                        .orElseGet(Optional::empty);
                }

                return Optional.empty();
            })
            .addVendorDeleteRowValidator(dr -> validateRowId(dr.rowId()))
            .addVendorColumnValueValidator(cv -> validatePropertyKey(cv.key()))
            .addVendorPredicateValidator(p -> {
                if (p instanceof RawPredicate<?> rp) {
                    return validatePropertyKey(rp.propertyKey());
                }
                return Optional.empty();
            })
            .addVendorSelectPropertyValidator(PostgreInputValidator::validateSelectField)
            .build();
    }

    private static Optional<InputValidationErr> validateDecimalPrecisionAndScale(Property v) {
        if (v instanceof DecimalProperty dp) {
            if (dp.precision() > MAX_PRECISION || dp.precision() < 1) {
                return Optional.of(InputValidationErr.of(DECIMAL_PRECISION_IS_INVALID));
            }

            if (dp.scale() > MAX_SCALE || dp.scale() < 1) {
                return Optional.of(InputValidationErr.of(DECIMAL_SCALE_IS_INVALID));
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateTableKey(String tableKey) {
        return basicValidator.validateTableKey(tableKey);
    }

    @Override
    public Optional<InputValidationErr> validateNewTable(NewTable newTable) {
        return basicValidator.validateNewTable(newTable);

    }

    @Override
    public Optional<InputValidationErr> validateAddProperty(AddProperty addProperty) {
        return basicValidator.validateAddProperty(addProperty);
    }


    @Override
    public Optional<InputValidationErr> validateDropProperty(DropProperty dropProperty) {
        return basicValidator.validateDropProperty(dropProperty);
    }

    @Override
    public Optional<InputValidationErr> validateDeleteRow(DeleteRow deleteRow) {
        return basicValidator.validateDeleteRow(deleteRow);
    }

    @Override
    public Optional<InputValidationErr> validateDeleteTable(DropTable deleteTable) {
        return basicValidator.validateDeleteTable(deleteTable);
    }

    @Override
    public Optional<InputValidationErr> validateSelectQuery(SelectQuery selectQuery) {
        return basicValidator.validateSelectQuery(selectQuery);
    }

    @Override
    public Optional<InputValidationErr> validateInsertRow(InsertRow insertRow) {
        return basicValidator.validateInsertRow(insertRow);
    }

    @Override
    public Optional<InputValidationErr> validateUpdateRow(UpdateRow updateRow) {
        return basicValidator.validateUpdateRow(updateRow);
    }


    private static Optional<InputValidationErr> validateSelectField(SelectProperty p) {
        if (!STATIC_FIELDS.contains(p.propertyKey()) && !PROPERTY_KEY_REGEXP.matcher(p.propertyKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (SYSTEM_EXCLUDED_FIELDS.contains(p.propertyKey())) {
            return Optional.of(InputValidationErr.of(FORBIDDEN_PROPERTY_KEY));
        }
        return Optional.empty();
    }

    private static Optional<InputValidationErr> validateRowId(int rowId) {
        if (rowId < 1) {
            return Optional.of(InputValidationErr.of(WRONG_ROW_ID_FORMAT));
        }
        return Optional.empty();
    }

    private Optional<InputValidationErr> validateReference(ReferenceProperty r) {
        if (!TABLE_KEY_REGEXP.matcher(r.refTableKey()).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> validateTableKeyFormat(String tableKey) {
        if (!TABLE_KEY_REGEXP.matcher(tableKey).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }


    private static Optional<InputValidationErr> validatePropertyKey(String propertyKey) {
        if (SYSTEM_EXCLUDED_FIELDS.contains(propertyKey)) {
            return Optional.of(InputValidationErr.of(FORBIDDEN_PROPERTY_KEY));
        }

        if (!PROPERTY_KEY_REGEXP.matcher(propertyKey).matches()) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        return Optional.empty();
    }
}
