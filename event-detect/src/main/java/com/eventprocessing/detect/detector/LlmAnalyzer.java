package com.eventprocessing.detect.detector;

import com.eventprocessing.detect.config.DetectProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LlmAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(LlmAnalyzer.class);

    private final DetectProperties.Embedding config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LlmAnalyzer(DetectProperties properties, ObjectMapper objectMapper) {
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

    public String analyzeAnomaly(String eventPayload, String eventType, String historicalContext) {
        if (!isAvailable()) {
            return "LLM analysis unavailable: API key not configured";
        }

        try {
            String prompt = buildPrompt(eventPayload, eventType, historicalContext);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", config.getModel(),
                    "max_tokens", 500,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl() + "/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("LLM API returned {}: {}", response.statusCode(), response.body());
                return "LLM analysis failed: API returned " + response.statusCode();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.at("/content/0/text");

            if (content.isMissingNode()) {
                return "LLM analysis failed: unexpected response format";
            }

            return content.asText();
        } catch (Exception e) {
            log.error("LLM analysis failed: {}", e.getMessage());
            return "LLM analysis failed: " + e.getMessage();
        }
    }

    private String buildPrompt(String eventPayload, String eventType, String historicalContext) {
        return """
                You are an event stream anomaly analyst. Analyze this event and explain what is unusual about it.

                Event type: %s

                Event payload:
                %s

                Historical context (typical events of this type):
                %s

                Provide a concise analysis:
                1. What is unusual about this event compared to typical events of this type?
                2. What could have caused this anomaly?
                3. What is the recommended action?

                Keep your response under 200 words.
                """.formatted(eventType, eventPayload, historicalContext);
    }
}
