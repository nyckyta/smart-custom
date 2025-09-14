package edu.ukma.smart.virtual.dml.select;

import java.util.Objects;

public final class SelectProperty {
    private final String propertyKey;

    private SelectProperty(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public static SelectProperty of(String propertyName) {
        return new SelectProperty(propertyName);
    }

    public String propertyKey() {
        return propertyKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SelectProperty) obj;
        return Objects.equals(this.propertyKey, that.propertyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyKey);
    }

    @Override
    public String toString() {
        return "SelectProperty[propertyKey=%s]".formatted(propertyKey);
    }


}
