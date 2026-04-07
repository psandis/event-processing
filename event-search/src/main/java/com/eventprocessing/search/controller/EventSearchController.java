package com.eventprocessing.search.controller;

import com.eventprocessing.search.StoredEvent;
import com.eventprocessing.search.service.EventSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Search stored events")
public class EventSearchController {

    private final EventSearchService searchService;

    public EventSearchController(EventSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search events with filters")
    public ResponseEntity<Page<StoredEvent>> searchEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(type, source, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<StoredEvent> getEvent(@PathVariable String id) {
        return ResponseEntity.ok(searchService.getById(id));
    }

    @GetMapping("/types")
    @Operation(summary = "List all event types")
    public ResponseEntity<List<String>> getTypes() {
        return ResponseEntity.ok(searchService.getTypes());
    }

    @GetMapping("/sources")
    @Operation(summary = "List all event sources")
    public ResponseEntity<List<String>> getSources() {
        return ResponseEntity.ok(searchService.getSources());
    }

    @GetMapping("/stats")
    @Operation(summary = "Event statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(searchService.getStats());
    }
}
