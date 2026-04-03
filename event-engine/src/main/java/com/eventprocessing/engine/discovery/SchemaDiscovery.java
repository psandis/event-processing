package com.eventprocessing.engine.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaDiscovery {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, FieldInfo> discover(JsonNode event) {
        Map<String, FieldInfo> fields = new LinkedHashMap<>();
        if (event != null && event.isObject()) {
            extractFields(event, "", fields);
        }
        return fields;
    }

    public Map<String, FieldInfo> discoverFromMultiple(Iterable<JsonNode> events) {
        Map<String, FieldInfo> merged = new LinkedHashMap<>();
        for (JsonNode event : events) {
            Map<String, FieldInfo> fields = discover(event);
            for (Map.Entry<String, FieldInfo> entry : fields.entrySet()) {
                merged.merge(entry.getKey(), entry.getValue(),
                        (existing, incoming) -> new FieldInfo(existing.path(), existing.type(), existing.occurrences() + 1));
            }
        }
        return merged;
    }

    private void extractFields(JsonNode node, String prefix, Map<String, FieldInfo> fields) {
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String fieldPath = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonNode value = entry.getValue();

            fields.put(fieldPath, new FieldInfo(fieldPath, inferType(value), 1));

            if (value.isObject()) {
                extractFields(value, fieldPath, fields);
            } else if (value.isArray() && !value.isEmpty() && value.get(0).isObject()) {
                extractFields(value.get(0), fieldPath + "[*]", fields);
            }
        }
    }

    private String inferType(JsonNode value) {
        if (value.isTextual()) return "string";
        if (value.isInt()) return "integer";
        if (value.isLong()) return "long";
        if (value.isDouble() || value.isFloat()) return "double";
        if (value.isBoolean()) return "boolean";
        if (value.isArray()) return "array";
        if (value.isObject()) return "object";
        if (value.isNull()) return "null";
        return "unknown";
    }

    public record FieldInfo(String path, String type, int occurrences) {
    }
}
