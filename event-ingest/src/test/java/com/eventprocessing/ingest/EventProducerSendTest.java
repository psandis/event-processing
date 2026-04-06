package com.eventprocessing.ingest;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.ingest.config.IngestProperties;
import com.eventprocessing.ingest.service.EventProducer;
import com.eventprocessing.ingest.service.EventSubmissionException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventProducerSendTest {

    private TestKafkaTemplate kafkaTemplate;
    private EventProducer eventProducer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = new TestKafkaTemplate();

        IngestProperties properties = new IngestProperties();
        properties.setKafkaSendTimeout(Duration.ofSeconds(1));
        eventProducer = new EventProducer(kafkaTemplate, properties);
    }

    @Test
    void submitWaitsForKafkaAck() {
        kafkaTemplate.result = CompletableFuture.completedFuture(null);

        EventRequest request = new EventRequest(
                "order.created",
                "test-service",
                JsonNodeFactory.instance.objectNode().put("orderId", 1),
                null
        );

        Event event = eventProducer.submit(request);

        assertThat(event.getId()).startsWith("evt_");
        assertThat(event.getType()).isEqualTo("order.created");
    }

    @Test
    void submitThrowsWhenKafkaSendFails() {
        kafkaTemplate.result = CompletableFuture.failedFuture(new RuntimeException("broker down"));

        EventRequest request = new EventRequest(
                "order.created",
                "test-service",
                JsonNodeFactory.instance.objectNode().put("orderId", 1),
                null
        );

        assertThatThrownBy(() -> eventProducer.submit(request))
                .isInstanceOf(EventSubmissionException.class)
                .hasMessage("Failed to submit event to Kafka");
    }

    private static final class TestKafkaTemplate extends KafkaTemplate<String, String> {

        private CompletableFuture<SendResult<String, String>> result;

        private TestKafkaTemplate() {
            super(new DefaultKafkaProducerFactory<>(Map.of()));
        }

        @Override
        public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
            return result;
        }
    }
}
