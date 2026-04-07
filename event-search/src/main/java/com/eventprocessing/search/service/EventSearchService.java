package com.eventprocessing.search.service;

import com.eventprocessing.search.StoredEvent;
import com.eventprocessing.search.StoredEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EventSearchService {

    private final StoredEventRepository repository;

    public EventSearchService(StoredEventRepository repository) {
        this.repository = repository;
    }

    public Page<StoredEvent> search(String type, String source, String status,
                                     OffsetDateTime from, OffsetDateTime to,
                                     Pageable pageable) {
        if (type != null && source != null) {
            return repository.findByTypeAndSource(type, source, pageable);
        }
        if (type != null) {
            return repository.findByType(type, pageable);
        }
        if (source != null) {
            return repository.findBySource(source, pageable);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        if (from != null && to != null) {
            return repository.findByReceivedAtBetween(from, to, pageable);
        }
        return repository.findAll(pageable);
    }

    public StoredEvent getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    public List<String> getTypes() {
        return repository.findDistinctTypes();
    }

    public List<String> getSources() {
        return repository.findDistinctSources();
    }

    public Map<String, Object> getStats() {
        long total = repository.count();
        List<String> types = repository.findDistinctTypes();
        List<String> sources = repository.findDistinctSources();

        return Map.of(
                "totalEvents", total,
                "types", types.size(),
                "sources", sources.size()
        );
    }
}
