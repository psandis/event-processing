package com.eventprocessing.common.serialization;

import com.eventprocessing.common.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class EventSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private EventSerializer() {
    }

    public static String serialize(Event event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event: " + event.getId(), e);
        }
    }

    public static Event deserialize(String json) {
        try {
            return MAPPER.readValue(json, Event.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize event", e);
        }
    }

    public static ObjectMapper objectMapper() {
        return MAPPER;
    }
}
