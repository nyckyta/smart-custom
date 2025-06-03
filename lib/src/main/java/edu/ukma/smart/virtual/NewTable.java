package edu.ukma.smart.virtual;

import java.util.List;

// TODO: strict validation
public record NewTable(
    String key,
    String name,
    String description,
    List<Property> properties
) {
}
