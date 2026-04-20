package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.ALTER_TABLE_PROPERTY_IS_NOT_SET;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_OPERATOR_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CONSTRAINT_PROPERTY_TYPE_MISMATCH;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CONSTRAINT_VALUE_TYPE_MISMATCH;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.CREATE_TABLE_EMPTY_NAME_FOR_TABLE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.LIST_VALUE_MISSING_TYPE;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.SELECT_PREDICATE_VALUE_IS_EMPTY;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.UNIQUE_CONSTRAINT_HAS_NO_PROPERTIES;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.UPDATE_ROW_NO_PROPERTIES;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_PROPERTY_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT;
import static edu.ukma.smart.virtual.errors.InputValidationErr.ErrorCode.WRONG_TABLE_KEY_FORMAT;

import edu.ukma.smart.virtual.ddl.alter.AddProperty;
import edu.ukma.smart.virtual.ddl.alter.DropProperty;
import edu.ukma.smart.virtual.ddl.constraints.Constraint;
import edu.ukma.smart.virtual.ddl.constraints.DecimalGreaterOrEqualConstraint;
import edu.ukma.smart.virtual.ddl.constraints.DecimalGreaterThanConstraint;
import edu.ukma.smart.virtual.ddl.constraints.DecimalInConstraint;
import edu.ukma.smart.virtual.ddl.constraints.DecimalLessOrEqualConstraint;
import edu.ukma.smart.virtual.ddl.constraints.DecimalLessThanConstraint;
import edu.ukma.smart.virtual.ddl.constraints.DecimalNotInConstraint;
import edu.ukma.smart.virtual.ddl.constraints.LongGreaterOrEqualConstraint;
import edu.ukma.smart.virtual.ddl.constraints.LongGreaterThanConstraint;
import edu.ukma.smart.virtual.ddl.constraints.LongInConstraint;
import edu.ukma.smart.virtual.ddl.constraints.LongLessOrEqualConstraint;
import edu.ukma.smart.virtual.ddl.constraints.LongLessThanConstraint;
import edu.ukma.smart.virtual.ddl.constraints.LongNotInConstraint;
import edu.ukma.smart.virtual.ddl.constraints.MaxLengthConstraint;
import edu.ukma.smart.virtual.ddl.constraints.MinLengthConstraint;
import edu.ukma.smart.virtual.ddl.constraints.PropertyConstraint;
import edu.ukma.smart.virtual.ddl.constraints.StringGreaterOrEqualConstraint;
import edu.ukma.smart.virtual.ddl.constraints.StringGreaterThanConstraint;
import edu.ukma.smart.virtual.ddl.constraints.StringInConstraint;
import edu.ukma.smart.virtual.ddl.constraints.StringLessOrEqualConstraint;
import edu.ukma.smart.virtual.ddl.constraints.StringLessThanConstraint;
import edu.ukma.smart.virtual.ddl.constraints.StringNotInConstraint;
import edu.ukma.smart.virtual.ddl.constraints.UniqueConstraint;
import edu.ukma.smart.virtual.ddl.create.NewTable;
import edu.ukma.smart.virtual.ddl.create.Property;
import edu.ukma.smart.virtual.ddl.create.ReferenceProperty;
import edu.ukma.smart.virtual.ddl.create.StringProperty;
import edu.ukma.smart.virtual.ddl.drop.DropTable;
import edu.ukma.smart.virtual.dml.delete.DeleteRow;
import edu.ukma.smart.virtual.dml.insert.InsertRow;
import edu.ukma.smart.virtual.dml.select.CompoundPredicate;
import edu.ukma.smart.virtual.dml.select.Predicate;
import edu.ukma.smart.virtual.dml.select.RawPredicate;
import edu.ukma.smart.virtual.dml.select.SelectProperty;
import edu.ukma.smart.virtual.dml.select.SelectQuery;
import edu.ukma.smart.virtual.dml.update.UpdateRow;
import edu.ukma.smart.virtual.dml.values.ColumnValue;
import edu.ukma.smart.virtual.dml.values.ListValue;
import edu.ukma.smart.virtual.errors.InputValidationErr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class BasicInputValidator implements InputValidator {

    private final List<Function<String, Optional<InputValidationErr>>> vendorTableKeyValidators;
    private final List<Function<NewTable, Optional<InputValidationErr>>> vendorNewTableValidators;
    private final List<Function<AddProperty, Optional<InputValidationErr>>> vendorAddPropertyValidators;
    private final List<Function<DropProperty, Optional<InputValidationErr>>> vendorDropPropertyValidators;
    private final List<Function<DeleteRow, Optional<InputValidationErr>>> vendorDeleteRowValidators;
    private final List<Function<DropTable, Optional<InputValidationErr>>> vendorDropTableValidators;
    private final List<Function<SelectQuery, Optional<InputValidationErr>>> vendorSelectQueryValidators;
    private final List<Function<InsertRow, Optional<InputValidationErr>>> vendorInsertRowValidators;
    private final List<Function<UpdateRow, Optional<InputValidationErr>>> vendorUpdateRowValidators;
    private final List<Function<Property, Optional<InputValidationErr>>> vendorPropertyValidators;
    private final List<Function<Constraint, Optional<InputValidationErr>>> vendorConstraintValidators;
    private final List<Function<ColumnValue<?>, Optional<InputValidationErr>>> vendorColumnValueValidators;
    private final List<Function<SelectProperty, Optional<InputValidationErr>>> vendorSelectPropertyValidators;
    private final List<Function<Predicate, Optional<InputValidationErr>>> vendorPredicateValidators;


    public BasicInputValidator(
        List<Function<String, Optional<InputValidationErr>>> vendorTableKeyValidators,
        List<Function<NewTable, Optional<InputValidationErr>>> vendorNewTableValidators,
        List<Function<AddProperty, Optional<InputValidationErr>>> vendorAddPropertyValidators,
        List<Function<DropProperty, Optional<InputValidationErr>>> vendorDropPropertyValidators,
        List<Function<DeleteRow, Optional<InputValidationErr>>> vendorDeleteRowValidators,
        List<Function<DropTable, Optional<InputValidationErr>>> vendorDropTableValidators,
        List<Function<SelectQuery, Optional<InputValidationErr>>> vendorSelectQueryValidators,
        List<Function<InsertRow, Optional<InputValidationErr>>> vendorInsertRowValidators,
        List<Function<UpdateRow, Optional<InputValidationErr>>> vendorUpdateRowValidators,
        List<Function<Property, Optional<InputValidationErr>>> vendorPropertyValidators,
        List<Function<Constraint, Optional<InputValidationErr>>> vendorConstraintValidators,
        List<Function<ColumnValue<?>, Optional<InputValidationErr>>> vendorColumnValueValidators,
        List<Function<SelectProperty, Optional<InputValidationErr>>> vendorSelectPropertyValidators,
        List<Function<Predicate, Optional<InputValidationErr>>> vendorPredicateValidators
    ) {
        this.vendorTableKeyValidators =
            vendorTableKeyValidators == null ? List.of() : Collections.unmodifiableList(vendorTableKeyValidators);
        this.vendorNewTableValidators = vendorNewTableValidators == null ? List.of() : Collections.unmodifiableList(
            vendorNewTableValidators);
        this.vendorAddPropertyValidators =
            vendorAddPropertyValidators == null ? List.of() : Collections.unmodifiableList(vendorAddPropertyValidators);
        this.vendorDropPropertyValidators =
            vendorDropPropertyValidators == null ? List.of() : Collections.unmodifiableList(vendorDropPropertyValidators);
        this.vendorDeleteRowValidators = vendorDeleteRowValidators == null ? List.of() : Collections.unmodifiableList(
            vendorDeleteRowValidators);
        this.vendorDropTableValidators = vendorDropTableValidators == null ? List.of() : Collections.unmodifiableList(
            vendorDropTableValidators);
        this.vendorSelectQueryValidators =
            vendorSelectQueryValidators == null ? List.of() : Collections.unmodifiableList(vendorSelectQueryValidators);
        this.vendorInsertRowValidators = vendorInsertRowValidators == null ? List.of() : Collections.unmodifiableList(
            vendorInsertRowValidators);
        this.vendorUpdateRowValidators = vendorUpdateRowValidators == null ? List.of() : Collections.unmodifiableList(
            vendorUpdateRowValidators);
        this.vendorPropertyValidators =
            vendorPropertyValidators == null ? List.of() : Collections.unmodifiableList(vendorPropertyValidators);
        this.vendorConstraintValidators =
            vendorConstraintValidators == null ? List.of() : Collections.unmodifiableList(vendorConstraintValidators);
        this.vendorColumnValueValidators =
            vendorColumnValueValidators == null ? List.of() : Collections.unmodifiableList(vendorColumnValueValidators);
        this.vendorSelectPropertyValidators =
            vendorSelectPropertyValidators == null ? List.of() : Collections.unmodifiableList(vendorSelectPropertyValidators);
        this.vendorPredicateValidators =
            vendorPredicateValidators == null ? List.of() : Collections.unmodifiableList(vendorPredicateValidators);
    }


    @Override
    public Optional<InputValidationErr> validateTableKey(String tableKey) {
        if (tableKey == null || tableKey.isBlank()) {
            return Optional.of(InputValidationErr.of(WRONG_TABLE_KEY_FORMAT));
        }

        return triggerVendorValidators(vendorTableKeyValidators, tableKey);
    }

    @Override
    public Optional<InputValidationErr> validateNewTable(NewTable newTable) {
        var err = validateTableKey(newTable.key());
        if (err.isPresent()) {
            return err;
        }

        if (newTable.name() == null || newTable.name().isBlank()) {
            return Optional.of(InputValidationErr.of(CREATE_TABLE_EMPTY_NAME_FOR_TABLE));
        }

        // map is used to do contextual validation of constraint with respect to properties
        for (var p : newTable.properties()) {
            err = validateProp(p);
            if (err.isPresent()) {
                return err;
            }
        }

        return triggerVendorValidators(vendorNewTableValidators, newTable);
    }

    private Optional<InputValidationErr> validateProp(Property p) {
        if (p.key() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (p.name() == null || p.name().isBlank()) {
            return Optional.of(InputValidationErr.of(CREATE_TABLE_EMPTY_NAME_FOR_PROPERTY));
        }


        var err = switch (p.type()) {
            case STRING, DECIMAL, INTEGER, BOOLEAN -> triggerVendorValidators(vendorPropertyValidators, p);
            case REFERENCE -> validateReferenceProperty((ReferenceProperty) p)
                .or(() -> triggerVendorValidators(vendorPropertyValidators, p));
        };

        if (err.isPresent()) {
            return err;
        }

        for (var c : p.constraints()) {
            if (!c.type().supportProperty(p)) {
                return Optional.of(InputValidationErr.of(CONSTRAINT_PROPERTY_TYPE_MISMATCH));
            }

            err = validateConstraint(p, c).or(() -> triggerVendorValidators(vendorConstraintValidators, c));
            if (err.isPresent()) {
                return err;
            }
        }

        return Optional.empty();
    }


    private static Optional<InputValidationErr> validateConstraint(Property p, Constraint columnConstraint) {
        return switch (columnConstraint.type()) {
            case UNIQUE -> validateUniqueConstraint((UniqueConstraint) columnConstraint);
            case GREATER_THAN_VALUE,
                 LESS_THAN_VALUE,
                 GREATER_OR_EQUAL_THAN_VALUE,
                 LESS_OR_EQUAL_THAN_VALUE,
                 IN,
                 NOT_IN,
                 STRING_MAX_LENGTH,
                 STRING_MIN_LENGTH -> validatePropertyConstraint(p, (PropertyConstraint) columnConstraint);
        };
    }

    private static Optional<InputValidationErr> validatePropertyConstraint(
        Property propKey,
        PropertyConstraint columnConstraint
    ) {
        boolean valid = switch (columnConstraint.type()) {
            case STRING_MAX_LENGTH -> propKey instanceof StringProperty && columnConstraint instanceof MaxLengthConstraint;
            case STRING_MIN_LENGTH -> propKey instanceof StringProperty && columnConstraint instanceof MinLengthConstraint;
            case GREATER_THAN_VALUE, LESS_THAN_VALUE, GREATER_OR_EQUAL_THAN_VALUE, LESS_OR_EQUAL_THAN_VALUE ->
                constraintTypeMatchProperty(propKey, columnConstraint);
            case IN, NOT_IN -> constraintInTypeMatchProperty(propKey, columnConstraint);
            case UNIQUE -> throw new IllegalArgumentException("Unique constraint can not be there");
        };

        if (!valid) {
            return Optional.of(InputValidationErr.of(CONSTRAINT_VALUE_TYPE_MISMATCH));
        }

        return Optional.empty();
    }

    private static boolean constraintTypeMatchProperty(Property property, PropertyConstraint constraint) {
        return switch (property.type()) {
            case STRING -> constraint instanceof StringGreaterThanConstraint
                || constraint instanceof StringLessThanConstraint
                || constraint instanceof StringGreaterOrEqualConstraint
                || constraint instanceof StringLessOrEqualConstraint;
            case DECIMAL -> constraint instanceof DecimalGreaterThanConstraint
                || constraint instanceof DecimalLessThanConstraint
                || constraint instanceof DecimalGreaterOrEqualConstraint
                || constraint instanceof DecimalLessOrEqualConstraint;
            case INTEGER, REFERENCE -> constraint instanceof LongGreaterThanConstraint
                || constraint instanceof LongLessThanConstraint
                || constraint instanceof LongGreaterOrEqualConstraint
                || constraint instanceof LongLessOrEqualConstraint;
            case BOOLEAN -> false;
        };
    }

    private static boolean constraintInTypeMatchProperty(Property property, PropertyConstraint constraint) {
        return switch (property.type()) {
            case STRING -> constraint instanceof StringInConstraint || constraint instanceof StringNotInConstraint;
            case DECIMAL -> constraint instanceof DecimalInConstraint || constraint instanceof DecimalNotInConstraint;
            case INTEGER, REFERENCE -> constraint instanceof LongInConstraint || constraint instanceof LongNotInConstraint;
            case BOOLEAN -> false;
        };
    }

    private static Optional<InputValidationErr> validateUniqueConstraint(
        UniqueConstraint columnConstraint
    ) {
        if (columnConstraint.properties().isEmpty()) {
            return Optional.of(InputValidationErr.of(UNIQUE_CONSTRAINT_HAS_NO_PROPERTIES));
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputValidationErr> validateAddProperty(AddProperty addProperty) {
        var err = validateTableKey(addProperty.tableKey());
        if (err.isPresent()) {
            return err;
        }

        if (addProperty.property() == null) {
            return Optional.of(InputValidationErr.of(ALTER_TABLE_PROPERTY_IS_NOT_SET));
        }

        return validateProp(addProperty.property()).or(() -> triggerVendorValidators(vendorAddPropertyValidators, addProperty));
    }

    @Override
    public Optional<InputValidationErr> validateDropProperty(DropProperty dropProperty) {
        var err = validateTableKey(dropProperty.tableKey());
        if (err.isPresent()) {
            return err;
        }

        if (dropProperty.columnKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        return triggerVendorValidators(vendorDropPropertyValidators, dropProperty);
    }

    @Override
    public Optional<InputValidationErr> validateDeleteRow(DeleteRow deleteRow) {
        var err = validateTableKey(deleteRow.tableKey());
        if (err.isPresent()) {
            return err;
        }

        return triggerVendorValidators(vendorDeleteRowValidators, deleteRow);
    }

    @Override
    public Optional<InputValidationErr> validateDeleteTable(DropTable deleteTable) {
        var err = validateTableKey(deleteTable.tableKey());
        if (err.isPresent()) {
            return err;
        }

        return triggerVendorValidators(vendorDropTableValidators, deleteTable);
    }

    @Override
    public Optional<InputValidationErr> validateSelectQuery(SelectQuery selectQuery) {
        var err = validateTableKey(selectQuery.tableKey());
        if (err.isPresent()) {
            return err;
        }

        if (selectQuery.predicate() != null) {
            err = validatePredicate(selectQuery.predicate()).or(
                () -> triggerVendorValidators(vendorPredicateValidators, selectQuery.predicate()));

            if (err.isPresent()) {
                return err;
            }
        }

        for (var prop : selectQuery.propertyKeysToReturn()) {
            err = validateSelectProperty(prop).or(() -> triggerVendorValidators(vendorSelectPropertyValidators, prop));
            if (err.isPresent()) {
                return err;
            }
        }

        return triggerVendorValidators(vendorSelectQueryValidators, selectQuery);
    }

    @Override
    public Optional<InputValidationErr> validateInsertRow(InsertRow insertRow) {
        var err = validateTableKey(insertRow.tableKey());
        if (err.isPresent()) {
            return err;
        }

        return triggerVendorValidators(vendorInsertRowValidators, insertRow);
    }

    @Override
    public Optional<InputValidationErr> validateUpdateRow(UpdateRow updateRow) {
        var err = validateTableKey(updateRow.tableKey());
        if (err.isPresent()) {
            return err;
        }

        if (updateRow.valuesToUpdate() == null || updateRow.valuesToUpdate().isEmpty()) {
            return Optional.of(new InputValidationErr(UPDATE_ROW_NO_PROPERTIES));
        }

        for (var prop : updateRow.valuesToUpdate()) {
            err = validateColumnValue(prop).or(() -> triggerVendorValidators(vendorColumnValueValidators, prop));
            if (err.isPresent()) {
                return err;
            }
        }
        return triggerVendorValidators(vendorUpdateRowValidators, updateRow);
    }

    public Optional<InputValidationErr> validateColumnValue(ColumnValue<?> columnValue) {
        if (columnValue.key() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        if (columnValue instanceof ListValue<?> l) {
            if (l.elementsType() == null) {
                return Optional.of(InputValidationErr.of(LIST_VALUE_MISSING_TYPE));
            }
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> validateSelectProperty(SelectProperty p) {
        if (p.propertyKey() == null) {
            return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
        }

        return Optional.empty();
    }

    private static Optional<InputValidationErr> validatePredicate(Predicate prop) {
        switch (prop.type()) {
            case REFERENCE, INTEGER, STRING, DECIMAL, NULL, BOOLEAN -> {
                final var p = (RawPredicate<?>) prop;
                if (p.propertyKey() == null) {
                    return Optional.of(InputValidationErr.of(WRONG_PROPERTY_KEY_FORMAT));
                }

                if (p.value() == null) {
                    return Optional.of(InputValidationErr.of(SELECT_PREDICATE_VALUE_IS_EMPTY));
                }
            }
            case COMPOUND -> {
                final var p = (CompoundPredicate) prop;
                if (p.left() == null) {
                    return Optional.of(InputValidationErr.of(COMPOUND_PREDICATE_LEFT_PART_IS_EMPTY));
                }

                if (p.right() == null) {
                    return Optional.of(InputValidationErr.of(COMPOUND_PREDICATE_RIGHT_PART_IS_EMPTY));
                }

                if (p.op() == null) {
                    return Optional.of(InputValidationErr.of(COMPOUND_PREDICATE_OPERATOR_IS_EMPTY));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<InputValidationErr> validateReferenceProperty(ReferenceProperty rp) {
        var err = validateTableKey(rp.refTableKey());
        if (err.isPresent()) {
            return Optional.of(InputValidationErr.of(WRONG_REFERENCE_PROPERTY_TABLE_KEY_FORMAT));
        }

        return Optional.empty();
    }

    private static <T> Optional<InputValidationErr> triggerVendorValidators(
        List<Function<T, Optional<InputValidationErr>>> validators, T entity) {
        return validators
            .stream()
            .map(v -> v.apply(entity))
            .filter(Optional::isPresent)
            .findAny()
            .orElseGet(Optional::empty);
    }

    public static final class BasicInputValidatorBuilder {
        private List<Function<String, Optional<InputValidationErr>>> tableKeyValidators = new ArrayList<>();
        private List<Function<NewTable, Optional<InputValidationErr>>> newTableValidators = new ArrayList<>();
        private List<Function<AddProperty, Optional<InputValidationErr>>> addPropertyValidators = new ArrayList<>();
        private List<Function<DropProperty, Optional<InputValidationErr>>> dropPropertyValidators = new ArrayList<>();
        private List<Function<DeleteRow, Optional<InputValidationErr>>> deleteRowValidators = new ArrayList<>();
        private List<Function<DropTable, Optional<InputValidationErr>>> dropTableValidators = new ArrayList<>();
        private List<Function<SelectQuery, Optional<InputValidationErr>>> selectQueryValidators = new ArrayList<>();
        private List<Function<InsertRow, Optional<InputValidationErr>>> insertRowValidators = new ArrayList<>();
        private List<Function<UpdateRow, Optional<InputValidationErr>>> updateRowValidators = new ArrayList<>();
        private List<Function<Property, Optional<InputValidationErr>>> vendorPropertyValidators = new ArrayList<>();
        private List<Function<Constraint, Optional<InputValidationErr>>> vendorConstraintValidators = new ArrayList<>();
        private List<Function<ColumnValue<?>, Optional<InputValidationErr>>> vendorColumnValueValidators = new ArrayList<>();
        private List<Function<SelectProperty, Optional<InputValidationErr>>> vendorSelectPropertyValidators = new ArrayList<>();
        private List<Function<Predicate, Optional<InputValidationErr>>> vendorPredicateValidators = new ArrayList<>();

        private BasicInputValidatorBuilder() {
        }

        public static BasicInputValidatorBuilder builder() {
            return new BasicInputValidatorBuilder();
        }

        public BasicInputValidatorBuilder vendorTableKeyValidators(
            List<Function<NewTable, Optional<InputValidationErr>>> newTableValidators) {
            this.newTableValidators = newTableValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorTableKeyValidator(
            Function<String, Optional<InputValidationErr>> vendorTableKeyValidator
        ) {
            this.tableKeyValidators.add(vendorTableKeyValidator);
            return this;
        }

        public BasicInputValidatorBuilder addVendorTableValidator(
            Function<NewTable, Optional<InputValidationErr>> newTableValidator
        ) {
            this.newTableValidators.add(newTableValidator);
            return this;
        }

        public BasicInputValidatorBuilder vendorNewTableValidators(
            List<Function<NewTable, Optional<InputValidationErr>>> newTableValidators) {
            this.newTableValidators = newTableValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorNewTableValidator(Function<NewTable, Optional<InputValidationErr>> validator) {
            this.newTableValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorAddPropertyValidators(
            List<Function<AddProperty, Optional<InputValidationErr>>> addPropertyValidators) {
            this.addPropertyValidators = addPropertyValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorAddPropertyValidator(
            Function<AddProperty, Optional<InputValidationErr>> validator) {
            this.addPropertyValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorDropPropertyValidators(
            List<Function<DropProperty, Optional<InputValidationErr>>> dropPropertyValidators) {
            this.dropPropertyValidators = dropPropertyValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorDropPropertyValidator(
            Function<DropProperty, Optional<InputValidationErr>> validator
        ) {
            this.dropPropertyValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorDeleteRowValidators(
            List<Function<DeleteRow, Optional<InputValidationErr>>> deleteRowValidators) {
            this.deleteRowValidators = deleteRowValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorDeleteRowValidator(
            Function<DeleteRow, Optional<InputValidationErr>> validator
        ) {
            this.deleteRowValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorDropTableValidators(
            List<Function<DropTable, Optional<InputValidationErr>>> dropTableValidators) {
            this.dropTableValidators = dropTableValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorDropTableValidator(
            Function<DropTable, Optional<InputValidationErr>> validator
        ) {
            this.dropTableValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorSelectQueryValidators(
            List<Function<SelectQuery, Optional<InputValidationErr>>> selectQueryValidators) {
            this.selectQueryValidators = selectQueryValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorSelectQueryValidator(
            Function<SelectQuery, Optional<InputValidationErr>> validator
        ) {
            this.selectQueryValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorInsertRowValidators(
            List<Function<InsertRow, Optional<InputValidationErr>>> insertRowValidators) {
            this.insertRowValidators = insertRowValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorInsertRowValidator(
            Function<InsertRow, Optional<InputValidationErr>> validator
        ) {
            this.insertRowValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorUpdateRowValidators(
            List<Function<UpdateRow, Optional<InputValidationErr>>> updateRowValidators) {
            this.updateRowValidators = updateRowValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorUpdateRowValidator(
            Function<UpdateRow, Optional<InputValidationErr>> validator
        ) {
            this.updateRowValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorPropertyValidators(
            List<Function<Property, Optional<InputValidationErr>>> vendorPropertyValidators) {
            this.vendorPropertyValidators = vendorPropertyValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorPropertyValidator(
            Function<Property, Optional<InputValidationErr>> validator
        ) {
            this.vendorPropertyValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorConstraintValidators(
            List<Function<Constraint, Optional<InputValidationErr>>> vendorConstraintValidators) {
            this.vendorConstraintValidators = vendorConstraintValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorConstraintValidator(
            Function<Constraint, Optional<InputValidationErr>> validator
        ) {
            this.vendorConstraintValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorColumnValueValidators(
            List<Function<ColumnValue<?>, Optional<InputValidationErr>>> vendorColumnValueValidators) {
            this.vendorColumnValueValidators = vendorColumnValueValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorColumnValueValidator(
            Function<ColumnValue<?>, Optional<InputValidationErr>> validator
        ) {
            this.vendorColumnValueValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorSelectPropertyValidators(
            List<Function<SelectProperty, Optional<InputValidationErr>>> vendorSelectPropertyValidators) {
            this.vendorSelectPropertyValidators = vendorSelectPropertyValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorSelectPropertyValidator(
            Function<SelectProperty, Optional<InputValidationErr>> validator
        ) {
            this.vendorSelectPropertyValidators.add(validator);
            return this;
        }

        public BasicInputValidatorBuilder vendorPredicateValidators(
            List<Function<Predicate, Optional<InputValidationErr>>> vendorPredicateValidators) {
            this.vendorPredicateValidators = vendorPredicateValidators;
            return this;
        }

        public BasicInputValidatorBuilder addVendorPredicateValidator(
            Function<Predicate, Optional<InputValidationErr>> validator
        ) {
            this.vendorPredicateValidators.add(validator);
            return this;
        }

        public BasicInputValidator build() {
            return new BasicInputValidator(
                tableKeyValidators,
                newTableValidators,
                addPropertyValidators,
                dropPropertyValidators,
                deleteRowValidators,
                dropTableValidators,
                selectQueryValidators,
                insertRowValidators,
                updateRowValidators,
                vendorPropertyValidators,
                vendorConstraintValidators,
                vendorColumnValueValidators,
                vendorSelectPropertyValidators,
                vendorPredicateValidators
            );
        }
    }
}
