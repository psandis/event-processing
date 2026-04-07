package com.eventprocessing.admin.service;

import com.eventprocessing.common.mapping.ErrorHandling;
import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.mapping.PipelineState;
import com.eventprocessing.admin.repository.PipelineEntity;
import com.eventprocessing.admin.repository.PipelineRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final PipelineRepository pipelineRepository;
    private final ObjectMapper objectMapper;

    public PipelineService(PipelineRepository pipelineRepository, ObjectMapper objectMapper) {
        this.pipelineRepository = pipelineRepository;
        this.objectMapper = objectMapper;
    }

    public List<PipelineDefinition> getAllPipelines() {
        return pipelineRepository.findAll().stream()
                .map(this::toDefinition)
                .toList();
    }

    public PipelineDefinition getPipeline(String name) {
        PipelineEntity entity = pipelineRepository.findByName(name)
                .orElseThrow(() -> new PipelineNotFoundException(name));
        return toDefinition(entity);
    }

    public PipelineDefinition getPipelineVersion(String name, int version) {
        PipelineEntity entity = pipelineRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new PipelineNotFoundException(name + " v" + version));
        return toDefinition(entity);
    }

    public List<PipelineDefinition> getPipelineVersions(String name) {
        List<PipelineEntity> versions = pipelineRepository.findByNameOrderByVersionDesc(name);
        if (versions.isEmpty()) {
            throw new PipelineNotFoundException(name);
        }
        return versions.stream().map(this::toDefinition).toList();
    }

    @Transactional
    public PipelineDefinition createPipeline(PipelineDefinition definition) {
        if (pipelineRepository.existsByName(definition.getName())) {
            throw new PipelineAlreadyExistsException(definition.getName());
        }

        definition.setVersion(1);
        definition.setState(PipelineState.DRAFT);

        PipelineEntity entity = toEntity(definition);
        entity = pipelineRepository.save(entity);

        log.info("Pipeline created: {} v{} ({})", entity.getName(), entity.getVersion(), entity.getState());
        return toDefinition(entity);
    }

    @Transactional
    public PipelineDefinition updatePipeline(String name, PipelineDefinition definition) {
        PipelineEntity entity = pipelineRepository.findByName(name)
                .orElseThrow(() -> new PipelineNotFoundException(name));

        if (PipelineState.ACTIVE.name().equals(entity.getState())) {
            throw new IllegalStateException(
                    "Cannot directly update an ACTIVE pipeline. Create a new draft version instead.");
        }

        entity.setDescription(definition.getDescription());
        entity.setSourceTopic(definition.getSourceTopic());
        entity.setDestinationTopic(definition.getDestinationTopic());
        entity.setFieldMappings(serializeJson(definition.getFieldMappings()));
        entity.setErrorHandling(definition.getErrorHandling() != null
                ? serializeJson(definition.getErrorHandling()) : null);

        entity = pipelineRepository.save(entity);

        log.info("Pipeline updated: {} v{}", entity.getName(), entity.getVersion());
        return toDefinition(entity);
    }

    @Transactional
    public PipelineDefinition createDraftVersion(String name) {
        List<PipelineEntity> versions = pipelineRepository.findByNameOrderByVersionDesc(name);
        if (versions.isEmpty()) {
            throw new PipelineNotFoundException(name);
        }

        PipelineEntity latest = versions.getFirst();
        int nextVersion = latest.getVersion() + 1;

        PipelineEntity draft = new PipelineEntity();
        draft.setName(name);
        draft.setDescription(latest.getDescription());
        draft.setSourceTopic(latest.getSourceTopic());
        draft.setDestinationTopic(latest.getDestinationTopic());
        draft.setVersion(nextVersion);
        draft.setState(PipelineState.DRAFT.name());
        draft.setFieldMappings(latest.getFieldMappings());
        draft.setErrorHandling(latest.getErrorHandling());

        draft = pipelineRepository.save(draft);

        log.info("Draft version created: {} v{} (from v{})", name, nextVersion, latest.getVersion());
        return toDefinition(draft);
    }

    @Transactional
    public PipelineDefinition deploy(String name, int version) {
        PipelineEntity entity = pipelineRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new PipelineNotFoundException(name + " v" + version));

        if (!PipelineState.DRAFT.name().equals(entity.getState())) {
            throw new IllegalStateException("Only DRAFT pipelines can be deployed. Current state: " + entity.getState());
        }

        pipelineRepository.findByNameAndState(name, PipelineState.ACTIVE.name())
                .ifPresent(active -> {
                    active.setState(PipelineState.PAUSED.name());
                    pipelineRepository.save(active);
                    log.info("Previous active version paused: {} v{}", name, active.getVersion());
                });

        entity.setState(PipelineState.ACTIVE.name());
        entity = pipelineRepository.save(entity);

        log.info("Pipeline deployed: {} v{} (ACTIVE)", name, version);
        return toDefinition(entity);
    }

    @Transactional
    public PipelineDefinition pause(String name) {
        PipelineEntity entity = pipelineRepository.findByNameAndState(name, PipelineState.ACTIVE.name())
                .orElseThrow(() -> new IllegalStateException("No active version found for pipeline: " + name));

        entity.setState(PipelineState.PAUSED.name());
        entity = pipelineRepository.save(entity);

        log.info("Pipeline paused: {} v{}", name, entity.getVersion());
        return toDefinition(entity);
    }

    @Transactional
    public PipelineDefinition resume(String name) {
        PipelineEntity entity = pipelineRepository.findByNameAndState(name, PipelineState.PAUSED.name())
                .orElseThrow(() -> new IllegalStateException("No paused version found for pipeline: " + name));

        entity.setState(PipelineState.ACTIVE.name());
        entity = pipelineRepository.save(entity);

        log.info("Pipeline resumed: {} v{}", name, entity.getVersion());
        return toDefinition(entity);
    }

    @Transactional
    public void deletePipeline(String name) {
        if (!pipelineRepository.existsByName(name)) {
            throw new PipelineNotFoundException(name);
        }
        pipelineRepository.deleteByName(name);
        log.info("Pipeline deleted (all versions): {}", name);
    }

    @Transactional
    public void deletePipelineVersion(String name, int version) {
        PipelineEntity entity = pipelineRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new PipelineNotFoundException(name + " v" + version));

        if (PipelineState.ACTIVE.name().equals(entity.getState())) {
            throw new IllegalStateException("Cannot delete an ACTIVE pipeline version. Pause it first.");
        }

        pipelineRepository.deleteByNameAndVersion(name, version);
        log.info("Pipeline version deleted: {} v{}", name, version);
    }

    private PipelineDefinition toDefinition(PipelineEntity entity) {
        PipelineDefinition def = new PipelineDefinition();
        def.setName(entity.getName());
        def.setDescription(entity.getDescription());
        def.setSourceTopic(entity.getSourceTopic());
        def.setDestinationTopic(entity.getDestinationTopic());
        def.setVersion(entity.getVersion());
        def.setState(PipelineState.valueOf(entity.getState()));
        def.setFieldMappings(deserializeFieldMappings(entity.getFieldMappings()));
        if (entity.getErrorHandling() != null) {
            def.setErrorHandling(deserializeErrorHandling(entity.getErrorHandling()));
        }
        return def;
    }

    private PipelineEntity toEntity(PipelineDefinition definition) {
        PipelineEntity entity = new PipelineEntity();
        entity.setName(definition.getName());
        entity.setDescription(definition.getDescription());
        entity.setSourceTopic(definition.getSourceTopic());
        entity.setDestinationTopic(definition.getDestinationTopic());
        entity.setVersion(definition.getVersion());
        entity.setState(definition.getState().name());
        entity.setFieldMappings(serializeJson(definition.getFieldMappings()));
        if (definition.getErrorHandling() != null) {
            entity.setErrorHandling(serializeJson(definition.getErrorHandling()));
        }
        return entity;
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private List<FieldMapping> deserializeFieldMappings(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize field mappings", e);
        }
    }

    private ErrorHandling deserializeErrorHandling(String json) {
        try {
            return objectMapper.readValue(json, ErrorHandling.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize error handling", e);
        }
    }
}
