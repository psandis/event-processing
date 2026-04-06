package com.eventprocessing.engine;

import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.engine.config.EngineProperties;
import com.eventprocessing.engine.config.PipelineLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineLoaderTest {

    private MockWebServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void loadsPipelineFromAdmin() throws Exception {
        PipelineDefinition pipeline = new PipelineDefinition();
        pipeline.setName("test-pipeline");
        pipeline.setSourceTopic("events.raw");
        pipeline.setDestinationTopic("events.processed");
        pipeline.setEnabled(true);
        pipeline.setFieldMappings(List.of(new FieldMapping("a", "b")));

        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(pipeline))
                .addHeader("Content-Type", "application/json"));

        EngineProperties props = new EngineProperties("test-pipeline", mockServer.url("/").toString());
        PipelineLoader loader = new PipelineLoader(props);

        PipelineDefinition loaded = loader.load();

        assertThat(loaded.getName()).isEqualTo("test-pipeline");
        assertThat(loaded.getSourceTopic()).isEqualTo("events.raw");
        assertThat(loaded.getFieldMappings()).hasSize(1);
    }

    @Test
    void throwsWhenPipelineNameMissing() {
        EngineProperties props = new EngineProperties("", "http://localhost:8091");
        PipelineLoader loader = new PipelineLoader(props);

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PIPELINE_NAME");
    }

    @Test
    void throwsWhenPipelineNameNull() {
        EngineProperties props = new EngineProperties(null, "http://localhost:8091");
        PipelineLoader loader = new PipelineLoader(props);

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PIPELINE_NAME");
    }

    @Test
    void throwsWhenPipelineDisabled() throws Exception {
        PipelineDefinition pipeline = new PipelineDefinition();
        pipeline.setName("disabled-pipeline");
        pipeline.setSourceTopic("events.raw");
        pipeline.setDestinationTopic("events.processed");
        pipeline.setEnabled(false);
        pipeline.setFieldMappings(List.of());

        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(pipeline))
                .addHeader("Content-Type", "application/json"));

        EngineProperties props = new EngineProperties("disabled-pipeline", mockServer.url("/").toString());
        PipelineLoader loader = new PipelineLoader(props);

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }
}
