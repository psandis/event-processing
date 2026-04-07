package com.eventprocessing.admin.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pipeline_definitions")
public class PipelineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "source_topic", nullable = false)
    private String sourceTopic;

    @Column(name = "destination_topic", nullable = false)
    private String destinationTopic;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false, length = 20)
    private String state = "DRAFT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mappings", nullable = false, columnDefinition = "jsonb")
    private String fieldMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_handling", columnDefinition = "jsonb")
    private String errorHandling;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public PipelineEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceTopic() { return sourceTopic; }
    public void setSourceTopic(String sourceTopic) { this.sourceTopic = sourceTopic; }

    public String getDestinationTopic() { return destinationTopic; }
    public void setDestinationTopic(String destinationTopic) { this.destinationTopic = destinationTopic; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(String fieldMappings) { this.fieldMappings = fieldMappings; }

    public String getErrorHandling() { return errorHandling; }
    public void setErrorHandling(String errorHandling) { this.errorHandling = errorHandling; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
