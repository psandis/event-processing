package com.eventprocessing.common.converter;

import com.eventprocessing.common.mapping.ConversionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class TypeConverter {

    private TypeConverter() {
    }

    public static JsonNode convert(JsonNode value, ConversionType type) {
        if (value == null || value.isNull()) {
            return value;
        }

        return switch (type) {
            case NONE -> value;
            case TO_STRING -> new TextNode(value.asText());
            case TO_INTEGER -> new IntNode(parseInteger(value));
            case TO_LONG -> new LongNode(parseLong(value));
            case TO_DOUBLE -> new DoubleNode(parseDouble(value));
            case TO_BOOLEAN -> BooleanNode.valueOf(parseBoolean(value));
            case TO_TIMESTAMP -> new TextNode(parseTimestamp(value));
            case TO_UPPER -> new TextNode(value.asText().toUpperCase());
            case TO_LOWER -> new TextNode(value.asText().toLowerCase());
            case MASK -> new TextNode(mask(value.asText()));
            case FLATTEN -> value;
        };
    }

    private static int parseInteger(JsonNode value) {
        if (value.isNumber()) return value.intValue();
        return Integer.parseInt(value.asText().trim());
    }

    private static long parseLong(JsonNode value) {
        if (value.isNumber()) return value.longValue();
        return Long.parseLong(value.asText().trim());
    }

    private static double parseDouble(JsonNode value) {
        if (value.isNumber()) return value.doubleValue();
        return Double.parseDouble(value.asText().trim());
    }

    private static boolean parseBoolean(JsonNode value) {
        if (value.isBoolean()) return value.booleanValue();
        return Boolean.parseBoolean(value.asText().trim());
    }

    private static String parseTimestamp(JsonNode value) {
        String text = value.asText().trim();
        try {
            OffsetDateTime.parse(text);
            return text;
        } catch (Exception e) {
            return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    private static String mask(String value) {
        if (value.length() <= 2) return "**";
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }
}
