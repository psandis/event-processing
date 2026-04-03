package com.eventprocessing.ingest;

import com.eventprocessing.ingest.service.EventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
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
}
