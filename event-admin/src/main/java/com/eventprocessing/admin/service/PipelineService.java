package com.eventprocessing.admin.service;

import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.admin.repository.PipelineEntity;
import com.eventprocessing.admin.repository.PipelineRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PipelineService {

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
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + name));
        return toDefinition(entity);
    }

    @Transactional
    public PipelineDefinition createPipeline(PipelineDefinition definition) {
        if (pipelineRepository.existsByName(definition.getName())) {
            throw new IllegalArgumentException("Pipeline already exists: " + definition.getName());
        }

        PipelineEntity entity = toEntity(definition);
        entity = pipelineRepository.save(entity);
        return toDefinition(entity);
    }

    @Transactional
    public PipelineDefinition updatePipeline(String name, PipelineDefinition definition) {
        PipelineEntity entity = pipelineRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + name));

        entity.setDescription(definition.getDescription());
        entity.setSourceTopic(definition.getSourceTopic());
        entity.setDestinationTopic(definition.getDestinationTopic());
        entity.setEnabled(definition.isEnabled());
        entity.setFieldMappings(serializeJson(definition.getFieldMappings()));
        if (definition.getErrorHandling() != null) {
            entity.setErrorHandling(serializeJson(definition.getErrorHandling()));
        }

        entity = pipelineRepository.save(entity);
        return toDefinition(entity);
    }

    @Transactional
    public void deletePipeline(String name) {
        if (!pipelineRepository.existsByName(name)) {
            throw new IllegalArgumentException("Pipeline not found: " + name);
        }
        pipelineRepository.deleteByName(name);
    }

    @Transactional
    public PipelineDefinition togglePipeline(String name, boolean enabled) {
        PipelineEntity entity = pipelineRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found: " + name));
        entity.setEnabled(enabled);
        entity = pipelineRepository.save(entity);
        return toDefinition(entity);
    }

    private PipelineDefinition toDefinition(PipelineEntity entity) {
        PipelineDefinition def = new PipelineDefinition();
        def.setName(entity.getName());
        def.setDescription(entity.getDescription());
        def.setSourceTopic(entity.getSourceTopic());
        def.setDestinationTopic(entity.getDestinationTopic());
        def.setEnabled(entity.isEnabled());
        def.setFieldMappings(deserializeFieldMappings(entity.getFieldMappings()));
        return def;
    }

    private PipelineEntity toEntity(PipelineDefinition definition) {
        PipelineEntity entity = new PipelineEntity();
        entity.setName(definition.getName());
        entity.setDescription(definition.getDescription());
        entity.setSourceTopic(definition.getSourceTopic());
        entity.setDestinationTopic(definition.getDestinationTopic());
        entity.setEnabled(definition.isEnabled());
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
            throw new IllegalArgumentException("Failed to serialize JSON", e);
        }
    }

    private List<FieldMapping> deserializeFieldMappings(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize field mappings", e);
        }
    }
}
