package com.eventprocessing.admin.controller;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.admin.service.PipelineService;
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

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/pipelines")
    @Operation(summary = "List all pipelines")
    public ResponseEntity<List<PipelineDefinition>> listPipelines() {
        return ResponseEntity.ok(pipelineService.getAllPipelines());
    }

    @GetMapping("/pipelines/{name}")
    @Operation(summary = "Get pipeline by name")
    public ResponseEntity<PipelineDefinition> getPipeline(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.getPipeline(name));
    }

    @PostMapping("/pipelines")
    @Operation(summary = "Create a new pipeline")
    public ResponseEntity<PipelineDefinition> createPipeline(@Valid @RequestBody PipelineDefinition definition) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.createPipeline(definition));
    }

    @PutMapping("/pipelines/{name}")
    @Operation(summary = "Update a pipeline")
    public ResponseEntity<PipelineDefinition> updatePipeline(
            @PathVariable String name,
            @Valid @RequestBody PipelineDefinition definition) {
        return ResponseEntity.ok(pipelineService.updatePipeline(name, definition));
    }

    @DeleteMapping("/pipelines/{name}")
    @Operation(summary = "Delete a pipeline")
    public ResponseEntity<Void> deletePipeline(@PathVariable String name) {
        pipelineService.deletePipeline(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pipelines/{name}/pause")
    @Operation(summary = "Pause a pipeline")
    public ResponseEntity<PipelineDefinition> pausePipeline(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.togglePipeline(name, false));
    }

    @PostMapping("/pipelines/{name}/resume")
    @Operation(summary = "Resume a pipeline")
    public ResponseEntity<PipelineDefinition> resumePipeline(@PathVariable String name) {
        return ResponseEntity.ok(pipelineService.togglePipeline(name, true));
    }

    @GetMapping("/status")
    @Operation(summary = "Platform health status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "event-admin"));
    }
}
