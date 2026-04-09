package com.eventprocessing.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructConversionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void structToFlatJson() throws Exception {
        Struct struct = Struct.newBuilder()
                .putFields("orderId", Value.newBuilder().setNumberValue(42).build())
                .putFields("total", Value.newBuilder().setStringValue("99.99").build())
                .putFields("active", Value.newBuilder().setBoolValue(true).build())
                .build();

        String json = JsonFormat.printer().print(struct);
        JsonNode node = mapper.readTree(json);

        assertThat(node.get("orderId").numberValue()).isEqualTo(42.0);
        assertThat(node.get("total").textValue()).isEqualTo("99.99");
        assertThat(node.get("active").booleanValue()).isTrue();
    }
}
