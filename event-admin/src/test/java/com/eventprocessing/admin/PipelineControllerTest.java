package com.eventprocessing.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void statusEndpoint() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createAndGetPipeline() throws Exception {
        String body = """
                {
                    "name": "test-pipeline-1",
                    "description": "Test pipeline",
                    "sourceTopic": "events.raw",
                    "destinationTopic": "events.processed",
                    "enabled": true,
                    "fieldMappings": [
                        { "sourceField": "userId", "destinationField": "customerId" }
                    ]
                }
                """;

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("test-pipeline-1")));

        mockMvc.perform(get("/api/pipelines/test-pipeline-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTopic", is("events.raw")))
                .andExpect(jsonPath("$.destinationTopic", is("events.processed")));
    }

    @Test
    void updatePipeline() throws Exception {
        String create = """
                {
                    "name": "test-pipeline-2",
                    "sourceTopic": "source.a",
                    "destinationTopic": "dest.a",
                    "fieldMappings": [
                        { "sourceField": "a", "destinationField": "b" }
                    ]
                }
                """;

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated());

        String update = """
                {
                    "name": "test-pipeline-2",
                    "description": "Updated",
                    "sourceTopic": "source.b",
                    "destinationTopic": "dest.b",
                    "fieldMappings": [
                        { "sourceField": "x", "destinationField": "y" }
                    ]
                }
                """;

        mockMvc.perform(put("/api/pipelines/test-pipeline-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTopic", is("source.b")))
                .andExpect(jsonPath("$.description", is("Updated")));
    }

    @Test
    void deletePipeline() throws Exception {
        String body = """
                {
                    "name": "test-pipeline-3",
                    "sourceTopic": "source.c",
                    "destinationTopic": "dest.c",
                    "fieldMappings": []
                }
                """;

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/pipelines/test-pipeline-3"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pipelines/test-pipeline-3"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pauseAndResumePipeline() throws Exception {
        String body = """
                {
                    "name": "test-pipeline-4",
                    "sourceTopic": "source.d",
                    "destinationTopic": "dest.d",
                    "fieldMappings": []
                }
                """;

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pipelines/test-pipeline-4/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)));

        mockMvc.perform(post("/api/pipelines/test-pipeline-4/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    void listPipelines() throws Exception {
        mockMvc.perform(get("/api/pipelines"))
                .andExpect(status().isOk());
    }
}
