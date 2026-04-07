package com.eventprocessing.admin.controller;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.admin.service.MappingTestService;
import com.eventprocessing.admin.service.PipelineService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Pipelines", description = "Pipeline management endpoints")
public class PipelineController {

    private final PipelineService pipelineService;
    private final MappingTestService mappingTestService;

    public PipelineController(PipelineService pipelineService, MappingTestService mappingTestService) {
        this.pipelineService = pipelineService;
        this.mappingTestService = mappingTestService;
    }

    @GetMapping("/pipelines")
    @Operation(summary = "List all pipelines")
    public ResponseEntity<List<PipelineDefinition>> listPipelines() {
        return ResponseEntity.ok(pipelineService.getAllPipelines());
    }

    @GetMapping("/pipelines/{name}")
    @Operation(summary = "Get latest version of a pipeline")
    public ResponseEntity<PipelineDefinition> getPipeline(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.getPipeline(name));
    }

    @GetMapping("/pipelines/{name}/versions")
    @Operation(summary = "List all versions of a pipeline")
    public ResponseEntity<List<PipelineDefinition>> getPipelineVersions(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.getPipelineVersions(name));
    }

    @GetMapping("/pipelines/{name}/versions/{version}")
    @Operation(summary = "Get a specific version of a pipeline")
    public ResponseEntity<PipelineDefinition> getPipelineVersion(
            @PathVariable String name, @PathVariable int version) {
        return ResponseEntity.ok(pipelineService.getPipelineVersion(name, version));
    }

    @PostMapping("/pipelines")
    @Operation(summary = "Create a new pipeline (starts as DRAFT)")
    public ResponseEntity<PipelineDefinition> createPipeline(@Valid @RequestBody PipelineDefinition definition) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.createPipeline(definition));
    }

    @PutMapping("/pipelines/{name}")
    @Operation(summary = "Update a DRAFT pipeline")
    public ResponseEntity<PipelineDefinition> updatePipeline(
            @PathVariable String name,
            @Valid @RequestBody PipelineDefinition definition) {
        return ResponseEntity.ok(pipelineService.updatePipeline(name, definition));
    }

    @PostMapping("/pipelines/{name}/draft")
    @Operation(summary = "Create a new draft version from the latest version")
    public ResponseEntity<PipelineDefinition> createDraft(@PathVariable String name) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.createDraftVersion(name));
    }

    @PostMapping("/pipelines/{name}/versions/{version}/deploy")
    @Operation(summary = "Deploy a DRAFT version (pauses current ACTIVE if any)")
    public ResponseEntity<PipelineDefinition> deploy(
            @PathVariable String name, @PathVariable int version) {
        return ResponseEntity.ok(pipelineService.deploy(name, version));
    }

    @PostMapping("/pipelines/{name}/pause")
    @Operation(summary = "Pause the active version")
    public ResponseEntity<PipelineDefinition> pausePipeline(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.pause(name));
    }

    @PostMapping("/pipelines/{name}/resume")
    @Operation(summary = "Resume a paused version")
    public ResponseEntity<PipelineDefinition> resumePipeline(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.resume(name));
    }

    @DeleteMapping("/pipelines/{name}")
    @Operation(summary = "Delete a pipeline and all its versions")
    public ResponseEntity<Void> deletePipeline(@PathVariable String name) {
        pipelineService.deletePipeline(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pipelines/{name}/versions/{version}")
    @Operation(summary = "Delete a specific version (cannot delete ACTIVE)")
    public ResponseEntity<Void> deletePipelineVersion(
            @PathVariable String name, @PathVariable int version) {
        pipelineService.deletePipelineVersion(name, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pipelines/{name}/test")
    @Operation(summary = "Test a pipeline mapping with sample data")
    public ResponseEntity<JsonNode> testMapping(
            @PathVariable String name,
            @RequestBody JsonNode samplePayload) {
        PipelineDefinition pipeline = pipelineService.getPipeline(name);
        JsonNode result = mappingTestService.testMapping(pipeline, samplePayload);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @Operation(summary = "Platform health status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "event-admin"));
    }
}
