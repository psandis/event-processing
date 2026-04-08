package com.eventprocessing.ingest.grpc;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;
import com.eventprocessing.ingest.service.BatchSizeExceededException;
import com.eventprocessing.ingest.service.EventSubmissionException;
import com.eventprocessing.ingest.service.EventSubmitter;
import com.eventprocessing.ingest.service.IngestRequestValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.validation.ConstraintViolationException;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class GrpcEventService extends EventServiceGrpc.EventServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcEventService.class);

    private final EventSubmitter eventProducer;
    private final IngestRequestValidator requestValidator;
    private final ObjectMapper objectMapper;

    public GrpcEventService(
            EventSubmitter eventProducer,
            IngestRequestValidator requestValidator,
            ObjectMapper objectMapper
    ) {
        this.eventProducer = eventProducer;
        this.requestValidator = requestValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void submitEvent(com.eventprocessing.ingest.grpc.EventRequest request,
                            StreamObserver<EventResponse> responseObserver) {
        try {
            EventRequest eventRequest = toEventRequest(request);
            requestValidator.validateEventRequest(eventRequest);
            Event event = eventProducer.submit(eventRequest);

            responseObserver.onNext(toResponse(event));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapException("submitEvent", e));
        }
    }

    @Override
    public void submitBatch(BatchEventRequest request,
                            StreamObserver<BatchEventResponse> responseObserver) {
        try {
            requestValidator.validateBatchSize(request.getEventsCount());
            BatchEventResponse.Builder builder = BatchEventResponse.newBuilder();

            for (com.eventprocessing.ingest.grpc.EventRequest eventRequest : request.getEventsList()) {
                EventRequest mapped = toEventRequest(eventRequest);
                requestValidator.validateEventRequest(mapped);
                Event event = eventProducer.submit(mapped);
                builder.addEvents(toResponse(event));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(mapException("submitBatch", e));
        }
    }

    private EventRequest toEventRequest(com.eventprocessing.ingest.grpc.EventRequest grpcRequest) {
        JsonNode payload = structToJsonNode(grpcRequest.getPayload());
        JsonNode metadata = grpcRequest.hasMetadata() ? structToJsonNode(grpcRequest.getMetadata()) : null;
        return new EventRequest(
                grpcRequest.getType(),
                grpcRequest.getSource(),
                payload,
                metadata
        );
    }

    private JsonNode structToJsonNode(Struct struct) {
        ObjectNode node = objectMapper.createObjectNode();
        struct.getFieldsMap().forEach((key, value) -> node.set(key, valueToJsonNode(value)));
        return node;
    }

    private JsonNode valueToJsonNode(Value value) {
        return switch (value.getKindCase()) {
            case NUMBER_VALUE -> objectMapper.getNodeFactory().numberNode(value.getNumberValue());
            case STRING_VALUE -> objectMapper.getNodeFactory().textNode(value.getStringValue());
            case BOOL_VALUE -> objectMapper.getNodeFactory().booleanNode(value.getBoolValue());
            case STRUCT_VALUE -> structToJsonNode(value.getStructValue());
            case LIST_VALUE -> {
                ArrayNode arr = objectMapper.createArrayNode();
                value.getListValue().getValuesList().forEach(v -> arr.add(valueToJsonNode(v)));
                yield arr;
            }
            case NULL_VALUE -> objectMapper.getNodeFactory().nullNode();
            default -> objectMapper.getNodeFactory().nullNode();
        };
    }

    private StatusRuntimeException mapException(String operation, Exception exception) {
        if (exception instanceof ConstraintViolationException) {
            return Status.INVALID_ARGUMENT
                    .withDescription("Validation failed")
                    .asRuntimeException();
        }
        if (exception instanceof BatchSizeExceededException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException();
        }
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException();
        }
        if (exception instanceof EventSubmissionException) {
            log.error("gRPC {} failed while submitting to Kafka", operation, exception);
            return Status.UNAVAILABLE
                    .withDescription("Event could not be submitted at this time")
                    .asRuntimeException();
        }

        log.error("gRPC {} failed unexpectedly", operation, exception);
        return Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException();
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.newBuilder()
                .setId(event.getId())
                .setType(event.getType())
                .setSource(event.getSource())
                .setTimestamp(event.getTimestamp().toString())
                .setStatus(event.getStatus().name())
                .build();
    }
}
