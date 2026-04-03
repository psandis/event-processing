# Event Processing

Declarative event transformation and routing platform built on Apache Kafka. Define field-level mappings between source and destination topics. The platform reads events, applies transforms, and writes results. No custom consumer code needed.

## Design Principles

1. **Configuration over code.** Transforms are defined declaratively as field mappings, not as compiled Java consumers. Adding a new transformation should never require a code change or redeployment.

2. **Events are immutable.** Once an event enters the platform, it is never modified in place. Transforms produce new events on destination topics. The original event stays untouched on the source topic.

3. **Services are stateless.** Each service can be restarted, scaled, or replaced without data loss. All state lives in Kafka (events) or PostgreSQL (pipeline definitions). No in-memory state that cannot be recovered.

4. **Fail-safe by default.** If a transform fails, the event goes to a dead letter topic with error details. Processing continues for other events. No single failure stops the pipeline.

5. **Hot reload.** Pipeline definitions can be created, updated, or deleted at runtime through the admin API. The engine picks up changes without restart.

6. **Language-agnostic boundaries.** The ingest API accepts standard JSON over HTTP. Any system in any language can produce or consume events. The platform does not impose client libraries or SDK requirements.

7. **Each service owns its responsibility.** Ingest only ingests. Engine only transforms. Admin only manages configuration. No service does two jobs.

## Architecture Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Messaging | Apache Kafka with KRaft | Industry standard for event streaming. KRaft removes Zookeeper dependency. |
| Stream processing | Spring Kafka consumer/producer | Simpler than Kafka Streams for per-pipeline dynamic topology. Pipelines are loaded at runtime, not compiled into a fixed topology. |
| Pipeline storage | PostgreSQL with JSONB | Field mappings are flexible JSON structures. JSONB allows indexing and querying without rigid schemas. |
| Event format | JSON | Universal, human-readable, supported by every language. No schema registry required for v1. |
| Transform model | Field-level source-to-destination mapping | Maps directly to the visual mapper UI planned for later. Each mapping is an independent unit that can be tested, displayed, and modified individually. |
| Module structure | Maven multi-module | Each service is independently deployable but shares the common library. Single build command for the full platform. |
| Error handling | Dead letter topic | Failed events are never dropped. They can be inspected and retried through the admin API. |
| Testing | Mock Kafka for unit tests, real Kafka for E2E | Unit tests run without infrastructure. Docker Compose provides real Kafka for integration and manual testing. |

## Data Flow

```
Producer (any language, any system)
    |
    | HTTP POST /api/events
    v
[event-ingest] ---- validates event ---- assigns ID + timestamp
    |
    | publishes to Kafka
    v
[events.raw] (Kafka topic)
    |
    | event-engine consumes
    v
[event-engine]
    |
    | 1. loads pipeline definitions from event-admin
    | 2. matches event type to pipeline source topic
    | 3. applies field mappings (rename, convert, mask, etc.)
    | 4. produces transformed event to destination topic
    |
    v
[destination topic] (e.g. analytics.orders, warehouse.fulfillment)
    |
    | any consumer picks it up
    v
Consumer (any language, any system)

On failure:
    [event-engine] --> [events.failed] (dead letter topic)
```

## Event Model

```json
{
  "id": "evt_8f14e45f-ceea-4d9b-8b56-34a1c4b5c5a2",
  "type": "order.created",
  "source": "order-service",
  "timestamp": "2026-04-03T10:30:00Z",
  "payload": {
    "orderId": 5001,
    "customerId": 42,
    "total": "129.99",
    "currency": "EUR",
    "items": [
      { "sku": "AUD-001", "name": "Headphones", "qty": 1 }
    ],
    "address": {
      "street": "Mannerheimintie 1",
      "city": "Helsinki",
      "country": "fi"
    }
  }
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | UUID | Auto-generated | Unique event identifier |
| `type` | String | Yes | Dot-notation event type |
| `source` | String | Yes | Originating system |
| `timestamp` | ISO 8601 | Auto-generated | When the event was received |
| `payload` | JSON object | Yes | Event data, any structure |
| `metadata` | JSON object | No | Correlation IDs, versioning, tracing |

## Pipeline Definition

A pipeline defines how events transform from a source topic to a destination topic through field-level mappings.

```json
{
  "name": "orders-to-warehouse",
  "description": "Map order events to warehouse fulfillment format",
  "sourceTopic": "events.order.created",
  "destinationTopic": "warehouse.fulfillment",
  "enabled": true,
  "fieldMappings": [
    { "sourceField": "orderId", "destinationField": "fulfillmentId" },
    { "sourceField": "total", "destinationField": "amount", "conversion": "TO_DOUBLE" },
    { "sourceField": "address.city", "destinationField": "shipping.city" },
    { "sourceField": "address.country", "destinationField": "shipping.countryCode", "conversion": "TO_UPPER" },
    { "sourceField": "debug", "destinationField": "debug", "excluded": true }
  ],
  "errorHandling": {
    "retries": 3,
    "backoffMs": 1000,
    "deadLetterTopic": "events.failed.warehouse"
  }
}
```

Each field mapping specifies:

| Property | Description |
|----------|-------------|
| `sourceField` | JSON path in the source event (supports dot notation for nested fields) |
| `destinationField` | JSON path in the output event (creates nested structure automatically) |
| `conversion` | Optional type conversion to apply |
| `defaultValue` | Value to use if source field is missing. Supports `${now}` and `${uuid}`. |
| `excluded` | If true, field is explicitly dropped from output |

## Type Conversions

| Conversion | From | To | Example |
|------------|------|----|---------|
| `TO_STRING` | any | string | `123` to `"123"` |
| `TO_INTEGER` | string/number | integer | `"42"` to `42` |
| `TO_LONG` | string/number | long | `"9999999999"` to `9999999999` |
| `TO_DOUBLE` | string/number | double | `"49.99"` to `49.99` |
| `TO_BOOLEAN` | string | boolean | `"true"` to `true` |
| `TO_TIMESTAMP` | string | ISO 8601 | Date string validated and normalized |
| `TO_UPPER` | string | string | `"hello"` to `"HELLO"` |
| `TO_LOWER` | string | string | `"HELLO"` to `"hello"` |
| `MASK` | string | string | `"secret123"` to `"s*******3"` |
| `FLATTEN` | object | fields | `{a: {b: 1}}` to `{b: 1}` |

## Modules

```
event-processing/
├── pom.xml                   Maven aggregator
├── docker-compose.yml        Kafka (KRaft), PostgreSQL, all services
├── start.sh                  Build, start, stop, test
├── Dockerfile                Multi-stage build (shared across services)
├── event-common/             Shared library (not a service)
├── event-ingest/             REST API for event submission
├── event-engine/             Kafka consumer/producer, transform execution
└── event-admin/              Control center API for pipeline management
```

| Module | Type | Port | Description |
|--------|------|------|-------------|
| event-common | Library | n/a | Event model, field mapping model, type converters, serialization |
| event-ingest | Service | 8090 | Accepts events via REST, validates, publishes to `events.raw` |
| event-engine | Service | n/a | Consumes from source topics, applies field mappings, produces to destination topics |
| event-admin | Service | 8091 | Pipeline CRUD, dead letter inspection, service health |

## API

### Ingest (port 8090)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/events` | Submit a single event |
| POST | `/api/events/batch` | Submit multiple events |
| GET | `/api/health` | Service health |

### Admin / Control Center (port 8091)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pipelines` | List all pipelines |
| GET | `/api/pipelines/{name}` | Get pipeline by name |
| POST | `/api/pipelines` | Create pipeline |
| PUT | `/api/pipelines/{name}` | Update pipeline |
| DELETE | `/api/pipelines/{name}` | Delete pipeline |
| POST | `/api/pipelines/{name}/pause` | Pause pipeline |
| POST | `/api/pipelines/{name}/resume` | Resume pipeline |
| GET | `/api/status` | Platform health |

Swagger UI available at `/swagger-ui.html` on both services.

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java (OpenJDK Temurin) | 21 |
| Framework | Spring Boot | 3.5.0 |
| Messaging | Spring Kafka | managed |
| Streaming | Apache Kafka (Confluent, KRaft mode) | 7.7.1 |
| Persistence | Spring Data JPA / Hibernate | 6.x |
| Database | PostgreSQL | 17 |
| JSON storage | PostgreSQL JSONB | n/a |
| Migrations | Flyway | managed |
| Validation | Jakarta Bean Validation | 3.x |
| API docs | SpringDoc OpenAPI | 2.8.6 |
| Testing | JUnit 5, MockMvc, EmbeddedKafka | 5.12+ |
| Containers | Docker, Docker Compose | 24.0+ |
| Build | Maven (wrapper included) | 3.9+ |

## Services

| Service | Port | |
|---------|------|-|
| Kafka (KRaft) | 9092 | Event streaming |
| PostgreSQL | 5877 | Pipeline definitions |
| event-ingest | 8090 | Event submission |
| event-engine | n/a | Transform processing |
| event-admin | 8091 | Control center |

## Quick Start

```bash
git clone https://github.com/psandis/event-processing.git
cd event-processing
./start.sh docker           # starts Kafka, PostgreSQL, and all services
```

### Submit a test event

```bash
curl -X POST http://localhost:8090/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "type": "order.created",
    "source": "test",
    "payload": {
      "orderId": 1,
      "total": "49.99",
      "currency": "EUR"
    }
  }'
```

### Create a pipeline

```bash
curl -X POST http://localhost:8091/api/pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "orders-to-analytics",
    "sourceTopic": "events.raw",
    "destinationTopic": "analytics.orders",
    "fieldMappings": [
      { "sourceField": "orderId", "destinationField": "id" },
      { "sourceField": "total", "destinationField": "amount", "conversion": "TO_DOUBLE" },
      { "sourceField": "currency", "destinationField": "currency" }
    ]
  }'
```

### Run tests

```bash
./start.sh test             # 39 tests across all modules
```

## Testing

39 tests across all modules. No Kafka or Docker required to run them.

| Module | Tests | Coverage |
|--------|-------|----------|
| event-common | 14 | Type converters (12 conversions), event serialization (2) |
| event-ingest | 5 | REST endpoints, validation, batch submission |
| event-engine | 14 | Mapping executor (10 transform scenarios), schema discovery (4) |
| event-admin | 6 | Pipeline CRUD, pause/resume, status |

## Coding Conventions

- Java 21 features used where appropriate (records, text blocks, pattern matching)
- Constructor injection, no field injection
- `final` on service dependencies
- DTOs as Java records
- One responsibility per class
- Logging with SLF4J, structured messages with context (event ID, pipeline name)
- All public API endpoints documented with OpenAPI annotations
- Tests named by behavior, not method name

## Roadmap

### Phase 1 (current)
Event ingestion, pipeline definition storage, transform engine core.

### Phase 2
Event store (persist processed events to PostgreSQL), search API (query by type, source, time range).

### Phase 3
Anomaly detection module with Pgvector embeddings and pattern matching.

### Phase 4
Visual mapper UI. Schema discovery visualization, drag-and-drop field mapping, live preview with real event data.

## License

[MIT](LICENSE)
