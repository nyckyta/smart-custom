package edu.ukma.smart.virtual.ddl.constraints;

import edu.ukma.smart.virtual.ddl.create.Property;
import java.util.Set;

public interface Constraint {

    Type type();

    enum Type {
        STRING_MAX_LENGTH(Set.of(Property.Type.STRING)),
        STRING_MIN_LENGTH(Set.of(Property.Type.STRING)),
        LESS_THAN_VALUE(Set.of(Property.Type.INTEGER, Property.Type.DECIMAL, Property.Type.REFERENCE, Property.Type.STRING)),
        GREATER_THAN_VALUE(Set.of(Property.Type.INTEGER, Property.Type.DECIMAL, Property.Type.REFERENCE, Property.Type.STRING)),
        LESS_OR_EQUAL_THAN_VALUE(Set.of(Property.Type.INTEGER, Property.Type.DECIMAL, Property.Type.REFERENCE, Property.Type.STRING)),
        GREATER_OR_EQUAL_THAN_VALUE(Set.of(Property.Type.INTEGER, Property.Type.DECIMAL, Property.Type.REFERENCE, Property.Type.STRING)),
        UNIQUE(Set.of(Property.Type.STRING, Property.Type.INTEGER, Property.Type.REFERENCE, Property.Type.BOOLEAN,
            Property.Type.DECIMAL)),
        IN(Set.of(Property.Type.STRING, Property.Type.INTEGER, Property.Type.REFERENCE,
            Property.Type.DECIMAL)),
        NOT_IN(Set.of(Property.Type.STRING, Property.Type.INTEGER, Property.Type.REFERENCE,
            Property.Type.DECIMAL));

        private final Set<Property.Type> supportedProperties;

        Type(Set<Property.Type> supportedProperties) {
            this.supportedProperties = supportedProperties;
        }

        public boolean supportProperty(Property prop) {
            return supportedProperties.contains(prop.type());
        }
    }
}
