package com.eventprocessing.engine.config;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.engine.stream.TransformTopology;
import org.apache.kafka.streams.StreamsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TopologyConfigurer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(TopologyConfigurer.class);

    private final StreamsBuilder streamsBuilder;
    private final TransformTopology transformTopology;
    private final PipelineDefinition pipeline;

    public TopologyConfigurer(
            @Qualifier("defaultKafkaStreamsBuilder") StreamsBuilder streamsBuilder,
            TransformTopology transformTopology,
            PipelineDefinition pipeline) {
        this.streamsBuilder = streamsBuilder;
        this.transformTopology = transformTopology;
        this.pipeline = pipeline;
    }

    @Override
    public void afterPropertiesSet() {
        transformTopology.buildTopology(streamsBuilder, pipeline);
        log.info("Engine ready. Pipeline: {} ({} -> {}, {} field mappings)",
                pipeline.getName(),
                pipeline.getSourceTopic(),
                pipeline.getDestinationTopic(),
                pipeline.getFieldMappings().size());
    }
}
