package com.eventprocessing.detect;

import com.eventprocessing.detect.config.DetectProperties;
import com.eventprocessing.detect.embedding.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class EmbeddingServiceTest {

    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        DetectProperties properties = new DetectProperties();
        service = new EmbeddingService(properties, new ObjectMapper());
    }

    @Test
    void isAvailableReturnsFalseByDefault() {
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void isAvailableReturnsTrueWhenConfigured() {
        DetectProperties properties = new DetectProperties();
        properties.getEmbedding().setEnabled(true);
        properties.getEmbedding().setApiKey("test-key");
        EmbeddingService configured = new EmbeddingService(properties, new ObjectMapper());

        assertThat(configured.isAvailable()).isTrue();
    }

    @Test
    void generateEmbeddingThrowsWhenNotConfigured() {
        assertThatThrownBy(() -> service.generateEmbedding("test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void cosineSimilarityOfIdenticalVectors() {
        double[] a = {1.0, 2.0, 3.0};
        double similarity = service.cosineSimilarity(a, a);
        assertThat(similarity).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void cosineSimilarityOfOrthogonalVectors() {
        double[] a = {1.0, 0.0, 0.0};
        double[] b = {0.0, 1.0, 0.0};
        double similarity = service.cosineSimilarity(a, b);
        assertThat(similarity).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void cosineSimilarityOfOppositeVectors() {
        double[] a = {1.0, 0.0};
        double[] b = {-1.0, 0.0};
        double similarity = service.cosineSimilarity(a, b);
        assertThat(similarity).isCloseTo(-1.0, within(0.0001));
    }

    @Test
    void cosineSimilarityOfSimilarVectors() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {1.1, 2.1, 3.1};
        double similarity = service.cosineSimilarity(a, b);
        assertThat(similarity).isGreaterThan(0.99);
    }

    @Test
    void cosineSimilarityThrowsOnDifferentDimensions() {
        double[] a = {1.0, 2.0};
        double[] b = {1.0, 2.0, 3.0};
        assertThatThrownBy(() -> service.cosineSimilarity(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same dimension");
    }

    @Test
    void cosineSimilarityOfZeroVectors() {
        double[] a = {0.0, 0.0};
        double[] b = {0.0, 0.0};
        double similarity = service.cosineSimilarity(a, b);
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    void computeCentroidOfSingleVector() {
        double[] v = {1.0, 2.0, 3.0};
        double[] centroid = service.computeCentroid(List.of(v));
        assertThat(centroid).containsExactly(1.0, 2.0, 3.0);
    }

    @Test
    void computeCentroidOfMultipleVectors() {
        double[] a = {0.0, 0.0};
        double[] b = {4.0, 6.0};
        double[] centroid = service.computeCentroid(List.of(a, b));
        assertThat(centroid).containsExactly(2.0, 3.0);
    }

    @Test
    void computeCentroidOfThreeVectors() {
        double[] a = {1.0, 1.0, 1.0};
        double[] b = {2.0, 2.0, 2.0};
        double[] c = {3.0, 3.0, 3.0};
        double[] centroid = service.computeCentroid(List.of(a, b, c));
        assertThat(centroid).containsExactly(2.0, 2.0, 2.0);
    }

    @Test
    void computeCentroidThrowsOnEmptyList() {
        assertThatThrownBy(() -> service.computeCentroid(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void computeCentroidThrowsOnMismatchedDimensions() {
        double[] a = {1.0, 2.0};
        double[] b = {1.0, 2.0, 3.0};
        assertThatThrownBy(() -> service.computeCentroid(List.of(a, b)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same dimension");
    }

    @Test
    void anomalyDetectionFlow() {
        double[] normal1 = {1.0, 1.0, 1.0};
        double[] normal2 = {1.1, 0.9, 1.0};
        double[] normal3 = {0.9, 1.1, 1.0};
        double[] anomalous = {-10.0, -5.0, -20.0};

        List<double[]> normals = List.of(normal1, normal2, normal3);
        double[] centroid = service.computeCentroid(normals);

        double distNormal = 1.0 - service.cosineSimilarity(normal1, centroid);
        double distAnomalous = 1.0 - service.cosineSimilarity(anomalous, centroid);

        assertThat(distNormal).isLessThan(0.01);
        assertThat(distAnomalous).isGreaterThan(1.0);
    }
}
