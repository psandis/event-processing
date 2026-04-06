package com.eventprocessing.admin;

import com.eventprocessing.admin.controller.GlobalExceptionHandler;
import com.eventprocessing.admin.controller.PipelineController;
import com.eventprocessing.admin.service.PipelineAlreadyExistsException;
import com.eventprocessing.admin.service.PipelineNotFoundException;
import com.eventprocessing.admin.service.PipelineService;
import com.eventprocessing.common.mapping.PipelineDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PipelineControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        PipelineController controller = new PipelineController(new InMemoryPipelineService());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

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

    static final class InMemoryPipelineService extends PipelineService {

        private final Map<String, PipelineDefinition> pipelines = new LinkedHashMap<>();

        private InMemoryPipelineService() {
            super(null, null);
        }

        @Override
        public List<PipelineDefinition> getAllPipelines() {
            return new ArrayList<>(pipelines.values());
        }

        @Override
        public PipelineDefinition getPipeline(String name) {
            PipelineDefinition pipeline = pipelines.get(name);
            if (pipeline == null) {
                throw new PipelineNotFoundException(name);
            }
            return copy(pipeline);
        }

        @Override
        public PipelineDefinition createPipeline(PipelineDefinition definition) {
            if (pipelines.containsKey(definition.getName())) {
                throw new PipelineAlreadyExistsException(definition.getName());
            }
            PipelineDefinition copy = copy(definition);
            pipelines.put(copy.getName(), copy);
            return copy(copy);
        }

        @Override
        public PipelineDefinition updatePipeline(String name, PipelineDefinition definition) {
            if (!pipelines.containsKey(name)) {
                throw new PipelineNotFoundException(name);
            }
            PipelineDefinition copy = copy(definition);
            pipelines.put(name, copy);
            return copy(copy);
        }

        @Override
        public void deletePipeline(String name) {
            if (pipelines.remove(name) == null) {
                throw new PipelineNotFoundException(name);
            }
        }

        @Override
        public PipelineDefinition togglePipeline(String name, boolean enabled) {
            PipelineDefinition pipeline = pipelines.get(name);
            if (pipeline == null) {
                throw new PipelineNotFoundException(name);
            }
            pipeline.setEnabled(enabled);
            return copy(pipeline);
        }

        private PipelineDefinition copy(PipelineDefinition source) {
            PipelineDefinition target = new PipelineDefinition();
            target.setName(source.getName());
            target.setDescription(source.getDescription());
            target.setSourceTopic(source.getSourceTopic());
            target.setDestinationTopic(source.getDestinationTopic());
            target.setEnabled(source.isEnabled());
            target.setFieldMappings(source.getFieldMappings() == null ? List.of() : List.copyOf(source.getFieldMappings()));
            target.setErrorHandling(source.getErrorHandling());
            return target;
        }
    }
}
