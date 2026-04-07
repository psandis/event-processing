package com.eventprocessing.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "stored_events")
public class StoredEvent {

    @Id
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @CreationTimestamp
    @Column(name = "stored_at")
    private OffsetDateTime storedAt;

    public StoredEvent() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }

    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }

    public OffsetDateTime getStoredAt() { return storedAt; }
    public void setStoredAt(OffsetDateTime storedAt) { this.storedAt = storedAt; }
}
