package com.eventprocessing.engine.config;

import com.eventprocessing.common.mapping.PipelineDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PipelineLoader {

    private static final Logger log = LoggerFactory.getLogger(PipelineLoader.class);

    private final RestClient restClient;
    private final EngineProperties properties;

    public PipelineLoader(EngineProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.adminUrl())
                .build();
    }

    public PipelineDefinition load() {
        String name = properties.pipelineName();
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("PIPELINE_NAME environment variable is required");
        }

        log.info("Loading pipeline definition: {} from {}", name, properties.adminUrl());

        PipelineDefinition pipeline = restClient.get()
                .uri("/api/pipelines/{name}", name)
                .retrieve()
                .body(PipelineDefinition.class);

        if (pipeline == null) {
            throw new IllegalStateException("Pipeline not found: " + name);
        }

        if (!pipeline.isEnabled()) {
            throw new IllegalStateException("Pipeline is disabled: " + name);
        }

        log.info("Pipeline loaded: {} ({} -> {}, {} field mappings)",
                pipeline.getName(),
                pipeline.getSourceTopic(),
                pipeline.getDestinationTopic(),
                pipeline.getFieldMappings().size());

        return pipeline;
    }
}
