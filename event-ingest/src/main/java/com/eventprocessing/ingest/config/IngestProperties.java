package com.eventprocessing.ingest.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {

    @Min(1)
    private int maxBatchSize = 500;

    @NotNull
    private Duration kafkaSendTimeout = Duration.ofSeconds(5);

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public Duration getKafkaSendTimeout() {
        return kafkaSendTimeout;
    }

    public void setKafkaSendTimeout(Duration kafkaSendTimeout) {
        this.kafkaSendTimeout = kafkaSendTimeout;
    }
}
