package edu.ukma.smart.virtual.values;

import java.math.BigDecimal;

public record DecimalValue(
    String key,
    BigDecimal value
) implements ColumnValue {

  public static DecimalValue of(String key, BigDecimal value) {
    return new DecimalValue(key, value);
  }
}
