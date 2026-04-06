package com.eventprocessing.admin.service;

public class PipelineAlreadyExistsException extends RuntimeException {

    public PipelineAlreadyExistsException(String name) {
        super("Pipeline already exists: " + name);
    }
}
