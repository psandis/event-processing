package com.eventprocessing.admin.controller;

import com.eventprocessing.admin.service.TopicService;
import com.eventprocessing.common.discovery.SchemaDiscovery.FieldInfo;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@Tag(name = "Topics", description = "Kafka topic discovery endpoints")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    @Operation(summary = "List available Kafka topics")
    public ResponseEntity<List<String>> listTopics() {
        return ResponseEntity.ok(topicService.listTopics());
    }

    @GetMapping("/{topic}/schema")
    @Operation(summary = "Discover schema from topic sample events")
    public ResponseEntity<Map<String, FieldInfo>> discoverSchema(@PathVariable String topic) {
        return ResponseEntity.ok(topicService.discoverSchema(topic));
    }

    @GetMapping("/{topic}/sample")
    @Operation(summary = "Get sample events from a topic")
    public ResponseEntity<List<JsonNode>> getSampleEvents(
            @PathVariable String topic,
            @RequestParam(defaultValue = "5") int count) {
        return ResponseEntity.ok(topicService.getSampleEvents(topic, count));
    }
}
