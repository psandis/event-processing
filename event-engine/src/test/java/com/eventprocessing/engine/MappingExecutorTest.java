package com.eventprocessing.engine;

import com.eventprocessing.common.mapping.ConversionType;
import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.engine.executor.MappingExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MappingExecutorTest {

    private final MappingExecutor executor = new MappingExecutor();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void directMapping() throws Exception {
        JsonNode input = mapper.readTree("""
                { "userId": 123, "name": "John" }
                """);

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("userId", "customerId"),
                mapping("name", "customerName")
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("customerId").intValue()).isEqualTo(123);
        assertThat(result.get("customerName").textValue()).isEqualTo("John");
    }

    @Test
    void typeConversion() throws Exception {
        JsonNode input = mapper.readTree("""
                { "total": "49.99", "count": 3 }
                """);

        FieldMapping totalMapping = mapping("total", "amount");
        totalMapping.setConversion(ConversionType.TO_DOUBLE);
        FieldMapping countMapping = mapping("count", "quantity");
        countMapping.setConversion(ConversionType.TO_STRING);

        PipelineDefinition pipeline = pipeline(List.of(totalMapping, countMapping));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("amount").doubleValue()).isEqualTo(49.99);
        assertThat(result.get("quantity").textValue()).isEqualTo("3");
    }

    @Test
    void nestedSourceField() throws Exception {
        JsonNode input = mapper.readTree("""
                { "address": { "city": "Helsinki", "country": "FI" } }
                """);

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("address.city", "city"),
                mapping("address.country", "countryCode")
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("city").textValue()).isEqualTo("Helsinki");
        assertThat(result.get("countryCode").textValue()).isEqualTo("FI");
    }

    @Test
    void nestedDestinationField() throws Exception {
        JsonNode input = mapper.readTree("""
                { "city": "Helsinki", "country": "FI" }
                """);

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("city", "location.city"),
                mapping("country", "location.country")
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("location").get("city").textValue()).isEqualTo("Helsinki");
        assertThat(result.get("location").get("country").textValue()).isEqualTo("FI");
    }

    @Test
    void excludedFieldsSkipped() throws Exception {
        JsonNode input = mapper.readTree("""
                { "name": "John", "password": "secret" }
                """);

        FieldMapping excluded = mapping("password", "password");
        excluded.setExcluded(true);

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("name", "name"),
                excluded
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.has("name")).isTrue();
        assertThat(result.has("password")).isFalse();
    }

    @Test
    void defaultValueUsedWhenSourceMissing() throws Exception {
        JsonNode input = mapper.readTree("""
                { "name": "John" }
                """);

        FieldMapping withDefault = mapping("currency", "currency");
        withDefault.setDefaultValue("EUR");

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("name", "name"),
                withDefault
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("name").textValue()).isEqualTo("John");
        assertThat(result.get("currency").textValue()).isEqualTo("EUR");
    }

    @Test
    void dynamicValueNow() throws Exception {
        JsonNode input = mapper.readTree("""
                { "name": "John" }
                """);

        FieldMapping dynamic = mapping("processedAt", "processedAt");
        dynamic.setDefaultValue("${now}");

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("name", "name"),
                dynamic
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("processedAt").textValue()).isNotBlank();
    }

    @Test
    void toUpperConversion() throws Exception {
        JsonNode input = mapper.readTree("""
                { "country": "finland" }
                """);

        FieldMapping upper = mapping("country", "countryCode");
        upper.setConversion(ConversionType.TO_UPPER);

        PipelineDefinition pipeline = pipeline(List.of(upper));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("countryCode").textValue()).isEqualTo("FINLAND");
    }

    @Test
    void maskConversion() throws Exception {
        JsonNode input = mapper.readTree("""
                { "email": "john@example.com" }
                """);

        FieldMapping masked = mapping("email", "email");
        masked.setConversion(ConversionType.MASK);

        PipelineDefinition pipeline = pipeline(List.of(masked));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("email").textValue()).startsWith("j");
        assertThat(result.get("email").textValue()).endsWith("m");
        assertThat(result.get("email").textValue()).contains("*");
    }

    @Test
    void missingSourceFieldSkipped() throws Exception {
        JsonNode input = mapper.readTree("""
                { "name": "John" }
                """);

        PipelineDefinition pipeline = pipeline(List.of(
                mapping("name", "name"),
                mapping("nonexistent", "other")
        ));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.has("name")).isTrue();
        assertThat(result.has("other")).isFalse();
    }

    @Test
    void flattenObjectIntoParent() throws Exception {
        JsonNode input = mapper.readTree("""
                { "address": { "city": "Helsinki", "zip": "00100" } }
                """);

        FieldMapping flatten = mapping("address", "");
        flatten.setConversion(ConversionType.FLATTEN);

        PipelineDefinition pipeline = pipeline(List.of(flatten));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("city").textValue()).isEqualTo("Helsinki");
        assertThat(result.get("zip").textValue()).isEqualTo("00100");
    }

    @Test
    void flattenObjectIntoNestedDestination() throws Exception {
        JsonNode input = mapper.readTree("""
                { "address": { "city": "Helsinki", "country": "FI" } }
                """);

        FieldMapping flatten = mapping("address", "shipping");
        flatten.setConversion(ConversionType.FLATTEN);

        PipelineDefinition pipeline = pipeline(List.of(flatten));

        JsonNode result = executor.execute(input, pipeline);

        assertThat(result.get("shipping").get("city").textValue()).isEqualTo("Helsinki");
        assertThat(result.get("shipping").get("country").textValue()).isEqualTo("FI");
    }

    private FieldMapping mapping(String source, String destination) {
        return new FieldMapping(source, destination);
    }

    private PipelineDefinition pipeline(List<FieldMapping> mappings) {
        PipelineDefinition def = new PipelineDefinition();
        def.setName("test-pipeline");
        def.setSourceTopic("test.source");
        def.setDestinationTopic("test.destination");
        def.setFieldMappings(mappings);
        return def;
    }
}
