package com.eventprocessing.ingest.service;

import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.ingest.config.IngestProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class IngestRequestValidator {

    private final Validator validator;
    private final IngestProperties ingestProperties;

    public IngestRequestValidator(Validator validator, IngestProperties ingestProperties) {
        this.validator = validator;
        this.ingestProperties = ingestProperties;
    }

    public void validateEventRequest(EventRequest request) {
        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    public void validateBatchSize(int batchSize) {
        if (batchSize > ingestProperties.getMaxBatchSize()) {
            throw new BatchSizeExceededException(batchSize, ingestProperties.getMaxBatchSize());
        }
    }
}
