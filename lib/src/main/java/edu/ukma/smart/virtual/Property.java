package edu.ukma.smart.virtual;

// TODO: strict validation
public record Property(
    String name,
    String description,
    Type type,
    String defaultValue,
    boolean isRequired,
    boolean isUnique
) {

    public enum Type {
        STRING("VARCHAR");

        public final String sqlType;

        Type(String sqlType) {
            this.sqlType = sqlType;
        }
    }
}
