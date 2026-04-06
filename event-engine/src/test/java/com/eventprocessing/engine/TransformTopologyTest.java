package com.eventprocessing.engine;

import com.eventprocessing.common.mapping.ConversionType;
import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.engine.executor.MappingExecutor;
import com.eventprocessing.engine.stream.TransformTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class TransformTopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;
    private TestOutputTopic<String, String> deadLetterTopic;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MappingExecutor executor = new MappingExecutor();
        TransformTopology topology = new TransformTopology(executor);

        FieldMapping renameField = new FieldMapping("userId", "customerId");
        FieldMapping convertField = new FieldMapping("total", "amount");
        convertField.setConversion(ConversionType.TO_DOUBLE);

        PipelineDefinition pipeline = new PipelineDefinition();
        pipeline.setName("test-pipeline");
        pipeline.setSourceTopic("input-topic");
        pipeline.setDestinationTopic("output-topic");
        pipeline.setFieldMappings(List.of(renameField, convertField));

        StreamsBuilder builder = new StreamsBuilder();
        topology.buildTopology(builder, pipeline);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        testDriver = new TopologyTestDriver(builder.build(), props);
        inputTopic = testDriver.createInputTopic("input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());
        deadLetterTopic = testDriver.createOutputTopic("events.failed", Serdes.String().deserializer(), Serdes.String().deserializer());
    }

    @AfterEach
    void tearDown() {
        if (testDriver != null) {
            testDriver.close();
        }
    }

    @Test
    void transformsEventSuccessfully() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("userId", 42);
        payload.put("total", "99.99");

        Event event = new Event("test.event", "test-source", payload);
        String json = EventSerializer.serialize(event);

        inputTopic.pipeInput(event.getId(), json);

        assertThat(outputTopic.isEmpty()).isFalse();
        String output = outputTopic.readValue();
        Event result = EventSerializer.deserialize(output);

        assertThat(result.getStatus().name()).isEqualTo("PROCESSED");
        assertThat(result.getPayload().get("customerId").intValue()).isEqualTo(42);
        assertThat(result.getPayload().get("amount").doubleValue()).isEqualTo(99.99);
        assertThat(result.getPayload().has("userId")).isFalse();
        assertThat(deadLetterTopic.isEmpty()).isTrue();
    }

    @Test
    void sendsFailedEventToDeadLetter() {
        inputTopic.pipeInput("bad-key", "not valid json at all");

        assertThat(outputTopic.isEmpty()).isTrue();
        assertThat(deadLetterTopic.isEmpty()).isFalse();
        String failed = deadLetterTopic.readValue();
        assertThat(failed).contains("not valid json at all");
    }

    @Test
    void processesMultipleEvents() {
        for (int i = 0; i < 5; i++) {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("userId", i);
            payload.put("total", String.valueOf(i * 10.0));

            Event event = new Event("test.event", "test-source", payload);
            inputTopic.pipeInput(event.getId(), EventSerializer.serialize(event));
        }

        assertThat(outputTopic.getQueueSize()).isEqualTo(5);
        assertThat(deadLetterTopic.isEmpty()).isTrue();
    }
}
