package com.eventprocessing.common;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventStatus;
import com.eventprocessing.common.serialization.EventSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventSerializerTest {

    @Test
    void serializeAndDeserialize() {
        ObjectNode payload = EventSerializer.objectMapper().createObjectNode();
        payload.put("orderId", 1);
        payload.put("total", 49.99);

        Event event = new Event("order.created", "test-service", payload);

        String json = EventSerializer.serialize(event);
        assertThat(json).contains("order.created");
        assertThat(json).contains("test-service");

        Event deserialized = EventSerializer.deserialize(json);
        assertThat(deserialized.getType()).isEqualTo("order.created");
        assertThat(deserialized.getSource()).isEqualTo("test-service");
        assertThat(deserialized.getPayload().get("orderId").intValue()).isEqualTo(1);
        assertThat(deserialized.getPayload().get("total").doubleValue()).isEqualTo(49.99);
        assertThat(deserialized.getStatus()).isEqualTo(EventStatus.RECEIVED);
        assertThat(deserialized.getId()).startsWith("evt_");
    }

    @Test
    void deserializeInvalidJsonThrows() {
        assertThatThrownBy(() -> EventSerializer.deserialize("not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
