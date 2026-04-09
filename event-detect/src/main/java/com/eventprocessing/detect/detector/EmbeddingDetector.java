package com.eventprocessing.detect.detector;

import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.config.DetectProperties;
import com.eventprocessing.detect.embedding.EmbeddingException;
import com.eventprocessing.detect.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingDetector {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingDetector.class);
    private static final int BATCH_SIZE = 20;

    private final EmbeddingService embeddingService;
    private final AlertService alertService;
    private final JdbcTemplate jdbc;
    private final DetectProperties.Embedding config;

    public EmbeddingDetector(EmbeddingService embeddingService, AlertService alertService,
                              JdbcTemplate jdbc, DetectProperties properties) {
        this.embeddingService = embeddingService;
        this.alertService = alertService;
        this.jdbc = jdbc;
        this.config = properties.getEmbedding();
    }

    @Scheduled(fixedDelayString = "${detect.embedding.check-interval-ms:300000}")
    public void detect() {
        if (!embeddingService.isAvailable()) {
            log.debug("Embedding detection disabled");
            return;
        }

        log.info("Running embedding anomaly detection");

        List<String> eventTypes = jdbc.queryForList(
                "SELECT DISTINCT type FROM stored_events", String.class);

        for (String eventType : eventTypes) {
            detectForType(eventType);
        }
    }

    void detectForType(String eventType) {
        List<Map<String, Object>> recentEvents = jdbc.queryForList(
                "SELECT id, payload FROM stored_events WHERE type = ? ORDER BY stored_at DESC LIMIT ?",
                eventType, BATCH_SIZE);

        if (recentEvents.size() < 3) {
            log.debug("Not enough events for type {} to compute embeddings (need at least 3)", eventType);
            return;
        }

        List<double[]> embeddings = new ArrayList<>();
        List<String> eventIds = new ArrayList<>();

        for (Map<String, Object> event : recentEvents) {
            String payload = (String) event.get("payload");
            String eventId = (String) event.get("id");

            try {
                double[] embedding = embeddingService.generateEmbedding(payload);
                embeddings.add(embedding);
                eventIds.add(eventId);
            } catch (EmbeddingException e) {
                log.warn("Failed to generate embedding for event {}: {}", eventId, e.getMessage());
            }
        }

        if (embeddings.size() < 3) return;

        double[] centroid = embeddingService.computeCentroid(embeddings);

        for (int i = 0; i < embeddings.size(); i++) {
            double similarity = embeddingService.cosineSimilarity(embeddings.get(i), centroid);
            double distance = 1.0 - similarity;

            if (distance > config.getAnomalyThreshold()) {
                String eventId = eventIds.get(i);
                log.info("Anomalous event detected: {} (type={}, distance={:.4f}, threshold={})",
                        eventId, eventType, distance, config.getAnomalyThreshold());

                alertService.createAlert(
                        "EMBEDDING", "LOW",
                        eventType, null,
                        "Content anomaly detected",
                        String.format("Event %s has unusual content for type %s (distance: %.4f, threshold: %.4f)",
                                eventId, eventType, distance, config.getAnomalyThreshold())
                );
            }
        }
    }
}
