package com.eventprocessing.ingest;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.ingest.controller.EventController;
import com.eventprocessing.ingest.controller.GlobalExceptionHandler;
import com.eventprocessing.ingest.service.EventSubmitter;
import com.eventprocessing.ingest.service.IngestRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        com.eventprocessing.ingest.config.IngestProperties properties =
                new com.eventprocessing.ingest.config.IngestProperties();
        properties.setMaxBatchSize(2);

        IngestRequestValidator requestValidator =
                new IngestRequestValidator(validator.getValidator(), properties);

        EventController controller = new EventController(new StubEventProducer(), requestValidator);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("event-ingest"));
    }

    @Test
    void submitValidEvent() throws Exception {
        String body = """
                {
                    "type": "order.created",
                    "source": "test-service",
                    "payload": { "orderId": 1 }
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("order.created"))
                .andExpect(jsonPath("$.source").value("test-service"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void submitEventMissingTypeReturns400() throws Exception {
        String body = """
                {
                    "source": "test-service",
                    "payload": { "orderId": 1 }
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void submitEventMissingPayloadReturns400() throws Exception {
        String body = """
                {
                    "type": "order.created",
                    "source": "test-service"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void submitBatch() throws Exception {
        String body = """
                [
                    { "type": "a.1", "source": "s1", "payload": { "x": 1 } },
                    { "type": "a.2", "source": "s2", "payload": { "x": 2 } }
                ]
                """;

        mockMvc.perform(post("/api/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void submitBatchOverLimitReturns400() throws Exception {
        String body = """
                [
                    { "type": "a.1", "source": "s1", "payload": { "x": 1 } },
                    { "type": "a.2", "source": "s2", "payload": { "x": 2 } },
                    { "type": "a.3", "source": "s3", "payload": { "x": 3 } }
                ]
                """;

        mockMvc.perform(post("/api/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Batch Size Exceeded"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed Request"));
    }

    static final class StubEventProducer implements EventSubmitter {

        @Override
        public Event submit(EventRequest request) {
            return new Event(request.type(), request.source(), request.payload());
        }
    }
}
