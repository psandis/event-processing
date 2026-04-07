package com.eventprocessing.detect.detector;

import com.eventprocessing.common.discovery.SchemaDiscovery;
import com.eventprocessing.common.discovery.SchemaDiscovery.FieldInfo;
import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.config.DetectProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class SchemaDriftDetector {

    private static final Logger log = LoggerFactory.getLogger(SchemaDriftDetector.class);

    private final JdbcTemplate jdbc;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;
    private final SchemaDiscovery schemaDiscovery;
    private final int sampleSize;

    public SchemaDriftDetector(JdbcTemplate jdbc, AlertService alertService,
                                ObjectMapper objectMapper, DetectProperties properties) {
        this.jdbc = jdbc;
        this.alertService = alertService;
        this.objectMapper = objectMapper;
        this.schemaDiscovery = new SchemaDiscovery();
        this.sampleSize = properties.getSchema().getSampleSize();
    }

    @Scheduled(fixedDelayString = "${detect.schema.check-interval-ms:60000}")
    public void detect() {
        List<String> eventTypes = jdbc.queryForList(
                "SELECT DISTINCT type FROM stored_events", String.class);

        for (String eventType : eventTypes) {
            checkSchemaForType(eventType);
        }
    }

    void checkSchemaForType(String eventType) {
        List<String> payloads = jdbc.queryForList(
                "SELECT payload FROM stored_events WHERE type = ? ORDER BY stored_at DESC LIMIT ?",
                String.class, eventType, sampleSize);

        if (payloads.isEmpty()) return;

        Set<String> fieldNames = new TreeSet<>();
        for (String payload : payloads) {
            try {
                JsonNode node = objectMapper.readTree(payload);
                Map<String, FieldInfo> fields = schemaDiscovery.discover(node);
                fieldNames.addAll(fields.keySet());
            } catch (Exception e) {
                log.debug("Could not parse payload for schema detection: {}", e.getMessage());
            }
        }

        String schemaHash = hash(fieldNames.toString());
        String fieldsJson;
        try {
            fieldsJson = objectMapper.writeValueAsString(fieldNames);
        } catch (Exception e) {
            return;
        }

        Integer existingCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM schema_snapshots WHERE event_type = ?",
                Integer.class, eventType);

        if (existingCount == null || existingCount == 0) {
            jdbc.update(
                    "INSERT INTO schema_snapshots (event_type, schema_hash, field_names, sample_count) VALUES (?, ?, ?::jsonb, ?)",
                    eventType, schemaHash, fieldsJson, payloads.size());
            return;
        }

        String lastHash = jdbc.queryForObject(
                "SELECT schema_hash FROM schema_snapshots WHERE event_type = ? ORDER BY last_seen_at DESC LIMIT 1",
                String.class, eventType);

        if (schemaHash.equals(lastHash)) {
            jdbc.update(
                    "UPDATE schema_snapshots SET last_seen_at = NOW(), sample_count = ? WHERE event_type = ? AND schema_hash = ?",
                    payloads.size(), eventType, schemaHash);
            return;
        }

        Set<String> previousFields = getPreviousFields(eventType, lastHash);
        Set<String> addedFields = fieldNames.stream()
                .filter(f -> !previousFields.contains(f))
                .collect(Collectors.toSet());
        Set<String> removedFields = previousFields.stream()
                .filter(f -> !fieldNames.contains(f))
                .collect(Collectors.toSet());

        jdbc.update(
                "INSERT INTO schema_snapshots (event_type, schema_hash, field_names, sample_count) VALUES (?, ?, ?::jsonb, ?) ON CONFLICT (event_type, schema_hash) DO UPDATE SET last_seen_at = NOW()",
                eventType, schemaHash, fieldsJson, payloads.size());

        StringBuilder desc = new StringBuilder();
        if (!addedFields.isEmpty()) desc.append("New fields: ").append(addedFields).append(". ");
        if (!removedFields.isEmpty()) desc.append("Missing fields: ").append(removedFields).append(". ");

        alertService.createAlert(
                "SCHEMA_DRIFT", "MEDIUM",
                eventType, null,
                "Schema change detected for " + eventType,
                desc.toString().trim()
        );
    }

    private Set<String> getPreviousFields(String eventType, String hash) {
        String json = jdbc.queryForObject(
                "SELECT field_names FROM schema_snapshots WHERE event_type = ? AND schema_hash = ?",
                String.class, eventType, hash);
        try {
            List<String> fields = objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
            return new TreeSet<>(fields);
        } catch (Exception e) {
            return Set.of();
        }
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
