package com.eventprocessing.detect.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "anomaly_alerts")
public class AnomalyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "detector_type", nullable = false, length = 50)
    private String detectorType;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_source")
    private String eventSource;

    @Column(name = "event_id")
    private String eventId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String details;

    @Column(nullable = false)
    private boolean resolved = false;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    public AnomalyAlert() {}

    public AnomalyAlert(String detectorType, String severity, String title) {
        this.detectorType = detectorType;
        this.severity = severity;
        this.title = title;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDetectorType() { return detectorType; }
    public void setDetectorType(String detectorType) { this.detectorType = detectorType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventSource() { return eventSource; }
    public void setEventSource(String eventSource) { this.eventSource = eventSource; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
