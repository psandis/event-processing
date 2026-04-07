package com.eventprocessing.store.repository;

import com.eventprocessing.store.entity.StoredEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface StoredEventRepository extends JpaRepository<StoredEvent, String> {

    Page<StoredEvent> findByType(String type, Pageable pageable);

    Page<StoredEvent> findBySource(String source, Pageable pageable);

    Page<StoredEvent> findByTypeAndSource(String type, String source, Pageable pageable);

    Page<StoredEvent> findByReceivedAtBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    Page<StoredEvent> findByStatus(String status, Pageable pageable);

    @Query("SELECT DISTINCT e.type FROM StoredEvent e ORDER BY e.type")
    List<String> findDistinctTypes();

    @Query("SELECT DISTINCT e.source FROM StoredEvent e ORDER BY e.source")
    List<String> findDistinctSources();

    long countByType(String type);

    long countBySource(String source);

    long countByStatus(String status);
}
