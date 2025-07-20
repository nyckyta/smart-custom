package edu.ukma.smart.virtual.dml.values;

import java.math.BigDecimal;

public record DecimalValue(
    String key,
    BigDecimal value,
    Type type
) implements ColumnValue<BigDecimal> {

    public DecimalValue(String key, BigDecimal value, Type type) {
        this.key = key;
        this.value = value;
        this.type = Type.DECIMAL;
    }

    public static DecimalValue of(String key, BigDecimal value) {
        return new DecimalValue(key, value, Type.DECIMAL);
    }
}
