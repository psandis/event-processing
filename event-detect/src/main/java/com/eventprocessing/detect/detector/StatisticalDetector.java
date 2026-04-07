package com.eventprocessing.detect.detector;

import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.config.DetectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class StatisticalDetector {

    private static final Logger log = LoggerFactory.getLogger(StatisticalDetector.class);

    private final JdbcTemplate jdbc;
    private final AlertService alertService;
    private final DetectProperties.Statistical config;

    public StatisticalDetector(JdbcTemplate jdbc, AlertService alertService, DetectProperties properties) {
        this.jdbc = jdbc;
        this.alertService = alertService;
        this.config = properties.getStatistical();
    }

    @Scheduled(fixedDelayString = "${detect.statistical.check-interval-ms:30000}")
    public void detect() {
        OffsetDateTime windowEnd = OffsetDateTime.now();
        OffsetDateTime windowStart = windowEnd.minusMinutes(config.getWindowMinutes());

        List<Map<String, Object>> currentCounts = getCurrentCounts(windowStart, windowEnd);

        for (Map<String, Object> row : currentCounts) {
            String eventType = (String) row.get("type");
            String eventSource = (String) row.get("source");
            long count = ((Number) row.get("cnt")).longValue();

            Double avgCount = getHistoricalAverage(eventType, eventSource);
            Double stddev = getHistoricalStddev(eventType, eventSource);

            if (avgCount == null || stddev == null) {
                saveBaseline(eventType, eventSource, windowStart, windowEnd, count, null, null);
                continue;
            }

            saveBaseline(eventType, eventSource, windowStart, windowEnd, count, avgCount, stddev);

            if (stddev == 0) continue;
            if (count < config.getMinimumEvents()) continue;

            double zScore = (count - avgCount) / stddev;

            if (zScore > config.getSpikeThreshold()) {
                alertService.createAlert(
                        "STATISTICAL", "HIGH",
                        eventType, eventSource,
                        "Volume spike detected",
                        String.format("%s events from %s: %d events in last %d min (avg: %.0f, z-score: %.1f)",
                                eventType, eventSource, count, config.getWindowMinutes(), avgCount, zScore)
                );
            }

            if (count < avgCount * config.getDropThreshold() && avgCount > config.getMinimumEvents()) {
                alertService.createAlert(
                        "STATISTICAL", "MEDIUM",
                        eventType, eventSource,
                        "Volume drop detected",
                        String.format("%s events from %s: %d events in last %d min (avg: %.0f, drop: %.0f%%)",
                                eventType, eventSource, count, config.getWindowMinutes(), avgCount,
                                (1 - (double) count / avgCount) * 100)
                );
            }
        }
    }

    private List<Map<String, Object>> getCurrentCounts(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.queryForList(
                """
                SELECT type, source, COUNT(*) as cnt
                FROM stored_events
                WHERE received_at BETWEEN ? AND ?
                GROUP BY type, source
                """, from, to);
    }

    private Double getHistoricalAverage(String type, String source) {
        return jdbc.queryForObject(
                "SELECT AVG(event_count) FROM event_baselines WHERE event_type = ? AND event_source = ?",
                Double.class, type, source);
    }

    private Double getHistoricalStddev(String type, String source) {
        return jdbc.queryForObject(
                "SELECT STDDEV(event_count) FROM event_baselines WHERE event_type = ? AND event_source = ?",
                Double.class, type, source);
    }

    private void saveBaseline(String type, String source, OffsetDateTime start,
                              OffsetDateTime end, long count, Double avg, Double stddev) {
        jdbc.update(
                """
                INSERT INTO event_baselines (event_type, event_source, window_start, window_end, event_count, avg_count, stddev_count)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_type, event_source, window_start) DO UPDATE
                SET event_count = EXCLUDED.event_count, avg_count = EXCLUDED.avg_count, stddev_count = EXCLUDED.stddev_count
                """, type, source, start, end, count, avg, stddev);
    }
}
