package com.eventprocessing.ingest.service;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.ingest.config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Event submit(EventRequest request) {
        Event event = new Event(request.type(), request.source(), request.payload());
        if (request.metadata() != null) {
            event.setMetadata(request.metadata());
        }

        String json = EventSerializer.serialize(event);
        kafkaTemplate.send(KafkaTopics.EVENTS_RAW, event.getId(), json);

        log.info("Event submitted: id={}, type={}, source={}", event.getId(), event.getType(), event.getSource());
        return event;
    }
}
