# Event Processing

Declarative event transformation and routing platform built on Apache Kafka. Define field-level mappings between source and destination topics through a visual mapper UI, CLI, or REST API. The platform reads events, applies transforms, and writes results. No custom consumer code needed.

Includes a React-based visual field mapper with drag-and-drop connections, a Picocli command-line interface, REST and gRPC ingestion, pipeline versioning with active-passive deployment, and Kafka Streams processing with dead letter handling.

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

Event storage (runs in parallel):
    [all Kafka topics]
        |
        | event-store consumes every topic
        v
    [event-store]
        |
        | deserializes, deduplicates by ID, persists
        v
    [PostgreSQL] (stored_events table, JSONB payload)
        |
        | event-search reads
        v
    [event-search API]
        |
        | GET /api/events?type=order.created&source=test&from=...&to=...
        v
    Query results (paginated, filterable by type, source, status, time range)
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
├── event-store/              Event persistence from Kafka to PostgreSQL (Java, Maven)
├── event-search/             Query API for stored events (Java, Maven)
├── event-detect/             Anomaly detection (Java, Maven)
├── event-cli/                Command-line interface (Java, Picocli)
└── event-mapper-ui/          Visual field mapper (React, Vite)
```

| Module | Type | Port | Description |
|--------|------|------|-------------|
| event-common | Library | n/a | Event model, field mapping model, type converters, serialization |
| event-ingest | Service | 8090, 9190 | Accepts events via REST and gRPC, validates, publishes to Kafka |
| event-engine | Service | n/a | Consumes from source topics, applies field mappings, produces to destination topics. One instance per pipeline. |
| event-admin | Service | 8091 | Pipeline CRUD, versioning, deployment status, dead letter inspection |
| event-store | Service | n/a | Kafka consumer, persists all events to PostgreSQL with JSONB. Deduplicates by event ID. |
| event-search | Service | 8092 | REST API for querying stored events by type, source, status, time range |
| event-detect | Service | 8093 | Anomaly detection: statistical baselines, schema drift, embeddings, LLM analysis |
| event-cli | CLI | n/a | Command-line interface for managing the platform (Picocli) |
| event-mapper-ui | UI | 3070 | Visual field mapper with drag-and-drop, schema discovery, live preview |

## API

### Ingest (REST port 8090, gRPC port 9190)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/events` | Submit a single event |
| POST | `/api/events/batch` | Submit multiple events (max batch size configurable) |
| GET | `/api/health` | Service health |

gRPC service `EventService` available on port 9190 with `SubmitEvent` and `SubmitBatch` RPCs. Proto definition at `event-ingest/src/main/proto/event_service.proto`.

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

### Anomaly Detection (port 8093)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/alerts` | List alerts (filter by detectorType, severity, eventType, resolved) |
| GET | `/api/alerts/{id}` | Get alert details |
| POST | `/api/alerts/{id}/resolve` | Mark alert as resolved |
| GET | `/api/alerts/stats` | Alert counts (total, open, resolved) |

Swagger UI available at `/swagger-ui.html` on ingest, admin, search, and detect services.

### Search (port 8092)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | Search events with filters (type, source, status, from, to) |
| GET | `/api/events/{id}` | Get event by ID |
| GET | `/api/events/types` | List all event types |
| GET | `/api/events/sources` | List all event sources |
| GET | `/api/events/stats` | Event counts by type and source |

## CLI

Command-line interface for managing the platform. Built with Picocli, packaged as a single executable jar.

```bash
# Build the CLI
./mvnw package -pl event-cli -q

# Run it
java -jar event-cli/target/event-cli-*.jar [command]
```

### Commands

```bash
# Platform status
ep status

# Pipeline management
ep pipelines list
ep pipelines get orders-to-warehouse
ep pipelines create --name my-pipeline --source events.raw --dest output.topic
ep pipelines deploy orders-to-warehouse --version 1
ep pipelines pause orders-to-warehouse
ep pipelines resume orders-to-warehouse
ep pipelines draft orders-to-warehouse
ep pipelines delete orders-to-warehouse
ep pipelines versions orders-to-warehouse
ep pipelines test orders-to-warehouse '{"orderId":1,"total":"49.99"}'

# Topic operations
ep topics list
ep topics schema events.raw
ep topics sample events.raw --count 5

# Event submission
ep events send --type order.created --source test --payload '{"orderId":1}'
ep events send --type order.created --source test --file event.json
```

### Custom endpoints

```bash
ep --admin-url http://remote:8091 --ingest-url http://remote:8090 status
```

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
| CLI | Picocli | 4.7.6 |
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
| PostgreSQL (admin) | 5877 | Pipeline definitions |
| PostgreSQL (store) | 5878 | Event storage |
| event-ingest (REST) | 8090 | Event submission |
| event-ingest (gRPC) | 9190 | Event submission |
| event-engine | n/a | Transform processing |
| event-admin | 8091 | Control center |
| event-store | n/a | Kafka consumer, persists events |
| event-search | 8092 | Event query API |
| event-detect | 8093 | Anomaly detection |
| event-mapper-ui | 3070 | Visual mapper |

## Quick Start

```bash
git clone https://github.com/psandis/event-processing.git
cd event-processing
docker compose up --build -d    # starts Kafka, PostgreSQL, ingest, admin
./mvnw package -pl event-cli -q # build the CLI
```

### 1. Create a pipeline

Using the CLI:

```bash
java -jar event-cli/target/event-cli-*.jar pipelines create \
  --name orders-to-warehouse \
  --source events.raw \
  --dest warehouse.fulfillment
```

Or using curl:

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

Using the CLI:

```bash
java -jar event-cli/target/event-cli-*.jar events send \
  --type order.created --source order-service \
  --payload '{"orderId":5001,"total":"129.99","currency":"EUR","customer":{"city":"Helsinki","country":"fi"}}'
```

Or using curl:

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
./start.sh test             # 93 tests across all modules
```

## Testing

93 tests across all modules. No Kafka or Docker required to run them.

| Module | Tests | Coverage |
|--------|-------|----------|
| event-common | 14 | Type converters (12 conversions), event serialization (2) |
| event-ingest | 15 | REST endpoints (7), gRPC submit and batch (5), Kafka send behavior (2), Struct conversion (1) |
| event-engine | 23 | Mapping executor (12 including flatten), schema discovery (4), transform topology with dead letter (3), pipeline loader (4) |
| event-admin | 10 | Pipeline CRUD with versioning, deploy, pause/resume, mapping test |
| event-store | 3 | Event consumption, deduplication, invalid JSON handling |
| event-search | 7 | Search with filters, get by ID, types, sources, stats, 404 handling |
| event-detect | 6 | Alert CRUD, resolve, schema drift detection logic |
| event-cli | 15 | Command parsing, help output, required options, custom URLs, defaults |

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
Event ingestion (REST + gRPC), pipeline definition storage, transform engine with Kafka Streams, dead letter handling. CLI with Picocli.

### Phase 2 (complete)
Pipeline versioning (DRAFT/ACTIVE/PAUSED/DEPLOYING). Visual field mapper UI (React, xyflow). Event store (Kafka to PostgreSQL). Event search API (query by type, source, status, time range). 93 tests.

### Phase 3 (current: anomaly detection)

AI-powered anomaly detection across event streams. Four detection layers, each independent, all feeding into a unified alert system.

#### Detection Layers

**1. Statistical baselines**
Track event volume per type and source over sliding time windows. Detect spikes, drops, and unusual patterns in throughput. No AI needed, pure math. Configurable thresholds and sensitivity.

Example: "order.created events from payment-service dropped 90% in the last 5 minutes"

**2. Schema drift detection**
Compare incoming event structures against discovered schemas. Detect when fields appear, disappear, or change type. Uses the existing SchemaDiscovery engine.

Example: "order.created events from checkout-v2 now include a 'discount' field that was never seen before"

**3. Content anomaly detection (vector embeddings)**
Generate vector embeddings of event payloads using an LLM API (Claude or OpenAI). Store embeddings in PostgreSQL with Pgvector. Compute cosine distance from the cluster center for each event type. Events far from the center are flagged as anomalous.

Architecture:
```
event-store (PostgreSQL)
    |
    | event-detect reads stored events
    v
[event-detect]
    |
    | 1. batch events by type
    | 2. generate embeddings via LLM API
    | 3. store vectors in Pgvector
    | 4. compute distance from centroid
    | 5. flag outliers above threshold
    |
    v
[anomaly alerts] --> admin API --> mapper UI notifications
```

Example: "This order.created event has unusual field values. The 'total' of 999999.99 is 47x higher than the average for this event type."

**4. LLM analysis**
For flagged anomalies, send the event payload and historical context to an LLM for natural language analysis. Get a human-readable explanation of what's unusual and potential root causes.

Example: "This event contains a negative stock quantity (-5) which has never occurred before. This may indicate a race condition in the inventory service."

#### Module structure

```
event-detect/
├── detector/
│   ├── StatisticalDetector.java      Volume baselines, spike/drop detection
│   ├── SchemaDriftDetector.java      Field presence and type changes
│   ├── EmbeddingDetector.java        Vector similarity with Pgvector
│   └── LlmAnalyzer.java             Natural language anomaly explanation
├── embedding/
│   ├── EmbeddingService.java         LLM API client for generating vectors
│   └── VectorStore.java             Pgvector read/write operations
├── alert/
│   ├── AnomalyAlert.java            Alert model
│   └── AlertService.java            Stores and publishes alerts
└── config/
    └── DetectProperties.java         Thresholds, API keys, intervals
```

#### Tech additions

| Component | Technology |
|-----------|-----------|
| Vector storage | Pgvector (PostgreSQL extension) |
| Embeddings | Claude API or OpenAI API |
| Scheduling | Spring @Scheduled for periodic detection runs |
| Alerts | Kafka topic (events.anomalies) + REST API |

#### Configuration

Statistical and schema drift detection run automatically with no setup. Embedding and LLM detection require an API key.

```yaml
# application.yml or environment variables
detect:
  statistical:
    window-minutes: 5          # time window for volume analysis
    spike-threshold: 3.0       # z-score above this triggers spike alert
    drop-threshold: 0.3        # volume below this fraction of average triggers drop alert
    minimum-events: 10         # ignore types with fewer events than this
    check-interval-ms: 30000   # how often to check (30 seconds)
  schema:
    sample-size: 20            # number of recent events to analyze per type
    check-interval-ms: 60000   # how often to check (60 seconds)
  embedding:
    enabled: false             # set to true to enable AI detection
    api-key: ${EMBEDDING_API_KEY}
    model: claude-haiku-4-5-20251001
    anomaly-threshold: 0.85    # cosine distance above this is anomalous
    check-interval-ms: 300000  # how often to check (5 minutes)
```

To enable AI-powered detection:

```bash
# Set environment variables before starting
export EMBEDDING_API_KEY=your-anthropic-api-key
export EMBEDDING_API_URL=https://api.anthropic.com

# Or pass via Docker
docker run -e EMBEDDING_API_KEY=... -e detect.embedding.enabled=true event-detect
```

#### Alert severity levels

| Severity | Meaning |
|----------|---------|
| `HIGH` | Volume spike (z-score above threshold). Immediate attention needed. |
| `MEDIUM` | Volume drop or schema change. Investigate when possible. |
| `LOW` | Minor anomaly detected by embedding similarity. Informational. |

#### Using the alerts API

```bash
# List open alerts
curl http://localhost:8093/api/alerts?resolved=false

# Get alert details
curl http://localhost:8093/api/alerts/1

# Resolve an alert
curl -X POST http://localhost:8093/api/alerts/1/resolve

# Alert stats
curl http://localhost:8093/api/alerts/stats
```

## License

[MIT](LICENSE)
