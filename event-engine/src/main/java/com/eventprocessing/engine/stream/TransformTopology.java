package com.eventprocessing.engine.stream;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventStatus;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.engine.executor.MappingExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class TransformTopology {

    private static final Logger log = LoggerFactory.getLogger(TransformTopology.class);
    private static final String DEAD_LETTER_TOPIC = "events.failed";
    private static final String SUCCESS_BRANCH = "success";
    private static final String FAILURE_BRANCH = "failure";

    private final MappingExecutor mappingExecutor;

    public TransformTopology(MappingExecutor mappingExecutor) {
        this.mappingExecutor = mappingExecutor;
    }

    public void buildTopology(StreamsBuilder builder, PipelineDefinition pipeline) {
        String deadLetterTopic = pipeline.getErrorHandling() != null
                && pipeline.getErrorHandling().getDeadLetterTopic() != null
                ? pipeline.getErrorHandling().getDeadLetterTopic()
                : DEAD_LETTER_TOPIC;

        KStream<String, String> source = builder.stream(
                pipeline.getSourceTopic(),
                Consumed.with(Serdes.String(), Serdes.String())
        );

        KStream<String, String> transformed = source.mapValues(
                value -> transform(value, pipeline));

        transformed.split(Named.as("transform-"))
                .branch((key, value) -> !isFailedEvent(value),
                        Branched.withConsumer(stream ->
                                stream.to(pipeline.getDestinationTopic(),
                                        Produced.with(Serdes.String(), Serdes.String())),
                                SUCCESS_BRANCH))
                .branch((key, value) -> isFailedEvent(value),
                        Branched.withConsumer(stream ->
                                stream.to(deadLetterTopic,
                                        Produced.with(Serdes.String(), Serdes.String())),
                                FAILURE_BRANCH));

        log.info("Topology built: {} -> {} (dead letter: {})",
                pipeline.getSourceTopic(), pipeline.getDestinationTopic(), deadLetterTopic);
    }

    private String transform(String rawValue, PipelineDefinition pipeline) {
        try {
            Event event = EventSerializer.deserialize(rawValue);
            JsonNode transformed = mappingExecutor.execute(event.getPayload(), pipeline);

            event.setPayload(transformed);
            event.setStatus(EventStatus.PROCESSED);
            event.setProcessedAt(OffsetDateTime.now());

            return EventSerializer.serialize(event);
        } catch (Exception e) {
            log.error("Transform failed for pipeline {}: {}", pipeline.getName(), e.getMessage());
            return toFailedEvent(rawValue, pipeline.getName(), e.getMessage());
        }
    }

    private String toFailedEvent(String rawValue, String pipelineName, String errorMessage) {
        try {
            Event event = EventSerializer.deserialize(rawValue);
            event.setStatus(EventStatus.FAILED);
            event.setErrorMessage("[" + pipelineName + "] " + errorMessage);
            event.setProcessedAt(OffsetDateTime.now());
            return EventSerializer.serialize(event);
        } catch (Exception e) {
            log.error("Could not parse event for dead letter: {}", e.getMessage());
            return rawValue;
        }
    }

    private boolean isFailedEvent(String value) {
        try {
            Event event = EventSerializer.deserialize(value);
            return event.getStatus() == EventStatus.FAILED;
        } catch (Exception e) {
            return true;
        }
    }
}
