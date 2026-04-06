package com.eventprocessing.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "engine")
public record EngineProperties(
        String pipelineName,
        String adminUrl
) {
}
