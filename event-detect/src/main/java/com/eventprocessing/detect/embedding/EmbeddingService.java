package com.eventprocessing.detect.embedding;

import com.eventprocessing.detect.config.DetectProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final DetectProperties.Embedding config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(DetectProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getEmbedding();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isAvailable() {
        return config.isEnabled()
                && config.getApiKey() != null
                && !config.getApiKey().isBlank();
    }

    public double[] generateEmbedding(String text) {
        if (!isAvailable()) {
            throw new IllegalStateException("Embedding service is not configured. Set API key and enable.");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(new EmbeddingRequest(
                    config.getModel(),
                    text
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl() + "/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Embedding API returned {}: {}", response.statusCode(), response.body());
                throw new EmbeddingException("Embedding API returned " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingArray = root.at("/data/0/embedding");

            if (embeddingArray.isMissingNode() || !embeddingArray.isArray()) {
                throw new EmbeddingException("Invalid embedding response format");
            }

            double[] embedding = new double[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).doubleValue();
            }

            return embedding;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate embedding", e);
        }
    }

    public double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension: " + a.length + " vs " + b.length);
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) return 0.0;

        return dotProduct / denominator;
    }

    public double[] computeCentroid(List<double[]> vectors) {
        if (vectors.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute centroid of empty list");
        }

        int dimensions = vectors.getFirst().length;
        double[] centroid = new double[dimensions];

        for (double[] vector : vectors) {
            if (vector.length != dimensions) {
                throw new IllegalArgumentException("All vectors must have the same dimension");
            }
            for (int i = 0; i < dimensions; i++) {
                centroid[i] += vector[i];
            }
        }

        for (int i = 0; i < dimensions; i++) {
            centroid[i] /= vectors.size();
        }

        return centroid;
    }

    private record EmbeddingRequest(String model, String input) {
    }
}
