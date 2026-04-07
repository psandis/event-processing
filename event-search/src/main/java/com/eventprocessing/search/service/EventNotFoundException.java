package com.eventprocessing.search.service;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String id) {
        super("Event not found: " + id);
    }
}
