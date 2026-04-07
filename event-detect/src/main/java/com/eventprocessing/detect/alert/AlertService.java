package com.eventprocessing.detect.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AnomalyAlert createAlert(String detectorType, String severity,
                                     String eventType, String eventSource,
                                     String title, String description) {
        AnomalyAlert alert = new AnomalyAlert(detectorType, severity, title);
        alert.setEventType(eventType);
        alert.setEventSource(eventSource);
        alert.setDescription(description);
        alert = repository.save(alert);

        log.warn("Anomaly detected [{}] {}: {} (type={}, source={})",
                severity, detectorType, title, eventType, eventSource);
        return alert;
    }

    public Page<AnomalyAlert> getAlerts(String detectorType, String severity,
                                         String eventType, Boolean resolved,
                                         Pageable pageable) {
        if (detectorType != null) return repository.findByDetectorType(detectorType, pageable);
        if (severity != null) return repository.findBySeverity(severity, pageable);
        if (eventType != null) return repository.findByEventType(eventType, pageable);
        if (resolved != null) return repository.findByResolved(resolved, pageable);
        return repository.findAll(pageable);
    }

    public AnomalyAlert getAlert(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
    }

    @Transactional
    public AnomalyAlert resolveAlert(Long id) {
        AnomalyAlert alert = getAlert(id);
        alert.setResolved(true);
        return repository.save(alert);
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "total", repository.count(),
                "open", repository.countByResolved(false),
                "resolved", repository.countByResolved(true)
        );
    }
}
