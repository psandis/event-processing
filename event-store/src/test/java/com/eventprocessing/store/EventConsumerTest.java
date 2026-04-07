package com.eventprocessing.store;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.store.entity.StoredEvent;
import com.eventprocessing.store.repository.StoredEventRepository;
import com.eventprocessing.store.service.EventConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private StoredEventRepository repository;

    private EventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new EventConsumer(repository, objectMapper);
    }

    @Test
    void consumeStoresEvent() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("orderId", 1);
        Event event = new Event("order.created", "test", payload);
        String json = EventSerializer.serialize(event);

        when(repository.existsById(event.getId())).thenReturn(false);

        consumer.consume(json);

        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        verify(repository).save(captor.capture());

        StoredEvent stored = captor.getValue();
        assertThat(stored.getId()).isEqualTo(event.getId());
        assertThat(stored.getType()).isEqualTo("order.created");
        assertThat(stored.getSource()).isEqualTo("test");
        assertThat(stored.getStatus()).isEqualTo("RECEIVED");
    }

    @Test
    void consumeSkipsDuplicates() {
        ObjectNode payload = objectMapper.createObjectNode();
        Event event = new Event("test", "test", payload);
        String json = EventSerializer.serialize(event);

        when(repository.existsById(event.getId())).thenReturn(true);

        consumer.consume(json);

        verify(repository, never()).save(any());
    }

    @Test
    void consumeHandlesInvalidJson() {
        consumer.consume("not valid json");
        verify(repository, never()).save(any());
    }
}
