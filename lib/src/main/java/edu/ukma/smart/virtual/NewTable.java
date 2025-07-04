package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.properties.Property;
import java.util.List;
import java.util.Objects;

// TODO: strict validation
public record NewTable(
    String key,
    String name,
    String description,
    List<Property<?>> properties
) {

  public static NewTableBuilder builder() {
    return new NewTableBuilder();
  }

  public static final class NewTableBuilder {
    private String key;
    private String name;
    private String description;
    private List<Property<?>> properties;

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

    public NewTableBuilder properties(List<Property<?>> properties) {
      this.properties = properties;
      return this;
    }

    public NewTable build() {
      return new NewTable(
          Objects.requireNonNull(key),
          Objects.requireNonNull(name),
          Objects.requireNonNull(description),
          Objects.requireNonNull(properties)
      );
    }
  }
}
