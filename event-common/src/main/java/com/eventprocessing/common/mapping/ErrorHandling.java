package com.eventprocessing.common.mapping;

public class ErrorHandling {

    private int retries = 3;
    private long backoffMs = 1000;
    private String deadLetterTopic = "events.failed";

    public ErrorHandling() {
    }

    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }

    public long getBackoffMs() { return backoffMs; }
    public void setBackoffMs(long backoffMs) { this.backoffMs = backoffMs; }

    public String getDeadLetterTopic() { return deadLetterTopic; }
    public void setDeadLetterTopic(String deadLetterTopic) { this.deadLetterTopic = deadLetterTopic; }
}
