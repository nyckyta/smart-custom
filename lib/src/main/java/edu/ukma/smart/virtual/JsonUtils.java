package edu.ukma.smart.virtual;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import edu.ukma.smart.virtual.errors.FatalError;
import java.io.IOException;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static final JsonFactory jsonFactory = new JsonFactory();

    public static String generateComment(String name, String description) {
        final var sw = new StringWriter();
        try (var gen = jsonFactory.createGenerator(sw)) {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(name);
            gen.writeFieldName("description");
            gen.writeString(description);
            gen.writeEndObject();
        } catch (IOException e) {
            log.error("Failed to build comment JSON for the table", e);
            throw new FatalError("Failed to generate comment");
        }

        return sw.toString();
    }

    public static Pair<String, String> parseComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        String name = null;
        String description = null;
        try (var p = jsonFactory.createParser(comment)) {
            while (p.nextToken() != JsonToken.END_OBJECT) {
                if (p.currentToken() == JsonToken.FIELD_NAME) {
                    if (p.getText().equals("name") && p.nextToken() == JsonToken.VALUE_STRING) {
                        name = p.getValueAsString();
                        continue;
                    }

                    if (p.getText().equals("description") && p.nextToken() == JsonToken.VALUE_STRING) {
                        description = p.getValueAsString();
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Pair<>(name, description);
    }
}
