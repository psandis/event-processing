package com.eventprocessing.ingest;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.ingest.grpc.BatchEventRequest;
import com.eventprocessing.ingest.grpc.BatchEventResponse;
import com.eventprocessing.ingest.grpc.EventResponse;
import com.eventprocessing.ingest.grpc.GrpcEventService;
import com.eventprocessing.ingest.service.EventSubmissionException;
import com.eventprocessing.ingest.service.EventSubmitter;
import com.eventprocessing.ingest.service.IngestRequestValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcEventServiceTest {

    private CapturingEventProducer eventProducer;
    private GrpcEventService grpcService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        eventProducer = new CapturingEventProducer();
        IngestRequestValidator requestValidator = new IngestRequestValidator(
                Validation.buildDefaultValidatorFactory().getValidator(),
                ingestProperties(3)
        );
        grpcService = new GrpcEventService(eventProducer, requestValidator, mapper);
    }

    @Test
    void submitEventReturnsResponse() {
        Struct protoPayload = Struct.newBuilder()
                .putFields("orderId", Value.newBuilder().setNumberValue(1).build())
                .build();

        com.eventprocessing.ingest.grpc.EventRequest request =
                com.eventprocessing.ingest.grpc.EventRequest.newBuilder()
                        .setType("order.created")
                        .setSource("test")
                        .setPayload(protoPayload)
                        .build();

        AtomicReference<EventResponse> result = new AtomicReference<>();
        StreamObserver<EventResponse> observer = new StreamObserver<>() {
            @Override public void onNext(EventResponse value) { result.set(value); }
            @Override public void onError(Throwable t) { throw new RuntimeException(t); }
            @Override public void onCompleted() {}
        };

        grpcService.submitEvent(request, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getType()).isEqualTo("order.created");
        assertThat(result.get().getSource()).isEqualTo("test");
        assertThat(result.get().getStatus()).isEqualTo("RECEIVED");
        assertThat(result.get().getId()).startsWith("evt_");

        EventRequest capturedRequest = eventProducer.submittedRequests().getFirst();
        assertThat(capturedRequest.type()).isEqualTo("order.created");
        assertThat(capturedRequest.source()).isEqualTo("test");

        JsonNode capturedPayload = capturedRequest.payload();
        assertThat(capturedPayload.has("orderId")).isTrue();
        assertThat(capturedPayload.get("orderId").doubleValue()).isEqualTo(1.0);
        assertThat(capturedPayload.has("fields")).isFalse();
    }

    @Test
    void submitBatchReturnsMultipleResponses() {
        Struct protoPayload = Struct.newBuilder()
                .putFields("x", Value.newBuilder().setNumberValue(1).build())
                .build();

        com.eventprocessing.ingest.grpc.EventRequest req =
                com.eventprocessing.ingest.grpc.EventRequest.newBuilder()
                        .setType("test.event")
                        .setSource("src")
                        .setPayload(protoPayload)
                        .build();

        BatchEventRequest batchRequest = BatchEventRequest.newBuilder()
                .addEvents(req)
                .addEvents(req)
                .addEvents(req)
                .build();

        AtomicReference<BatchEventResponse> result = new AtomicReference<>();
        StreamObserver<BatchEventResponse> observer = new StreamObserver<>() {
            @Override public void onNext(BatchEventResponse value) { result.set(value); }
            @Override public void onError(Throwable t) { throw new RuntimeException(t); }
            @Override public void onCompleted() {}
        };

        grpcService.submitBatch(batchRequest, observer);

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getEventsCount()).isEqualTo(3);
        assertThat(eventProducer.submittedRequests()).hasSize(3);
    }

    @Test
    void submitEventReturnsInvalidArgumentForValidationFailure() {
        com.eventprocessing.ingest.grpc.EventRequest request =
                com.eventprocessing.ingest.grpc.EventRequest.newBuilder()
                        .setSource("test")
                        .setPayload(Struct.newBuilder().build())
                        .build();

        AtomicReference<Throwable> error = new AtomicReference<>();
        StreamObserver<EventResponse> observer = new StreamObserver<>() {
            @Override public void onNext(EventResponse value) {}
            @Override public void onError(Throwable t) { error.set(t); }
            @Override public void onCompleted() {}
        };

        grpcService.submitEvent(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void submitEventReturnsUnavailableForSubmissionFailure() {
        eventProducer.throwOnSubmit(new EventSubmissionException("Failed to submit event to Kafka", new RuntimeException("boom")));

        Struct protoPayload = Struct.newBuilder()
                .putFields("orderId", Value.newBuilder().setNumberValue(1).build())
                .build();

        com.eventprocessing.ingest.grpc.EventRequest request =
                com.eventprocessing.ingest.grpc.EventRequest.newBuilder()
                        .setType("order.created")
                        .setSource("test")
                        .setPayload(protoPayload)
                        .build();

        AtomicReference<Throwable> error = new AtomicReference<>();
        StreamObserver<EventResponse> observer = new StreamObserver<>() {
            @Override public void onNext(EventResponse value) {}
            @Override public void onError(Throwable t) { error.set(t); }
            @Override public void onCompleted() {}
        };

        grpcService.submitEvent(request, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getDescription())
                .isEqualTo("Event could not be submitted at this time");
    }

    @Test
    void submitBatchReturnsInvalidArgumentWhenBatchTooLarge() {
        Struct protoPayload = Struct.newBuilder()
                .putFields("x", Value.newBuilder().setNumberValue(1).build())
                .build();

        com.eventprocessing.ingest.grpc.EventRequest req =
                com.eventprocessing.ingest.grpc.EventRequest.newBuilder()
                        .setType("test.event")
                        .setSource("src")
                        .setPayload(protoPayload)
                        .build();

        BatchEventRequest batchRequest = BatchEventRequest.newBuilder()
                .addEvents(req)
                .addEvents(req)
                .addEvents(req)
                .addEvents(req)
                .build();

        AtomicReference<Throwable> error = new AtomicReference<>();
        StreamObserver<BatchEventResponse> observer = new StreamObserver<>() {
            @Override public void onNext(BatchEventResponse value) {}
            @Override public void onError(Throwable t) { error.set(t); }
            @Override public void onCompleted() {}
        };

        grpcService.submitBatch(batchRequest, observer);

        assertThat(error.get()).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) error.get()).getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    private com.eventprocessing.ingest.config.IngestProperties ingestProperties(int maxBatchSize) {
        com.eventprocessing.ingest.config.IngestProperties properties =
                new com.eventprocessing.ingest.config.IngestProperties();
        properties.setMaxBatchSize(maxBatchSize);
        return properties;
    }

    private static final class CapturingEventProducer implements EventSubmitter {

        private final List<EventRequest> submittedRequests = new ArrayList<>();
        private RuntimeException submitException;

        @Override
        public Event submit(EventRequest request) {
            if (submitException != null) {
                throw submitException;
            }

            submittedRequests.add(request);
            return new Event(request.type(), request.source(), request.payload());
        }

        private List<EventRequest> submittedRequests() {
            return submittedRequests;
        }

        private void throwOnSubmit(RuntimeException exception) {
            this.submitException = exception;
        }
    }
}
