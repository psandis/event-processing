package com.eventprocessing.ingest.service;

public class BatchSizeExceededException extends RuntimeException {

    public BatchSizeExceededException(int actualSize, int maxBatchSize) {
        super("Batch size %d exceeds maximum of %d".formatted(actualSize, maxBatchSize));
    }
}
