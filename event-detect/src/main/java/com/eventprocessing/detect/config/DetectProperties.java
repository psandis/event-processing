package com.eventprocessing.detect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "detect")
public class DetectProperties {

    private Statistical statistical = new Statistical();
    private Schema schema = new Schema();
    private Embedding embedding = new Embedding();

    public Statistical getStatistical() { return statistical; }
    public void setStatistical(Statistical statistical) { this.statistical = statistical; }

    public Schema getSchema() { return schema; }
    public void setSchema(Schema schema) { this.schema = schema; }

    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }

    public static class Statistical {
        private int windowMinutes = 5;
        private double spikeThreshold = 3.0;
        private double dropThreshold = 0.3;
        private int minimumEvents = 10;

        public int getWindowMinutes() { return windowMinutes; }
        public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

        public double getSpikeThreshold() { return spikeThreshold; }
        public void setSpikeThreshold(double spikeThreshold) { this.spikeThreshold = spikeThreshold; }

        public double getDropThreshold() { return dropThreshold; }
        public void setDropThreshold(double dropThreshold) { this.dropThreshold = dropThreshold; }

        public int getMinimumEvents() { return minimumEvents; }
        public void setMinimumEvents(int minimumEvents) { this.minimumEvents = minimumEvents; }
    }

    public static class Schema {
        private int sampleSize = 20;

        public int getSampleSize() { return sampleSize; }
        public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }
    }

    public static class Embedding {
        private boolean enabled = false;
        private String apiUrl = "";
        private String apiKey = "";
        private String model = "claude-haiku-4-5-20251001";
        private double anomalyThreshold = 0.85;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getAnomalyThreshold() { return anomalyThreshold; }
        public void setAnomalyThreshold(double anomalyThreshold) { this.anomalyThreshold = anomalyThreshold; }
    }
}
