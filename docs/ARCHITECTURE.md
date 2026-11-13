# Architecture & System Design

## 1. Architectural Style & Design Principles

The FreightFox Transporter Assignment application is engineered as a **Modular Monolith** adhering to **Clean Architecture** and **Domain-Driven Design (DDD)** principles.

### Deliberate Architectural Decisions
- **Single Process Modular Monolith**: Given the cohesive business boundary (transporter input ingestion and lane assignment optimization), microservices would introduce distributed latency, network unreliability, and unnecessary complexity.
- **Strict Decoupling of Domain & Optimization**: The optimization algorithm (`com.freightfox.optimizer`) is a pure Java library with **zero** dependencies on Spring, Hibernate, JPA, SQL, or web layers. It can be compiled and verified in complete isolation.
- **Strict Dependency Inversion**:
  ```
  Web / REST Controllers (API)
             │
             ▼
  Application Services (Orchestration & Transactions)
             │
      ┌──────┴──────────────────┐
      ▼                         ▼
  Domain & Optimizer        Persistence Repositories (JPA & PostgreSQL)
  (Pure Java)
  ```

---

## 2. Package & Module Structure

```
com.freightfox
├── FreightFoxApplication.java          # Spring Boot Main Entrypoint
│
├── config
│   └── OpenApiConfig.java              # OpenAPI 3 / Swagger UI Configuration
│
├── common
│   └── exception
│       ├── GlobalExceptionHandler.java # Centralized Exception-to-HTTP Mapper
│       ├── ErrorResponse.java          # Unified Error DTO
│       ├── InvalidDatasetException.java
│       ├── InfeasibleAssignmentException.java
│       └── NoActiveDatasetException.java
│
├── dataset                             # Ingestion Subsystem
│   ├── controller
│   │   └── DatasetInputController.java # POST /api/v1/transporters/input
│   ├── dto
│   │   ├── DatasetInputRequest.java
│   │   ├── DatasetInputResponse.java
│   │   ├── LaneInputDto.java
│   │   ├── TransporterInputDto.java
│   │   └── LaneQuoteInputDto.java
│   ├── entity
│   │   ├── DatasetSnapshotEntity.java  # Snapshot root entity
│   │   ├── LaneEntity.java             # Lane entity scoped to snapshot
│   │   ├── TransporterEntity.java      # Transporter entity scoped to snapshot
│   │   └── LaneQuoteEntity.java        # Quote entity scoped to lane & transporter
│   ├── repository
│   │   ├── DatasetSnapshotRepository.java
│   │   ├── LaneRepository.java
│   │   ├── TransporterRepository.java
│   │   └── LaneQuoteRepository.java
│   └── service
│       └── DatasetSnapshotService.java # Transactional snapshot ingestion & activation
│
├── assignment                          # Optimization & Assignment Subsystem
│   ├── controller
│   │   └── AssignmentController.java   # POST /api/v1/transporters/assignment
│   ├── dto
│   │   ├── AssignmentRequestDto.java
│   │   ├── AssignmentResponseDto.java
│   │   └── LaneAssignmentDto.java
│   └── service
│       └── TransporterAssignmentService.java # Isolated snapshot read & solver invocation
│
└── optimizer                           # Pure Java Optimization Engine (Zero Frameworks)
    ├── engine
    │   └── AssignmentOptimizer.java    # Bitmask DP over lane subsets
    └── model
        ├── Lane.java
        ├── Transporter.java
        ├── LaneQuote.java
        ├── LaneAssignment.java
        ├── OptimizationRequest.java
        └── OptimizationResult.java
```

---

## 3. Relational Persistence Model & Snapshot Isolation

### Database Schema (Flyway V1 Migration)

The database schema is fully normalized and enforced in PostgreSQL 17:

1. **`dataset_snapshot`**:
   - `id`: `BIGSERIAL PRIMARY KEY`
   - `active`: `BOOLEAN NOT NULL DEFAULT FALSE` (indexed for active snapshot retrieval)
   - `created_at`: `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`

2. **`lane`**:
   - `id`: `BIGSERIAL PRIMARY KEY`
   - `snapshot_id`: `BIGINT NOT NULL REFERENCES dataset_snapshot(id) ON DELETE CASCADE`
   - `lane_id`: `BIGINT NOT NULL` (client lane ID)
   - `origin`: `VARCHAR(255) NOT NULL`
   - `destination`: `VARCHAR(255) NOT NULL`
   - `UNIQUE(snapshot_id, lane_id)`

3. **`transporter`**:
   - `id`: `BIGSERIAL PRIMARY KEY`
   - `snapshot_id`: `BIGINT NOT NULL REFERENCES dataset_snapshot(id) ON DELETE CASCADE`
   - `transporter_id`: `BIGINT NOT NULL` (client transporter ID)
   - `name`: `VARCHAR(255) NOT NULL`
   - `UNIQUE(snapshot_id, transporter_id)`

4. **`lane_quote`**:
   - `id`: `BIGSERIAL PRIMARY KEY`
   - `snapshot_id`: `BIGINT NOT NULL REFERENCES dataset_snapshot(id) ON DELETE CASCADE`
   - `lane_id`: `BIGINT NOT NULL`
   - `transporter_id`: `BIGINT NOT NULL`
   - `quote_amount`: `NUMERIC(15, 2) NOT NULL CHECK (quote_amount >= 0)`
   - `UNIQUE(snapshot_id, lane_id, transporter_id)`
   - Foreign key to `(snapshot_id, lane_id)` on `lane`
   - Foreign key to `(snapshot_id, transporter_id)` on `transporter`

### Concurrency & Snapshot Isolation
Because the assignment endpoint `POST /api/v1/transporters/assignment` contains only `{"maxTransporters": N}` and does not pass a snapshot identifier, the system must guarantee snapshot consistency under concurrent operations:

1. **Atomic Ingestion (`DatasetSnapshotService`)**:
   - Each input submission is validated for internal consistency, cross-references, and non-negativity.
   - All lanes, transporters, and quotes are persisted under a new `DatasetSnapshotEntity` with `active = false`.
   - In the same database transaction, the previous active snapshot is deactivated, and the new snapshot is activated (`active = true`).
2. **Snapshot-Isolated Assignment Read (`TransporterAssignmentService`)**:
   - The assignment service loads the active snapshot ID at transaction start (`READ_COMMITTED` isolation).
   - All subsequent reads for lanes, transporters, and quotes are strictly scoped to that captured `snapshotId`.
   - Concurrent writes creating subsequent snapshots cannot corrupt or interleave with an ongoing assignment calculation.

---

## 4. API Error Handling Architecture

The application enforces a uniform, secure error structure via `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
  "status": "error",
  "message": "Human-readable summary of the error",
  "details": [
    "Field-specific or constraint-specific descriptions"
  ],
  "timestamp": "2026-11-13T06:22:05.181878339Z"
}
```

### HTTP Status Code Strategy
- **`200 OK`**: Successful input ingestion or optimal assignment result.
- **`400 Bad Request`**: Malformed JSON, Bean Validation constraint failures (e.g. `maxTransporters < 1`), duplicate IDs, or missing cross-references.
- **`404 Not Found`**: Assignment requested before any dataset snapshot has been submitted.
- **`422 Unprocessable Entity`**: Structurally valid dataset for which no valid complete assignment exists (infeasible optimization).
- **`500 Internal Server Error`**: Unexpected system or database failures (sanitized; zero stack traces leaked).

---

## 5. Security & Reliability Posture

- **Non-Root Docker Execution**: Runs as unprivileged `appuser` (UID/GID created in Alpine JRE stage).
- **SQL Injection Prevention**: 100% parameterized queries via Spring Data JPA. Zero raw SQL string concatenation.
- **Sanitized Logging**: Slf4j logs record entity counts and processing times; no sensitive credentials, secrets, or large payloads logged.
- **Actuator Healthcheck**: `/actuator/health` provides continuous liveness and readiness monitoring.
