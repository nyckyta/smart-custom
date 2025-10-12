package edu.ukma.smart.virtual.ddl.alter;

import edu.ukma.smart.virtual.ddl.create.Property;
import java.util.Objects;

public final class AddProperty {
    private final String tableKey;
    private final Property property;

    private AddProperty(String tableKey, Property property) {
        this.tableKey = tableKey;
        this.property = property;
    }

    public static AddPropertyBuilder builder() {
        return new AddPropertyBuilder();
    }

    public String tableKey() {
        return tableKey;
    }

    public Property property() {
        return property;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (AddProperty) obj;
        return Objects.equals(this.tableKey, that.tableKey)
               && Objects.equals(this.property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableKey, property);
    }

    @Override
    public String toString() {
        return "AddProperty[tableKey=%s, property=%s]".formatted(tableKey, property);
    }


    public static final class AddPropertyBuilder {
        private String tableKey;
        private Property property;

        private AddPropertyBuilder() {
        }


        public AddPropertyBuilder tableKey(String tableKey) {
            this.tableKey = tableKey;
            return this;
        }

        public AddPropertyBuilder property(Property property) {
            this.property = property;
            return this;
        }

        public AddProperty build() {
            return new AddProperty(tableKey, property);
        }
    }
}
