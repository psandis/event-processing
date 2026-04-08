package com.eventprocessing.engine.config;

import com.eventprocessing.common.mapping.FieldMapping;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.mapping.PipelineState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PipelineLoaderTest {

    private MockRestServiceServer mockServer;
    private RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://admin.test");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void loadsPipelineFromAdmin() throws Exception {
        PipelineDefinition pipeline = new PipelineDefinition();
        pipeline.setName("test-pipeline");
        pipeline.setSourceTopic("events.raw");
        pipeline.setDestinationTopic("events.processed");
        pipeline.setState(PipelineState.ACTIVE);
        pipeline.setFieldMappings(List.of(new FieldMapping("a", "b")));

        mockServer.expect(requestTo("http://admin.test/api/pipelines/test-pipeline"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(pipeline), MediaType.APPLICATION_JSON));

        EngineProperties props = new EngineProperties("test-pipeline", "http://admin.test");
        PipelineLoader loader = new PipelineLoader(props, restClient);

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
    void throwsWhenPipelinePaused() throws Exception {
        PipelineDefinition pipeline = new PipelineDefinition();
        pipeline.setName("paused-pipeline");
        pipeline.setSourceTopic("events.raw");
        pipeline.setDestinationTopic("events.processed");
        pipeline.setState(PipelineState.PAUSED);
        pipeline.setFieldMappings(List.of());

        mockServer.expect(requestTo("http://admin.test/api/pipelines/paused-pipeline"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(pipeline), MediaType.APPLICATION_JSON));

        EngineProperties props = new EngineProperties("paused-pipeline", "http://admin.test");
        PipelineLoader loader = new PipelineLoader(props, restClient);

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active or draft");
    }
}
