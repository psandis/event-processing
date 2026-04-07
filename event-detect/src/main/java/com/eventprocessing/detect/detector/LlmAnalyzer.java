package com.eventprocessing.detect.detector;

import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.config.DetectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(LlmAnalyzer.class);

    private final AlertService alertService;
    private final DetectProperties.Embedding config;

    public LlmAnalyzer(AlertService alertService, DetectProperties properties) {
        this.alertService = alertService;
        this.config = properties.getEmbedding();
    }

    public boolean isAvailable() {
        return config.isEnabled()
                && config.getApiKey() != null
                && !config.getApiKey().isBlank();
    }

    // Phase 3 implementation:
    // public String analyzeAnomaly(String eventPayload, String historicalContext) { ... }
    // Sends event + context to LLM, returns natural language explanation
}
