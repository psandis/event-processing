package com.eventprocessing.detect.embedding;

import com.eventprocessing.detect.config.DetectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final DetectProperties.Embedding config;

    public EmbeddingService(DetectProperties properties) {
        this.config = properties.getEmbedding();
    }

    public boolean isAvailable() {
        return config.isEnabled()
                && config.getApiKey() != null
                && !config.getApiKey().isBlank();
    }

    // Phase 3 implementation:
    // public double[] generateEmbedding(String text) { ... }
    // public double cosineSimilarity(double[] a, double[] b) { ... }
    // public double[] computeCentroid(List<double[]> vectors) { ... }
}
