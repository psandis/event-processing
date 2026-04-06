package com.eventprocessing.engine.executor;

import com.eventprocessing.common.converter.TypeConverter;
import com.eventprocessing.common.mapping.ConversionType;
import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class MappingExecutor {

    private static final Logger log = LoggerFactory.getLogger(MappingExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JsonNode execute(JsonNode input, PipelineDefinition pipeline) {
        ObjectNode output = MAPPER.createObjectNode();

        for (FieldMapping mapping : pipeline.getFieldMappings()) {
            if (mapping.isExcluded()) {
                continue;
            }

            JsonNode sourceValue = resolveField(input, mapping.getSourceField());

            if (sourceValue == null || sourceValue.isNull()) {
                if (mapping.getDefaultValue() != null) {
                    sourceValue = resolveDynamicValue(mapping.getDefaultValue());
                } else {
                    continue;
                }
            }

            if (mapping.getConversion() != null && mapping.getConversion() != ConversionType.NONE) {
                if (mapping.getConversion() == ConversionType.FLATTEN && sourceValue.isObject()) {
                    flattenInto(output, sourceValue, mapping.getDestinationField());
                    continue;
                }
                sourceValue = TypeConverter.convert(sourceValue, mapping.getConversion());
            }

            setField(output, mapping.getDestinationField(), sourceValue);
        }

        return output;
    }

    private JsonNode resolveField(JsonNode node, String path) {
        if (path == null || path.isEmpty()) return null;

        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null || !current.isObject()) return null;
            current = current.get(part);
        }

        return current;
    }

    private void setField(ObjectNode target, String path, JsonNode value) {
        if (path == null || path.isEmpty()) return;

        String[] parts = path.split("\\.");

        ObjectNode current = target;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode child = current.get(parts[i]);
            if (child == null || !child.isObject()) {
                ObjectNode newNode = MAPPER.createObjectNode();
                current.set(parts[i], newNode);
                current = newNode;
            } else {
                current = (ObjectNode) child;
            }
        }

        current.set(parts[parts.length - 1], value);
    }

    private void flattenInto(ObjectNode target, JsonNode source, String prefix) {
        Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (prefix != null && !prefix.isEmpty()) {
                setField(target, prefix + "." + field.getKey(), field.getValue());
            } else {
                target.set(field.getKey(), field.getValue());
            }
        }
    }

    private JsonNode resolveDynamicValue(String value) {
        return switch (value) {
            case "${now}" -> MAPPER.getNodeFactory().textNode(
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            case "${uuid}" -> MAPPER.getNodeFactory().textNode(UUID.randomUUID().toString());
            default -> MAPPER.getNodeFactory().textNode(value);
        };
    }
}
