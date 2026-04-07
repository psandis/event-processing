package com.eventprocessing.admin;

import com.eventprocessing.admin.controller.GlobalExceptionHandler;
import com.eventprocessing.admin.controller.PipelineController;
import com.eventprocessing.admin.service.MappingTestService;
import com.eventprocessing.admin.service.PipelineAlreadyExistsException;
import com.eventprocessing.admin.service.PipelineNotFoundException;
import com.eventprocessing.admin.service.PipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.common.mapping.PipelineState;
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

        MappingTestService mappingTestService = new MappingTestService(new ObjectMapper());
        PipelineController controller = new PipelineController(new InMemoryPipelineService(), mappingTestService);

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
    void createPipelineStartsAsDraft() throws Exception {
        String body = pipelineJson("test-create", "source.a", "dest.a");

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("test-create")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.state", is("DRAFT")));
    }

    @Test
    void getPipelineReturnsLatest() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-get", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pipelines/test-get"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("test-get")));
    }

    @Test
    void updateDraftPipeline() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-update", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/pipelines/test-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-update", "source.b", "dest.b")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTopic", is("source.b")));
    }

    @Test
    void deployDraftVersion() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-deploy", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pipelines/test-deploy/versions/1/deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("ACTIVE")));
    }

    @Test
    void pauseAndResumeActivePipeline() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-pause", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pipelines/test-pause/versions/1/deploy"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pipelines/test-pause/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("PAUSED")));

        mockMvc.perform(post("/api/pipelines/test-pause/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("ACTIVE")));
    }

    @Test
    void deletePipeline() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-delete", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/pipelines/test-delete"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pipelines/test-delete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testMappingReturnsTransformedOutput() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-mapping", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        String samplePayload = """
                { "a": "hello" }
                """;

        mockMvc.perform(post("/api/pipelines/test-mapping/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(samplePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.b").value("hello"));
    }

    @Test
    void cannotUpdateActivePipeline() throws Exception {
        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-no-update", "source.a", "dest.a")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/pipelines/test-no-update/versions/1/deploy"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/pipelines/test-no-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pipelineJson("test-no-update", "source.b", "dest.b")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listPipelines() throws Exception {
        mockMvc.perform(get("/api/pipelines"))
                .andExpect(status().isOk());
    }

    private String pipelineJson(String name, String source, String dest) {
        return """
                {
                    "name": "%s",
                    "sourceTopic": "%s",
                    "destinationTopic": "%s",
                    "fieldMappings": [
                        { "sourceField": "a", "destinationField": "b" }
                    ]
                }
                """.formatted(name, source, dest);
    }

    static final class InMemoryPipelineService extends PipelineService {

        private final Map<String, List<PipelineDefinition>> pipelines = new LinkedHashMap<>();

        private InMemoryPipelineService() {
            super(null, null);
        }

        @Override
        public List<PipelineDefinition> getAllPipelines() {
            return pipelines.values().stream()
                    .flatMap(List::stream)
                    .toList();
        }

        @Override
        public PipelineDefinition getPipeline(String name) {
            List<PipelineDefinition> versions = pipelines.get(name);
            if (versions == null || versions.isEmpty()) {
                throw new PipelineNotFoundException(name);
            }
            return copy(versions.getLast());
        }

        @Override
        public PipelineDefinition getPipelineVersion(String name, int version) {
            return getVersions(name).stream()
                    .filter(p -> p.getVersion() == version)
                    .findFirst()
                    .map(this::copy)
                    .orElseThrow(() -> new PipelineNotFoundException(name + " v" + version));
        }

        @Override
        public List<PipelineDefinition> getPipelineVersions(String name) {
            return new ArrayList<>(getVersions(name));
        }

        @Override
        public PipelineDefinition createPipeline(PipelineDefinition definition) {
            if (pipelines.containsKey(definition.getName())) {
                throw new PipelineAlreadyExistsException(definition.getName());
            }
            PipelineDefinition pipeline = copy(definition);
            pipeline.setVersion(1);
            pipeline.setState(PipelineState.DRAFT);
            pipelines.put(pipeline.getName(), new ArrayList<>(List.of(pipeline)));
            return copy(pipeline);
        }

        @Override
        public PipelineDefinition updatePipeline(String name, PipelineDefinition definition) {
            PipelineDefinition current = getPipeline(name);
            if (current.getState() == PipelineState.ACTIVE) {
                throw new IllegalStateException("Cannot directly update an ACTIVE pipeline.");
            }
            PipelineDefinition updated = copy(definition);
            updated.setVersion(current.getVersion());
            updated.setState(current.getState());
            List<PipelineDefinition> versions = getVersions(name);
            versions.set(versions.size() - 1, updated);
            return copy(updated);
        }

        @Override
        public PipelineDefinition createDraftVersion(String name) {
            PipelineDefinition latest = getPipeline(name);
            PipelineDefinition draft = copy(latest);
            draft.setVersion(latest.getVersion() + 1);
            draft.setState(PipelineState.DRAFT);
            getVersions(name).add(draft);
            return copy(draft);
        }

        @Override
        public PipelineDefinition deploy(String name, int version) {
            PipelineDefinition target = getPipelineVersion(name, version);
            if (target.getState() != PipelineState.DRAFT) {
                throw new IllegalStateException("Only DRAFT pipelines can be deployed.");
            }
            getVersions(name).stream()
                    .filter(p -> p.getState() == PipelineState.ACTIVE)
                    .forEach(p -> p.setState(PipelineState.PAUSED));
            getVersions(name).stream()
                    .filter(p -> p.getVersion() == version)
                    .forEach(p -> p.setState(PipelineState.ACTIVE));
            return copy(getPipelineVersion(name, version));
        }

        @Override
        public PipelineDefinition pause(String name) {
            PipelineDefinition active = getVersions(name).stream()
                    .filter(p -> p.getState() == PipelineState.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active version"));
            active.setState(PipelineState.PAUSED);
            return copy(active);
        }

        @Override
        public PipelineDefinition resume(String name) {
            PipelineDefinition paused = getVersions(name).stream()
                    .filter(p -> p.getState() == PipelineState.PAUSED)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No paused version"));
            paused.setState(PipelineState.ACTIVE);
            return copy(paused);
        }

        @Override
        public void deletePipeline(String name) {
            if (pipelines.remove(name) == null) {
                throw new PipelineNotFoundException(name);
            }
        }

        @Override
        public void deletePipelineVersion(String name, int version) {
            PipelineDefinition target = getPipelineVersion(name, version);
            if (target.getState() == PipelineState.ACTIVE) {
                throw new IllegalStateException("Cannot delete ACTIVE version");
            }
            getVersions(name).removeIf(p -> p.getVersion() == version);
        }

        private List<PipelineDefinition> getVersions(String name) {
            List<PipelineDefinition> versions = pipelines.get(name);
            if (versions == null || versions.isEmpty()) {
                throw new PipelineNotFoundException(name);
            }
            return versions;
        }

        private PipelineDefinition copy(PipelineDefinition source) {
            PipelineDefinition target = new PipelineDefinition();
            target.setName(source.getName());
            target.setDescription(source.getDescription());
            target.setSourceTopic(source.getSourceTopic());
            target.setDestinationTopic(source.getDestinationTopic());
            target.setVersion(source.getVersion());
            target.setState(source.getState());
            target.setFieldMappings(source.getFieldMappings() == null ? List.of() : List.copyOf(source.getFieldMappings()));
            target.setErrorHandling(source.getErrorHandling());
            return target;
        }
    }
}
