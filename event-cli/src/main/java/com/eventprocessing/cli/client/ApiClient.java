package com.eventprocessing.cli.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String adminUrl;
    private final String ingestUrl;

    public ApiClient(String adminUrl, String ingestUrl) {
        this.adminUrl = adminUrl;
        this.ingestUrl = ingestUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // Pipelines

    public List<JsonNode> listPipelines() throws IOException, InterruptedException {
        return getList(adminUrl + "/api/pipelines");
    }

    public JsonNode getPipeline(String name) throws IOException, InterruptedException {
        return get(adminUrl + "/api/pipelines/" + name);
    }

    public List<JsonNode> getPipelineVersions(String name) throws IOException, InterruptedException {
        return getList(adminUrl + "/api/pipelines/" + name + "/versions");
    }

    public JsonNode createPipeline(String body) throws IOException, InterruptedException {
        return post(adminUrl + "/api/pipelines", body);
    }

    public JsonNode updatePipeline(String name, String body) throws IOException, InterruptedException {
        return put(adminUrl + "/api/pipelines/" + name, body);
    }

    public void deletePipeline(String name) throws IOException, InterruptedException {
        delete(adminUrl + "/api/pipelines/" + name);
    }

    public JsonNode createDraft(String name) throws IOException, InterruptedException {
        return post(adminUrl + "/api/pipelines/" + name + "/draft", "");
    }

    public JsonNode deploy(String name, int version) throws IOException, InterruptedException {
        return post(adminUrl + "/api/pipelines/" + name + "/versions/" + version + "/deploy", "");
    }

    public JsonNode pause(String name) throws IOException, InterruptedException {
        return post(adminUrl + "/api/pipelines/" + name + "/pause", "");
    }

    public JsonNode resume(String name) throws IOException, InterruptedException {
        return post(adminUrl + "/api/pipelines/" + name + "/resume", "");
    }

    public JsonNode testMapping(String name, String payload) throws IOException, InterruptedException {
        return post(adminUrl + "/api/pipelines/" + name + "/test", payload);
    }

    // Topics

    public List<String> listTopics() throws IOException, InterruptedException {
        String json = getString(adminUrl + "/api/topics");
        return mapper.readValue(json, new TypeReference<>() {});
    }

    public JsonNode discoverSchema(String topic) throws IOException, InterruptedException {
        return get(adminUrl + "/api/topics/" + topic + "/schema");
    }

    public List<JsonNode> getSampleEvents(String topic, int count) throws IOException, InterruptedException {
        return getList(adminUrl + "/api/topics/" + topic + "/sample?count=" + count);
    }

    // Events

    public JsonNode submitEvent(String body) throws IOException, InterruptedException {
        return post(ingestUrl + "/api/events", body);
    }

    // Status

    public JsonNode adminStatus() throws IOException, InterruptedException {
        return get(adminUrl + "/api/status");
    }

    public JsonNode ingestHealth() throws IOException, InterruptedException {
        return get(ingestUrl + "/api/health");
    }

    // HTTP methods

    private JsonNode get(String url) throws IOException, InterruptedException {
        String json = getString(url);
        return mapper.readTree(json);
    }

    private List<JsonNode> getList(String url) throws IOException, InterruptedException {
        String json = getString(url);
        return mapper.readValue(json, new TypeReference<>() {});
    }

    private String getString(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return response.body();
    }

    private JsonNode post(String url, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        if (body == null || body.isEmpty()) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return response.body().isEmpty() ? null : mapper.readTree(response.body());
    }

    private JsonNode put(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return mapper.readTree(response.body());
    }

    private void delete(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
    }

    private void checkResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 400) {
            String detail = response.body();
            try {
                JsonNode error = mapper.readTree(detail);
                if (error.has("detail")) {
                    detail = error.get("detail").asText();
                }
            } catch (Exception ignored) {
            }
            throw new IOException("HTTP " + response.statusCode() + ": " + detail);
        }
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
