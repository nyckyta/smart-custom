package edu.ukma.smart.virtual.properties;

public record BooleanProperty(
    String key,
    String name,
    String description,
    Boolean defaultValue,
    boolean isRequired,
    boolean isUnique
) implements Property<Boolean> {

  public static BooleanPropertyBuilder builder() {
    return new BooleanPropertyBuilder();
  }

  public static final class BooleanPropertyBuilder {
    private String key;
    private String name;
    private String description;
    private Boolean defaultValue;
    private boolean isRequired;
    private boolean isUnique;

    private BooleanPropertyBuilder() {
    }

    public static BooleanPropertyBuilder aBooleanProperty() {
      return new BooleanPropertyBuilder();
    }

    public BooleanPropertyBuilder key(String key) {
      this.key = key;
      return this;
    }

    public BooleanPropertyBuilder name(String name) {
      this.name = name;
      return this;
    }

    public BooleanPropertyBuilder description(String description) {
      this.description = description;
      return this;
    }

    public BooleanPropertyBuilder defaultValue(Boolean defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public BooleanPropertyBuilder isRequired(boolean isRequired) {
      this.isRequired = isRequired;
      return this;
    }

    public BooleanPropertyBuilder isUnique(boolean isUnique) {
      this.isUnique = isUnique;
      return this;
    }

    public BooleanProperty build() {
      return new BooleanProperty(key, name, description, defaultValue, isRequired, isUnique);
    }
  }
}
