package com.eventprocessing.ingest.service;

public class EventSubmissionException extends RuntimeException {

    public EventSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
