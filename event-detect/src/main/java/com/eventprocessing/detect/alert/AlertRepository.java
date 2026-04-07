package com.eventprocessing.detect.alert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<AnomalyAlert, Long> {

    Page<AnomalyAlert> findByResolved(boolean resolved, Pageable pageable);

    Page<AnomalyAlert> findByDetectorType(String detectorType, Pageable pageable);

    Page<AnomalyAlert> findByEventType(String eventType, Pageable pageable);

    Page<AnomalyAlert> findBySeverity(String severity, Pageable pageable);

    long countByResolved(boolean resolved);
}
