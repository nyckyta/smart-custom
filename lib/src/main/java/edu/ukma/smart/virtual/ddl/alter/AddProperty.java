package edu.ukma.smart.virtual.ddl.alter;

import edu.ukma.smart.virtual.ddl.create.Property;

public record AddProperty(String tableKey, Property<?> property) {

    public AddProperty(String tableKey, Property<?> property) {
        this.tableKey = tableKey;
        this.property = property;
    }

    public static AddPropertyBuilder builder() {
        return new AddPropertyBuilder();
    }

    public static final class AddPropertyBuilder {
        private String tableKey;
        private Property<?> property;

        private AddPropertyBuilder() {
        }


        public AddPropertyBuilder tableKey(String tableKey) {
            this.tableKey = tableKey;
            return this;
        }

        public AddPropertyBuilder property(Property<?> property) {
            this.property = property;
            return this;
        }

        public AddProperty build() {
            return new AddProperty(tableKey, property);
        }
    }
}
