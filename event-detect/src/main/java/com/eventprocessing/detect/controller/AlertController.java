package com.eventprocessing.detect.controller;

import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.alert.AnomalyAlert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Anomaly detection alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List alerts with optional filters")
    public ResponseEntity<Page<AnomalyAlert>> getAlerts(
            @RequestParam(required = false) String detectorType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Boolean resolved,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(alertService.getAlerts(detectorType, severity, eventType, resolved, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AnomalyAlert> getAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlert(id));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Mark alert as resolved")
    public ResponseEntity<AnomalyAlert> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Alert statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(alertService.getStats());
    }
}
