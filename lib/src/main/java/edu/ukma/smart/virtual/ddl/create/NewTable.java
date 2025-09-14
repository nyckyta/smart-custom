package edu.ukma.smart.virtual.ddl.create;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class NewTable {
    private final String key;
    private final String name;
    private final String description;
    private final List<Property> properties;

    public NewTable(String key, String name, String description, List<Property> properties) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.properties = properties == null ? List.of() : Collections.unmodifiableList(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NewTable newTable)) {
            return false;
        }
        return Objects.equals(key, newTable.key) && Objects.equals(name, newTable.name)
               && Objects.equals(description, newTable.description)
               && Objects.equals(properties, newTable.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, description, properties);
    }

    public static NewTableBuilder builder() {
        return new NewTableBuilder();
    }

    public String key() {
        return key;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<Property> properties() {
        return properties;
    }

    @Override
    public String toString() {
        return "NewTable[key=%s, name=%s, description=%s, properties=%s]".formatted(key, name, description, properties);
    }

    public static final class NewTableBuilder {
        private String key;
        private String name;
        private String description;
        private List<Property> properties;

        private NewTableBuilder() {
        }

        public NewTableBuilder key(String key) {
            this.key = key;
            return this;
        }

        public NewTableBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NewTableBuilder description(String description) {
            this.description = description;
            return this;
        }

        public NewTableBuilder properties(List<Property> properties) {
            this.properties = properties;
            return this;
        }

        public NewTable build() {
            return new NewTable(
                key,
                name,
                description,
                properties
            );
        }
    }
}
