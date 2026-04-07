package com.eventprocessing.detect;

import com.eventprocessing.common.discovery.SchemaDiscovery;
import com.eventprocessing.common.discovery.SchemaDiscovery.FieldInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaDriftDetectorTest {

    private final SchemaDiscovery discovery = new SchemaDiscovery();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void detectsNewField() throws Exception {
        JsonNode event1 = mapper.readTree("""
                {"orderId": 1, "total": 49.99}
                """);
        JsonNode event2 = mapper.readTree("""
                {"orderId": 2, "total": 29.99, "discount": 5.00}
                """);

        Map<String, FieldInfo> schema1 = discovery.discover(event1);
        Map<String, FieldInfo> schema2 = discovery.discover(event2);

        Set<String> fields1 = schema1.keySet();
        Set<String> fields2 = schema2.keySet();

        Set<String> added = fields2.stream()
                .filter(f -> !fields1.contains(f))
                .collect(java.util.stream.Collectors.toSet());

        assertThat(added).containsExactly("discount");
    }

    @Test
    void detectsRemovedField() throws Exception {
        JsonNode event1 = mapper.readTree("""
                {"orderId": 1, "total": 49.99, "currency": "EUR"}
                """);
        JsonNode event2 = mapper.readTree("""
                {"orderId": 2, "total": 29.99}
                """);

        Set<String> fields1 = discovery.discover(event1).keySet();
        Set<String> fields2 = discovery.discover(event2).keySet();

        Set<String> removed = fields1.stream()
                .filter(f -> !fields2.contains(f))
                .collect(java.util.stream.Collectors.toSet());

        assertThat(removed).containsExactly("currency");
    }

    @Test
    void noChangeDetectedForIdenticalSchemas() throws Exception {
        JsonNode event1 = mapper.readTree("""
                {"orderId": 1, "total": 49.99}
                """);
        JsonNode event2 = mapper.readTree("""
                {"orderId": 2, "total": 29.99}
                """);

        Set<String> fields1 = discovery.discover(event1).keySet();
        Set<String> fields2 = discovery.discover(event2).keySet();

        assertThat(fields1).isEqualTo(fields2);
    }
}
