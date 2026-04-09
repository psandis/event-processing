package com.eventprocessing.detect;

import com.eventprocessing.detect.config.DetectProperties;
import com.eventprocessing.detect.detector.LlmAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmAnalyzerTest {

    @Test
    void isAvailableReturnsFalseByDefault() {
        DetectProperties properties = new DetectProperties();
        LlmAnalyzer analyzer = new LlmAnalyzer(properties, new ObjectMapper());

        assertThat(analyzer.isAvailable()).isFalse();
    }

    @Test
    void isAvailableReturnsTrueWhenConfigured() {
        DetectProperties properties = new DetectProperties();
        properties.getEmbedding().setEnabled(true);
        properties.getEmbedding().setApiKey("test-key");
        LlmAnalyzer analyzer = new LlmAnalyzer(properties, new ObjectMapper());

        assertThat(analyzer.isAvailable()).isTrue();
    }

    @Test
    void analyzeAnomalyReturnsUnavailableWhenNotConfigured() {
        DetectProperties properties = new DetectProperties();
        LlmAnalyzer analyzer = new LlmAnalyzer(properties, new ObjectMapper());

        String result = analyzer.analyzeAnomaly("{}", "test.event", "no history");

        assertThat(result).contains("unavailable");
    }
}
