package com.eventprocessing.admin.service;

public class PipelineNotFoundException extends RuntimeException {

    public PipelineNotFoundException(String name) {
        super("Pipeline not found: " + name);
    }
}
