package com.eventprocessing.engine.stream;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventStatus;
import com.eventprocessing.common.serialization.EventSerializer;
import com.eventprocessing.engine.executor.MappingExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class TransformTopology {

    private static final Logger log = LoggerFactory.getLogger(TransformTopology.class);
    private static final String DEAD_LETTER_TOPIC = "events.failed";

    private final MappingExecutor mappingExecutor;

    public TransformTopology(MappingExecutor mappingExecutor) {
        this.mappingExecutor = mappingExecutor;
    }

    public void buildTopology(StreamsBuilder builder, PipelineDefinition pipeline) {
        KStream<String, String> source = builder.stream(
                pipeline.getSourceTopic(),
                Consumed.with(Serdes.String(), Serdes.String())
        );

        source.mapValues(value -> transform(value, pipeline))
                .filter((key, value) -> value != null)
                .to(pipeline.getDestinationTopic(), Produced.with(Serdes.String(), Serdes.String()));

        log.info("Topology built: {} -> {}", pipeline.getSourceTopic(), pipeline.getDestinationTopic());
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
            return null;
        }
    }
}
