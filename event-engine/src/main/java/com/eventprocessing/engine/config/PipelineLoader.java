package com.eventprocessing.engine.config;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.mapping.PipelineState;
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
        this(properties, RestClient.builder()
                .baseUrl(properties.adminUrl())
                .build());
    }

    PipelineLoader(EngineProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
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

        if (pipeline.getState() != PipelineState.ACTIVE && pipeline.getState() != PipelineState.DRAFT) {
            throw new IllegalStateException("Pipeline is not active or draft: " + name + " (state: " + pipeline.getState() + ")");
        }

        log.info("Pipeline loaded: {} v{} ({} -> {}, {} field mappings, state: {})",
                pipeline.getName(),
                pipeline.getVersion(),
                pipeline.getSourceTopic(),
                pipeline.getDestinationTopic(),
                pipeline.getFieldMappings().size(),
                pipeline.getState());

        return pipeline;
    }
}
