package com.eventprocessing.ingest.service;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.ingest.config.IngestProperties;
import com.eventprocessing.ingest.config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final IngestProperties ingestProperties;

    public EventProducer(KafkaTemplate<String, String> kafkaTemplate, IngestProperties ingestProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.ingestProperties = ingestProperties;
    }

    public Event submit(EventRequest request) {
        Event event = new Event(request.type(), request.source(), request.payload());
        if (request.metadata() != null) {
            event.setMetadata(request.metadata());
        }

        String json = EventSerializer.serialize(event);
        try {
            kafkaTemplate.send(KafkaTopics.EVENTS_RAW, event.getId(), json)
                    .get(ingestProperties.getKafkaSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventSubmissionException("Interrupted while submitting event to Kafka", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new EventSubmissionException("Failed to submit event to Kafka", e);
        }

        log.info("Event submitted: id={}, type={}, source={}", event.getId(), event.getType(), event.getSource());
        return event;
    }
}
