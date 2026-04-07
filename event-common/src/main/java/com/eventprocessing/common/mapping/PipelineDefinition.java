package com.eventprocessing.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipelineDefinition {

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank
    private String sourceTopic;

    @NotBlank
    private String destinationTopic;

    private int version = 1;
    private PipelineState state = PipelineState.DRAFT;

    @NotNull
    private List<FieldMapping> fieldMappings = new ArrayList<>();
    private ErrorHandling errorHandling;

    public PipelineDefinition() {
    }

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

    public PipelineState getState() { return state; }
    public void setState(PipelineState state) { this.state = state; }

    public List<FieldMapping> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(List<FieldMapping> fieldMappings) { this.fieldMappings = fieldMappings; }

    public ErrorHandling getErrorHandling() { return errorHandling; }
    public void setErrorHandling(ErrorHandling errorHandling) { this.errorHandling = errorHandling; }
}
