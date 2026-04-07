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
├── Dockerfile                Multi-stage build (shared across Java services)
├── event-common/             Shared library (Java, Maven)
├── event-ingest/             REST + gRPC event submission (Java, Maven)
├── event-engine/             Kafka Streams transform execution (Java, Maven)
├── event-admin/              Control center API (Java, Maven)
└── event-mapper-ui/          Visual field mapper (React, Vite)
```

| Module | Type | Port | Description |
|--------|------|------|-------------|
| event-common | Library | n/a | Event model, field mapping model, type converters, serialization |
| event-ingest | Service | 8090, 9090 | Accepts events via REST and gRPC, validates, publishes to Kafka |
| event-engine | Service | n/a | Consumes from source topics, applies field mappings, produces to destination topics. One instance per pipeline. |
| event-admin | Service | 8091 | Pipeline CRUD, versioning, deployment status, dead letter inspection |
| event-mapper-ui | UI | 3000 | Visual field mapper with drag-and-drop, schema discovery, live preview |

## API

### Ingest (REST port 8090, gRPC port 9090)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/events` | Submit a single event |
| POST | `/api/events/batch` | Submit multiple events (max batch size configurable) |
| GET | `/api/health` | Service health |

gRPC service `EventService` available on port 9090 with `SubmitEvent` and `SubmitBatch` RPCs. Proto definition at `event-ingest/src/main/proto/event_service.proto`.

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

## Pipeline Versioning

Each pipeline supports multiple versions. Only one version can be active at a time.

| State | Meaning |
|-------|---------|
| `DRAFT` | Being designed in the mapper UI. Not deployed, no engine running. |
| `ACTIVE` | Deployed. Engine instance running, processing events. |
| `PAUSED` | Engine stopped. Can be resumed with the same configuration. |
| `DEPLOYING` | New version being rolled out via active-passive switch. |

Creating or editing a pipeline in the mapper UI creates a DRAFT version. Deploying promotes it to ACTIVE.

## Active-Passive Deployment

Changing a live pipeline uses an active-passive pattern to avoid downtime or event loss.

1. User opens an ACTIVE pipeline in the mapper UI
2. UI warns: "This pipeline is currently processing events. Changes will be deployed as a new version."
3. User edits field mappings, tests with sample data (working on a DRAFT version)
4. User clicks Deploy
5. System starts a new engine instance (passive) with the updated mappings
6. Passive engine catches up (consumer lag reaches zero)
7. Traffic switches to the new engine
8. Old engine stops
9. Old version is archived, new version becomes ACTIVE

Rollback: start the old version's engine instance. The archived config is preserved.

Docker handles the engine lifecycle. Each engine instance is a container running one pipeline version.

## Visual Mapper UI

React application for building field mappings visually. Connects to the admin API.

**Stack:** React 19, Vite, @xyflow/react v12, Zustand, Tailwind CSS v4

**Features:**
- Connect a source Kafka topic. Schema is discovered automatically from sample events.
- Source fields displayed on the left, destination fields on the right.
- Draw connections between fields by dragging.
- Configure type conversion on each connection (click the line).
- Set defaults, formatting, exclusions per field.
- Live preview: real sample events flow through the mappings, output shown side by side.
- Test runner: validate mappings before deploying.
- Pipeline state indicator (DRAFT, ACTIVE, PAUSED, DEPLOYING).
- Warning when editing an active pipeline.

## Tech Stack

### Backend

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java (OpenJDK Temurin) | 21 |
| Framework | Spring Boot | 3.5.0 |
| Messaging | Spring Kafka | managed |
| Streaming | Apache Kafka (Confluent, KRaft mode) | 7.7.1 |
| RPC | gRPC, Protobuf | 1.62.2 |
| Persistence | Spring Data JPA / Hibernate | 6.x |
| Database | PostgreSQL | 17 |
| JSON storage | PostgreSQL JSONB | n/a |
| Migrations | Flyway | managed |
| Validation | Jakarta Bean Validation | 3.x |
| API docs | SpringDoc OpenAPI | 2.8.6 |
| Testing | JUnit 5, MockMvc | 5.12+ |
| Build | Maven (wrapper included) | 3.9+ |

### Frontend (mapper UI)

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | React | 19 |
| Build tool | Vite | latest |
| Visual editor | @xyflow/react | 12 |
| State management | Zustand | latest |
| Styling | Tailwind CSS | 4 |

### Infrastructure

| Component | Technology | Version |
|-----------|-----------|---------|
| Containers | Docker, Docker Compose | 24.0+ |

## Services

| Service | Port | |
|---------|------|-|
| Kafka (KRaft) | 9492 | Event streaming |
| PostgreSQL | 5877 | Pipeline definitions |
| event-ingest (REST) | 8090 | Event submission |
| event-ingest (gRPC) | 9190 | Event submission |
| event-engine | n/a | Transform processing |
| event-admin | 8091 | Control center |
| event-mapper-ui | 3070 | Visual mapper |

## Quick Start

```bash
git clone https://github.com/psandis/event-processing.git
cd event-processing
docker compose up --build -d    # starts Kafka, PostgreSQL, ingest, admin
```

### 1. Create a pipeline

Define how events should be transformed from source to destination.

```bash
curl -X POST http://localhost:8091/api/pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "orders-to-warehouse",
    "description": "Transform order events for warehouse fulfillment",
    "sourceTopic": "events.raw",
    "destinationTopic": "warehouse.fulfillment",
    "fieldMappings": [
      { "sourceField": "orderId", "destinationField": "fulfillmentId" },
      { "sourceField": "total", "destinationField": "amount", "conversion": "TO_DOUBLE" },
      { "sourceField": "currency", "destinationField": "currency" },
      { "sourceField": "customer.city", "destinationField": "shipping.city" },
      { "sourceField": "customer.country", "destinationField": "shipping.countryCode", "conversion": "TO_UPPER" },
      { "sourceField": "debug", "destinationField": "debug", "excluded": true }
    ]
  }'
```

### 2. Start an engine instance for the pipeline

Each engine instance processes one pipeline. Start it with the pipeline name.

```bash
PIPELINE_NAME=orders-to-warehouse docker compose run -d event-engine
```

The engine fetches the pipeline definition from admin, builds a Kafka Streams topology, and starts consuming from the source topic.

### 3. Submit an event

```bash
curl -X POST http://localhost:8090/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "type": "order.created",
    "source": "order-service",
    "payload": {
      "orderId": 5001,
      "total": "129.99",
      "currency": "EUR",
      "customer": { "city": "Helsinki", "country": "fi" },
      "debug": true
    }
  }'
```

### 4. Verify the transformed event

```bash
docker exec event-processing-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic warehouse.fulfillment \
  --from-beginning --timeout-ms 5000
```

The transformed output:

```json
{
  "id": "evt_...",
  "type": "order.created",
  "source": "order-service",
  "status": "PROCESSED",
  "payload": {
    "fulfillmentId": 5001,
    "amount": 129.99,
    "currency": "EUR",
    "shipping": {
      "city": "Helsinki",
      "countryCode": "FI"
    }
  }
}
```

The `debug` field is excluded. `total` (string) became `amount` (double). `customer.country` ("fi") became `shipping.countryCode` ("FI"). Nested destination structure created automatically.

### Pipeline lifecycle

```bash
# Pause a pipeline (stops processing, engine keeps running)
curl -X POST http://localhost:8091/api/pipelines/orders-to-warehouse/pause

# Resume
curl -X POST http://localhost:8091/api/pipelines/orders-to-warehouse/resume

# Update pipeline (stop engine, update, start new engine)
docker stop <engine-container-id>
curl -X PUT http://localhost:8091/api/pipelines/orders-to-warehouse ...
PIPELINE_NAME=orders-to-warehouse docker compose run -d event-engine

# Delete pipeline
docker stop <engine-container-id>
curl -X DELETE http://localhost:8091/api/pipelines/orders-to-warehouse
```

### Run tests

```bash
./start.sh test             # 58 tests across all modules
```

## Testing

58 tests across all modules. No Kafka or Docker required to run them.

| Module | Tests | Coverage |
|--------|-------|----------|
| event-common | 14 | Type converters (12 conversions), event serialization (2) |
| event-ingest | 15 | REST endpoints (7), gRPC submit and batch (5), Kafka send behavior (2), Struct conversion (1) |
| event-engine | 23 | Mapping executor (12 including flatten), schema discovery (4), transform topology with dead letter (3), pipeline loader (4) |
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

### Phase 1 (complete)
Event ingestion (REST + gRPC), pipeline definition storage, transform engine with Kafka Streams, dead letter handling. 58 tests.

### Phase 2 (next: visual mapper UI)
Pipeline versioning (DRAFT/ACTIVE/PAUSED/DEPLOYING states). Active-passive deployment for live pipeline changes. React-based visual field mapper with schema discovery, drag-and-drop connections, type conversion config, and live preview.

### Phase 3
Event store (persist processed events to PostgreSQL), search API (query by type, source, time range).

### Phase 4
Anomaly detection module with Pgvector embeddings and pattern matching.

## License

[MIT](LICENSE)
