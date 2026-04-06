package com.eventprocessing.ingest.controller;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.ingest.service.EventProducer;
import com.eventprocessing.ingest.service.IngestRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Events", description = "Event ingestion endpoints")
public class EventController {

    private final EventProducer eventProducer;
    private final IngestRequestValidator requestValidator;

    public EventController(EventProducer eventProducer, IngestRequestValidator requestValidator) {
        this.eventProducer = eventProducer;
        this.requestValidator = requestValidator;
    }

    @PostMapping("/events")
    @Operation(summary = "Submit a single event")
    public ResponseEntity<Event> submitEvent(@Valid @RequestBody EventRequest request) {
        Event event = eventProducer.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(event);
    }

    @PostMapping("/events/batch")
    @Operation(summary = "Submit multiple events")
    public ResponseEntity<List<Event>> submitBatch(@Valid @RequestBody List<@Valid EventRequest> requests) {
        requestValidator.validateBatchSize(requests.size());
        List<Event> events = requests.stream()
                .map(eventProducer::submit)
                .toList();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(events);
    }

    @GetMapping("/health")
    @Operation(summary = "Service health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "event-ingest"));
    }
}
