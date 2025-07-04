package edu.ukma.smart.virtual.values;

public record ReferenceValue(String key, Integer value)
    implements ColumnValue {

  public static ReferenceValue of(String key, Integer value) {
    return new ReferenceValue(key, value);
  }
}
