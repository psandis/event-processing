package com.eventprocessing.detect.detector;

import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.config.DetectProperties;
import com.eventprocessing.detect.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingDetector {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingDetector.class);

    private final EmbeddingService embeddingService;
    private final AlertService alertService;
    private final DetectProperties.Embedding config;

    public EmbeddingDetector(EmbeddingService embeddingService, AlertService alertService,
                              DetectProperties properties) {
        this.embeddingService = embeddingService;
        this.alertService = alertService;
        this.config = properties.getEmbedding();
    }

    @Scheduled(fixedDelayString = "${detect.embedding.check-interval-ms:300000}")
    public void detect() {
        if (!config.isEnabled()) {
            log.debug("Embedding detection disabled. Set detect.embedding.enabled=true and provide API key.");
            return;
        }

        log.info("Running embedding anomaly detection...");
        // Phase 3 implementation:
        // 1. Fetch recent unprocessed events from stored_events
        // 2. Generate embeddings via EmbeddingService
        // 3. Store embeddings in Pgvector
        // 4. Compute cosine distance from centroid per event type
        // 5. Flag outliers above threshold
        // 6. Create alerts for anomalous events
    }
}
