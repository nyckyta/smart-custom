package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.errors.InputValidationErr;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.properties.BooleanProperty;
import edu.ukma.smart.virtual.properties.DecimalProperty;
import edu.ukma.smart.virtual.properties.IntegerProperty;
import edu.ukma.smart.virtual.properties.ReferenceProperty;
import edu.ukma.smart.virtual.properties.StringProperty;
import edu.ukma.smart.virtual.values.BooleanValue;
import edu.ukma.smart.virtual.values.ColumnValue;
import edu.ukma.smart.virtual.values.DecimalValue;
import edu.ukma.smart.virtual.values.IntegerValue;
import edu.ukma.smart.virtual.values.ReferenceValue;
import edu.ukma.smart.virtual.values.StringValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PostgreQueryBuilder implements QueryBuilder {

  private final static int MAX_PRECISION = 131072;
  private final static int MAX_SCALE = 16383;
  private final static Pattern KEY_REGEXP = Pattern.compile("^[a-z][a-z_]{1,100}$");
  private static final Logger log = LoggerFactory.getLogger(PostgreQueryBuilder.class);

  private static Optional<InputValidationErr> addIntegerColumn(IntegerProperty i,
                                                               StringBuilder statementBuilder,
                                                               List<String> checks) {
    if (i.isRequired() && i.defaultValue() == null) {
      return Optional.of(InputValidationErr.error(
          "Create table: Property key '%s' is required, default value is null"
              .formatted(i.key())));
    }

    statementBuilder.append(
        ",%s BIGINT DEFAULT %s %s %s\n".formatted(
            i.key(),
            i.defaultValue() == null ? "NULL" : i.defaultValue(),
            i.isRequired() ? "NOT NULL" : "",
            i.isUnique() ? "UNIQUE" : ""
        )
    );

    if (i.max() != null && i.min() != null) {
      if (i.max() < i.min()) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' max can not be less than min".formatted(i.key()))
        );
      }
      checks.add("CHECK (%s BETWEEN %d AND %d)".formatted(i.key(), i.min(), i.max()));
      return Optional.empty();
    }

    if (i.max() != null) {
      checks.add("CHECK (%s <= %d)".formatted(i.key(), i.max()));
      return Optional.empty();
    }

    if (i.min() != null) {
      checks.add("CHECK (%s >= %d)".formatted(i.key(), i.min()));
      return Optional.empty();
    }

    return Optional.empty();
  }

  private static Optional<InputValidationErr> addStringColumn(StringProperty s,
                                                              StringBuilder statementBuilder,
                                                              List<String> checks) {
    if (s.isRequired() && s.defaultValue() == null) {
      return Optional.of(InputValidationErr.error(
          "Create table: Property key '%s' is required, default value is null"
              .formatted(s.key())));
    }
    statementBuilder.append(
        ",%s TEXT DEFAULT %s %s %s\n".formatted(
            s.key(),
            s.defaultValue() == null ? "NULL" : "$$" + s.defaultValue() + "$$",
            s.isRequired() ? "NOT NULL" : "",
            s.isUnique() ? "UNIQUE" : "")
    );

    if (s.maxLength() != null && s.minLength() != null) {
      if (s.maxLength() < 1) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' max length can not be less than one".formatted(
                    s.key()))
        );
      }

      if (s.minLength() < 1) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' min length can not be less than one".formatted(
                    s.key()))
        );
      }

      if (s.maxLength() < s.minLength()) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' max length can not be less than min length".formatted(
                    s.key()))
        );
      }
      checks.add("CHECK (char_length(%s) BETWEEN %d AND %d)".formatted(s.key(), s.minLength(),
          s.maxLength()));
      return Optional.empty();
    }

    if (s.maxLength() != null) {
      if (s.maxLength() < 1) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' max length can not be less than one".formatted(
                    s.key()))
        );
      }

      checks.add("CHECK (char_length(%s) <= %d)".formatted(s.key(), s.maxLength()));
      return Optional.empty();
    }

    if (s.minLength() != null) {
      if (s.minLength() < 1) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' min length can not be less than one".formatted(
                    s.key()))
        );
      }

      checks.add("CHECK (char_length(%s) >= %d)".formatted(s.key(), s.minLength()));
      return Optional.empty();
    }

    return Optional.empty();
  }

  private static Optional<InputValidationErr> addBooleanColumn(BooleanProperty b,
                                                               StringBuilder statementBuilder) {
    if (b.isRequired() && b.defaultValue() == null) {
      return Optional.of(InputValidationErr.error(
          "Create table: Property key '%s' is required, default value is null"
              .formatted(b.key())));
    }

    statementBuilder.append(
        ",%s BOOLEAN DEFAULT %s %s %s\n".formatted(
            b.key(),
            b.defaultValue() == null ? "NULL" : b.defaultValue(),
            b.isRequired() ? "NOT NULL" : "",
            b.isUnique() ? "UNIQUE" : ""
        )
    );

    return Optional.empty();
  }

  @Override
  public Return<String> createTable(NewTable newTable) {
    if (!KEY_REGEXP.matcher(newTable.key()).matches()) {
      log.error("Create table: Table key '{}' does not match the required pattern '{}'",
          newTable.key(), KEY_REGEXP);
      return Return.error(
          InputValidationErr.error(
              "Table key %s should consist only lower case english and '_'".formatted(
                  newTable.key()))
      );
    }

    var query = new StringBuilder()
        // TODO: figure out how to add timestamp on update
        // _updated TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        // TODO: schema check
        .append(
            """
                CREATE TABLE public.%s (
                    _id SERIAL PRIMARY KEY NOT NULL,
                    _created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
                """.formatted(newTable.key())
        );

    if (newTable.properties().isEmpty()) {
      log.error("Create table: Table '{}' has no properties defined", newTable.key());
      return Return.error(
          InputValidationErr.error(
              "Table %s should have at least one property".formatted(newTable.key()))
      );
    }

    var constraints = new ArrayList<String>();
    for (var property : newTable.properties()) {
      if (!KEY_REGEXP.matcher(property.key()).matches()) {
        log.error("Create table: Property key '{}' does not match the required pattern '{}'",
            newTable.key(), KEY_REGEXP);
        return Return.error(
            InputValidationErr.error(
                "Property key %s should consist only lower case english and '_' up to 100 chars".formatted(
                    newTable.key())
            )
        );
      }

      var err = switch (property) {
        case StringProperty s -> addStringColumn(s, query, constraints);
        case IntegerProperty i -> addIntegerColumn(i, query, constraints);
        case BooleanProperty b -> addBooleanColumn(b, query);
        case DecimalProperty d -> addDecimalColumn(d, query, constraints);
        case ReferenceProperty r -> addReferenceProperty(r, query, constraints);
        default -> throw new IllegalStateException("Unexpected value: " + property);
      };

      if (err.isPresent()) {
        return Return.error(err.get());
      }
    }

    constraints.forEach(c -> query.append(",%s".formatted(c)));
    query.append(");");

    return Return.of(query.toString());
  }

  @Override
  public Return<String> deleteTable(String tableKey) {
    if (!KEY_REGEXP.matcher(tableKey).matches()) {
      log.error("Drop: Table key '{}' does not match the required pattern '{}'", tableKey,
          KEY_REGEXP);
      return Return.error(InputValidationErr.error(
          "Table key %s should consist only lower case english and '_'".formatted(tableKey)));
    }
    return Return.of("DROP TABLE %s;".formatted(tableKey));
  }

  @Override
  public Return<String> insertIntoTable(String tableKey, List<? extends ColumnValue> columnValues) {
    if (!KEY_REGEXP.matcher(tableKey).matches()) {
      log.error("Insert: Table key '{}' does not match the required pattern '{}'", tableKey,
          KEY_REGEXP);
      return Return.error(InputValidationErr.error("Wrong table key %s".formatted(tableKey)));
    }

    if (columnValues.isEmpty()) {
      return Return.of("INSERT INTO %s DEFAULT VALUES;".formatted(tableKey));
    }

    var columnsPart = new StringBuilder("(");
    var valuesPart = new StringBuilder("VALUES (");

    for (var column : columnValues) {
      if (!KEY_REGEXP.matcher(column.key()).matches()) {
        log.error("Add row: Table key '{}' does not match the required pattern '{}'", tableKey,
            KEY_REGEXP);
        return Return.error(InputValidationErr.error("Wrong table key %s".formatted(tableKey)));
      }

      columnsPart.append(column.key()).append(",");
      switch (column) {
        case StringValue s -> valuesPart.append("$$").append(s.value()).append("$$").append(",");
        case IntegerValue i -> valuesPart.append(i.value()).append(",");
        case BooleanValue b -> valuesPart.append(b.value()).append(",");
        case DecimalValue d -> valuesPart.append(d.value()).append(",");
        case ReferenceValue r -> valuesPart.append(r.value()).append(",");
        default -> throw new IllegalStateException("Unexpected value: " + column);
      }
    }
    // Remove last comma
    columnsPart.setLength(columnsPart.length() - 1);
    valuesPart.setLength(valuesPart.length() - 1);

    columnsPart.append(")");
    valuesPart.append(")");

    return Return.of("INSERT INTO %s %s %s;".formatted(tableKey, columnsPart, valuesPart));
  }

  @Override
  public Return<String> deleteFromTable(String tableKey, int rowId) {
    if (!KEY_REGEXP.matcher(tableKey).matches()) {
      log.error("Delete row: Table key '{}' does not match the required pattern '{}'", tableKey,
          KEY_REGEXP);
      return Return.error(InputValidationErr.error("Wrong table key %s".formatted(tableKey)));
    }

    var query = "DELETE FROM %s WHERE _id = %d;".formatted(tableKey, rowId);
    return Return.of(query);
  }

  private Optional<InputValidationErr> addReferenceProperty(
      ReferenceProperty r,
      StringBuilder statementBuilder,
      List<String> constraints
  ) {
    if (!KEY_REGEXP.matcher(r.refTableKey()).matches()) {
      return Optional.of(InputValidationErr.error(
          "Create table: wrong table reference %s".formatted(r.refTableKey())));
    }

    statementBuilder.append(
        ",%s INTEGER %s %s\n".formatted(
            r.key(),
            r.isRequired() ? "NOT NULL" : "",
            r.isUnique() ? "UNIQUE" : ""
        )
    );

    constraints.add("FOREIGN KEY (%s) REFERENCES %s(_id)".formatted(r.key(), r.refTableKey()));
    return Optional.empty();
  }

  private Optional<InputValidationErr> addDecimalColumn(DecimalProperty d,
                                                        StringBuilder statementBuilder,
                                                        List<String> checks) {
    if (d.isRequired() && d.defaultValue() == null) {
      return Optional.of(InputValidationErr.error(
          "Create table: Property key '%s' is required, default value is null"
              .formatted(d.key())));
    }

    if (d.precision() < 1 || d.precision() > MAX_PRECISION) {
      return Optional.of(InputValidationErr.error(
          "Create table: property %s has invalid precision".formatted(d.key())));
    }

    if (d.scale() < 1 || d.scale() > MAX_SCALE) {
      return Optional.of(InputValidationErr.error(
          "Create table: property %s has invalid scale".formatted(d.key())));
    }

    statementBuilder.append(
        ",%s NUMERIC(%d,%d) DEFAULT %s %s %s\n".formatted(
            d.key(),
            d.precision(),
            d.scale(),
            d.defaultValue() == null ? "NULL" : d.defaultValue(),
            d.isRequired() ? "NOT NULL" : "",
            d.isUnique() ? "UNIQUE" : ""
        )
    );

    if (d.max() != null && d.min() != null) {
      if (d.max().compareTo(d.min()) < 0) {
        return Optional.of(
            InputValidationErr.error(
                "Create table: Property key '%s' max can not be less than min".formatted(d.key()))
        );
      }
      checks.add("CHECK (%s BETWEEN %f AND %f)".formatted(d.key(), d.min(), d.max()));
      return Optional.empty();
    }

    if (d.max() != null) {
      checks.add("CHECK (%s <= %f)".formatted(d.key(), d.max()));
      return Optional.empty();
    }

    if (d.min() != null) {
      checks.add("CHECK (%s >= %f)".formatted(d.key(), d.min()));
      return Optional.empty();
    }

    return Optional.empty();
  }
}
