package com.eventprocessing.store.service;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.store.entity.StoredEvent;
import com.eventprocessing.store.repository.StoredEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final StoredEventRepository repository;
    private final ObjectMapper objectMapper;

    public EventConsumer(StoredEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topicPattern = ".*",
            groupId = "event-store",
            properties = {
                    "auto.offset.reset=earliest",
                    "max.poll.records=100"
            }
    )
    public void consume(String message) {
        try {
            Event event = EventSerializer.deserialize(message);

            if (repository.existsById(event.getId())) {
                log.debug("Event already stored, skipping: {}", event.getId());
                return;
            }

            StoredEvent stored = new StoredEvent();
            stored.setId(event.getId());
            stored.setType(event.getType());
            stored.setSource(event.getSource());
            stored.setPayload(objectMapper.writeValueAsString(event.getPayload()));
            if (event.getMetadata() != null) {
                stored.setMetadata(objectMapper.writeValueAsString(event.getMetadata()));
            }
            stored.setStatus(event.getStatus() != null ? event.getStatus().name() : "UNKNOWN");
            stored.setErrorMessage(event.getErrorMessage());
            stored.setReceivedAt(event.getTimestamp());
            stored.setProcessedAt(event.getProcessedAt());

            repository.save(stored);
            log.debug("Event stored: {} (type={}, source={})", event.getId(), event.getType(), event.getSource());
        } catch (Exception e) {
            log.error("Failed to store event: {}", e.getMessage());
        }
    }
}
