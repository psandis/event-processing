package com.eventprocessing.common;

import com.eventprocessing.common.converter.TypeConverter;
import com.eventprocessing.common.mapping.ConversionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeConverterTest {

    @Test
    void stringToInteger() {
        JsonNode result = TypeConverter.convert(new TextNode("123"), ConversionType.TO_INTEGER);
        assertThat(result.intValue()).isEqualTo(123);
    }

    @Test
    void stringToDouble() {
        JsonNode result = TypeConverter.convert(new TextNode("49.99"), ConversionType.TO_DOUBLE);
        assertThat(result.doubleValue()).isEqualTo(49.99);
    }

    @Test
    void stringToLong() {
        JsonNode result = TypeConverter.convert(new TextNode("9999999999"), ConversionType.TO_LONG);
        assertThat(result.longValue()).isEqualTo(9999999999L);
    }

    @Test
    void stringToBoolean() {
        JsonNode result = TypeConverter.convert(new TextNode("true"), ConversionType.TO_BOOLEAN);
        assertThat(result.booleanValue()).isTrue();
    }

    @Test
    void integerToString() {
        JsonNode result = TypeConverter.convert(new IntNode(42), ConversionType.TO_STRING);
        assertThat(result.textValue()).isEqualTo("42");
    }

    @Test
    void doubleToInteger() {
        JsonNode result = TypeConverter.convert(new DoubleNode(49.99), ConversionType.TO_INTEGER);
        assertThat(result.intValue()).isEqualTo(49);
    }

    @Test
    void toUpper() {
        JsonNode result = TypeConverter.convert(new TextNode("hello"), ConversionType.TO_UPPER);
        assertThat(result.textValue()).isEqualTo("HELLO");
    }

    @Test
    void toLower() {
        JsonNode result = TypeConverter.convert(new TextNode("HELLO"), ConversionType.TO_LOWER);
        assertThat(result.textValue()).isEqualTo("hello");
    }

    @Test
    void mask() {
        JsonNode result = TypeConverter.convert(new TextNode("secret123"), ConversionType.MASK);
        assertThat(result.textValue()).isEqualTo("s*******3");
    }

    @Test
    void maskShortValue() {
        JsonNode result = TypeConverter.convert(new TextNode("ab"), ConversionType.MASK);
        assertThat(result.textValue()).isEqualTo("**");
    }

    @Test
    void noneReturnsOriginal() {
        TextNode input = new TextNode("unchanged");
        JsonNode result = TypeConverter.convert(input, ConversionType.NONE);
        assertThat(result).isSameAs(input);
    }

    @Test
    void nullReturnsNull() {
        JsonNode result = TypeConverter.convert(null, ConversionType.TO_STRING);
        assertThat(result).isNull();
    }
}
