package com.eventprocessing.engine;

import com.eventprocessing.engine.discovery.SchemaDiscovery;
import com.eventprocessing.engine.discovery.SchemaDiscovery.FieldInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaDiscoveryTest {

    private final SchemaDiscovery discovery = new SchemaDiscovery();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void discoverFlatFields() throws Exception {
        JsonNode event = mapper.readTree("""
                {
                    "name": "test",
                    "count": 42,
                    "active": true,
                    "price": 19.99
                }
                """);

        Map<String, FieldInfo> fields = discovery.discover(event);

        assertThat(fields).containsKey("name");
        assertThat(fields.get("name").type()).isEqualTo("string");
        assertThat(fields.get("count").type()).isEqualTo("integer");
        assertThat(fields.get("active").type()).isEqualTo("boolean");
        assertThat(fields.get("price").type()).isEqualTo("double");
    }

    @Test
    void discoverNestedFields() throws Exception {
        JsonNode event = mapper.readTree("""
                {
                    "user": {
                        "name": "John",
                        "address": {
                            "city": "Helsinki"
                        }
                    }
                }
                """);

        Map<String, FieldInfo> fields = discovery.discover(event);

        assertThat(fields).containsKey("user");
        assertThat(fields.get("user").type()).isEqualTo("object");
        assertThat(fields).containsKey("user.name");
        assertThat(fields.get("user.name").type()).isEqualTo("string");
        assertThat(fields).containsKey("user.address.city");
        assertThat(fields.get("user.address.city").type()).isEqualTo("string");
    }

    @Test
    void discoverArrayFields() throws Exception {
        JsonNode event = mapper.readTree("""
                {
                    "items": [
                        { "sku": "ABC", "qty": 2 }
                    ]
                }
                """);

        Map<String, FieldInfo> fields = discovery.discover(event);

        assertThat(fields).containsKey("items");
        assertThat(fields.get("items").type()).isEqualTo("array");
        assertThat(fields).containsKey("items[*].sku");
        assertThat(fields.get("items[*].sku").type()).isEqualTo("string");
    }

    @Test
    void discoverFromNull() {
        Map<String, FieldInfo> fields = discovery.discover(null);
        assertThat(fields).isEmpty();
    }
}
