package com.eventprocessing.search;

import com.eventprocessing.search.controller.EventSearchController;
import com.eventprocessing.search.controller.GlobalExceptionHandler;
import com.eventprocessing.search.service.EventNotFoundException;
import com.eventprocessing.search.service.EventSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventSearchControllerTest {

    private MockMvc mockMvc;
    private InMemorySearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new InMemorySearchService();
        mockMvc = MockMvcBuilders.standaloneSetup(new EventSearchController(searchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void searchEventsReturnsPage() throws Exception {
        searchService.addEvent(createEvent("evt_1", "order.created", "test"));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].type", is("order.created")));
    }

    @Test
    void searchByTypeFilters() throws Exception {
        searchService.addEvent(createEvent("evt_1", "order.created", "test"));
        searchService.addEvent(createEvent("evt_2", "user.signup", "auth"));

        mockMvc.perform(get("/api/events").param("type", "order.created"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void getEventByIdReturns200() throws Exception {
        searchService.addEvent(createEvent("evt_1", "order.created", "test"));

        mockMvc.perform(get("/api/events/evt_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("evt_1")));
    }

    @Test
    void getEventByIdReturns404() throws Exception {
        mockMvc.perform(get("/api/events/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTypesReturnsList() throws Exception {
        searchService.addEvent(createEvent("evt_1", "order.created", "test"));
        searchService.addEvent(createEvent("evt_2", "user.signup", "auth"));

        mockMvc.perform(get("/api/events/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void getSourcesReturnsList() throws Exception {
        searchService.addEvent(createEvent("evt_1", "order.created", "test"));

        mockMvc.perform(get("/api/events/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void getStatsReturnsMap() throws Exception {
        searchService.addEvent(createEvent("evt_1", "order.created", "test"));

        mockMvc.perform(get("/api/events/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents", is(1)));
    }

    private StoredEvent createEvent(String id, String type, String source) {
        StoredEvent event = new StoredEvent();
        event.setId(id);
        event.setType(type);
        event.setSource(source);
        event.setPayload("{}");
        event.setStatus("RECEIVED");
        event.setReceivedAt(OffsetDateTime.now());
        return event;
    }

    static class InMemorySearchService extends EventSearchService {

        private final java.util.List<StoredEvent> events = new java.util.ArrayList<>();

        InMemorySearchService() {
            super(null);
        }

        void addEvent(StoredEvent event) {
            events.add(event);
        }

        @Override
        public org.springframework.data.domain.Page<StoredEvent> search(
                String type, String source, String status,
                OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
            List<StoredEvent> filtered = events.stream()
                    .filter(e -> type == null || e.getType().equals(type))
                    .filter(e -> source == null || e.getSource().equals(source))
                    .toList();
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        @Override
        public StoredEvent getById(String id) {
            return events.stream()
                    .filter(e -> e.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new EventNotFoundException(id));
        }

        @Override
        public List<String> getTypes() {
            return events.stream().map(StoredEvent::getType).distinct().sorted().toList();
        }

        @Override
        public List<String> getSources() {
            return events.stream().map(StoredEvent::getSource).distinct().sorted().toList();
        }

        @Override
        public Map<String, Object> getStats() {
            return Map.of("totalEvents", (long) events.size(), "types", getTypes().size(), "sources", getSources().size());
        }
    }
}
