# FreightFox Transporter Assignment Optimizer

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-10-red.svg)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Proprietary-gray.svg)]()

A production-grade, mathematically optimal backend service engineered in **Java 21** and **Spring Boot 3.3.5** for assigning freight transporters to shipping lanes using submitted quote matrices.

---

## 1. Problem Overview & Optimization Objective

Freight logistics procurement requires allocating a set of shipping lanes to carrier transporters. Given lane details, transporter quote matrices, and a user-specified ceiling $K = \text{maxTransporters}$, the engine solves a **lexicographic combinatorial optimization problem**:

1. **Priority 1 (Primary)**: **Maximize** the number of distinct transporters actually assigned to lanes, up to $K$.
2. **Priority 2 (Secondary)**: Among all feasible assignments achieving that maximal transporter count, **minimize** the total transportation cost.
3. **Hard Constraints**:
   - **Exact Lane Coverage**: Every lane must be assigned to exactly one transporter.
   - **Transporter Utilization Integrity**: Every transporter reported in `selectedTransporters` must be assigned to at least one lane (no unused selected transporters).
   - **Strict Quote Grounding**: Only submitted quotes are eligible. The engine **never** invents quotes or defaults unquoted lanes.
   - **Numerical Safety**: All monetary calculations use `BigDecimal` to eliminate binary floating-point drift.

---

## 2. Technology Stack & Architectural Principles

- **Language**: Java 21 LTS (Eclipse Temurin)
- **Framework**: Spring Boot 3.3.5
- **Persistence**: Spring Data JPA, Hibernate ORM, PostgreSQL 17
- **Database Migrations**: Flyway 10
- **Documentation**: SpringDoc OpenAPI 3 / Swagger UI 2.6.0
- **Testing**: JUnit 5, AssertJ, Mockito, Spring Boot Test, H2 (test fallback)
- **Containerization**: Multi-stage Dockerfile (Alpine JRE 21, non-root user `appuser`), Docker Compose
- **Cloud Deployment**: Render (`render.yaml` Infrastructure-as-Code)

### Architecture Highlights
- **Modular Monolith**: Clean boundaries between ingestion (`dataset`), assignment orchestration (`assignment`), and core math (`optimizer`).
- **Framework-Agnostic Optimization Core**: The optimizer in `com.freightfox.optimizer.*` is pure Java with **zero** dependencies on Spring, Hibernate, JPA, or SQL.
- **Snapshot Isolation**: Each input payload is validated and persisted as an immutable `DatasetSnapshot`. Assignment calculations read a single active snapshot inside a `READ_COMMITTED` transaction, ensuring zero race conditions during concurrent data updates.

---

## 3. Quick Start with Docker Compose (Recommended)

The easiest way to run the complete stack (Spring Boot Application + PostgreSQL 17) is with Docker Compose:

```bash
# Clone the repository
git clone https://github.com/shivam-shukla888/freightfox-transporter-optimizer.git
cd freightfox-transporter-optimizer

# Build and start services in the background
docker compose up --build -d
```

### Verify Service Health
```bash
docker compose ps
```

Expected output:
```
NAME                  STATUS                    PORTS
freightfox-app        Up (healthy)              0.0.0.0:8080->8080/tcp
freightfox-postgres   Up (healthy)              0.0.0.0:5434->5432/tcp
```

### Check Liveness Endpoint
```bash
curl http://localhost:8080/actuator/health
# Response: {"status":"UP"}
```

---

## 4. Local Development (Maven + PostgreSQL)

### 1. Start PostgreSQL Database
If running PostgreSQL via Docker:
```bash
docker run -d --name freightfox-postgres -p 5434:5432 \
  -e POSTGRES_DB=freightfox \
  -e POSTGRES_USER=freightfox \
  -e POSTGRES_PASSWORD=freightfox \
  postgres:17-alpine
```

### 2. Build & Run Application
```bash
mvn clean package -DskipTests
java -jar target/transporter-optimizer-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```
Or with Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 5. REST API Specification

### Endpoint 1: Ingest Lane & Transporter Quotes

- **Method**: `POST`
- **Route**: `/api/v1/transporters/input`
- **Content-Type**: `application/json`

#### Request Payload
```json
{
  "lanes": [
    {
      "id": 1,
      "origin": "Mumbai",
      "destination": "Delhi"
    },
    {
      "id": 2,
      "origin": "Chennai",
      "destination": "Kolkata"
    }
  ],
  "transporters": [
    {
      "id": 1,
      "name": "Transporter T1",
      "laneQuotes": [
        { "laneId": 1, "quote": 14000.00 },
        { "laneId": 2, "quote": 10500.00 }
      ]
    },
    {
      "id": 2,
      "name": "Transporter T2",
      "laneQuotes": [
        { "laneId": 1, "quote": 12000.00 },
        { "laneId": 2, "quote": 13000.00 }
      ]
    }
  ]
}
```

#### Success Response (`200 OK`)
```json
{
  "status": "success",
  "message": "Input data saved successfully."
}
```

---

### Endpoint 2: Optimize & Assign Transporters

- **Method**: `POST`
- **Route**: `/api/v1/transporters/assignment`
- **Content-Type**: `application/json`

#### Request Payload
```json
{
  "maxTransporters": 3
}
```

#### Success Response (`200 OK`)
```json
{
  "status": "success",
  "totalCost": 134876.00,
  "assignments": [
    { "laneId": 1, "transporterId": 4 },
    { "laneId": 2, "transporterId": 1 },
    { "laneId": 3, "transporterId": 4 },
    { "laneId": 4, "transporterId": 4 },
    { "laneId": 5, "transporterId": 4 },
    { "laneId": 6, "transporterId": 1 },
    { "laneId": 7, "transporterId": 1 },
    { "laneId": 8, "transporterId": 6 },
    { "laneId": 9, "transporterId": 6 }
  ],
  "selectedTransporters": [1, 4, 6]
}
```

---

### Centralized Error Handling

All client and server errors return a standard JSON structure:

```json
{
  "status": "error",
  "message": "Request validation failed",
  "details": [
    "maxTransporters: maxTransporters must be at least 1"
  ],
  "timestamp": "2026-11-13T06:22:05.181878339Z"
}
```

| HTTP Code | Scenario |
|---|---|
| `400 Bad Request` | Missing required fields, negative quotes, duplicate lane/transporter IDs, invalid `maxTransporters < 1`. |
| `404 Not Found` | Assignment requested before any dataset snapshot has been submitted. |
| `422 Unprocessable Entity` | Problem is mathematically infeasible (e.g. unquoted lanes). |
| `500 Internal Server Error` | Unexpected runtime error. |

---

## 6. Interactive OpenAPI / Swagger UI

Interactive Swagger documentation is available once the application is running:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3 JSON Schema**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 7. Testing & Quality Assurance

The application includes a rigorous test suite of **31 automated tests** covering unit, integration, oracle comparison, and concurrency:

```bash
# Run all tests
mvn clean test
```

### Highlights of Test Verification
- **Oracle Comparison**: `AssignmentOptimizerOracleComparisonTest` validates the dynamic programming engine against an independent brute-force reference solver across **50 randomized problem instances** with 100% agreement.
- **Pure Java Isolation**: `PureJavaOptimizerIsolationTest` uses reflection to verify that the optimizer package contains **zero** dependencies on Spring, Hibernate, or SQL.
- **Official 9-Lane Matrix**: Verified against the golden screenshot fixture ($N=9, M=7, K=3$), achieving exact cost `134,876.00` with transporters `[1, 4, 6]`.
- **Concurrent Ingestion & Assignment**: `ConcurrentDatasetAndAssignmentIntegrationTest` tests simultaneous assignment reads during an active snapshot replacement transaction.

---

## 8. Performance Benchmarks

Measured on standard consumer hardware via `AssignmentOptimizerBenchmarkTest`:

| Problem Instance | Lanes ($N$) | Transporters ($M$) | Submitted Quotes | $K$ (`maxTransporters`) | Optimization Latency |
|---|---|---|---|---|---|
| **Official 9-Lane Matrix** | 9 | 7 | 63 | 3 | **1.82 ms** |
| **Medium Scale** | 15 | 8 | 106 | 4 | **1.69 ms** |
| **Large Scale** | 20 | 10 | 168 | 5 | **8.86 ms** |
| **High Density Scale** | 25 | 12 | 246 | 4 | **12.24 ms** |

All benchmark instances solve in **< 15 milliseconds** on standard developer hardware, demonstrating fast, deterministic execution for typical freight tender matrices.

---

## 9. Render Cloud Deployment

The repository includes a production-ready `render.yaml` blueprint:
1. Connect this repository to your [Render Dashboard](https://dashboard.render.com).
2. Select **New +** $\rightarrow$ **Blueprint**.
3. Render automatically provisions:
   - Web Service: `freightfox-transporter-optimizer` (Docker container)
   - Database: `freightfox-postgres-db` (PostgreSQL)
4. Database connection credentials are securely wired automatically.

For detailed deployment instructions, see [docs/DEPLOYMENT.md](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/DEPLOYMENT.md).

---

## 10. Documentation Index

- [Architecture & Design Details](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/ARCHITECTURE.md)
- [Optimization Algorithm & Complexity](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/ALGORITHM.md)
- [Requirements Traceability Matrix](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/REQUIREMENTS_TRACEABILITY.md)
- [Assumptions & PDF Discrepancies Disclosures](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/ASSUMPTIONS.md)
- [Testing Strategy & Test Catalog](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/TESTING.md)
- [Production Deployment Guide](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/DEPLOYMENT.md)
- [Delivery Checklist](file:///c:/Users/thesh/freightfox-transporter-optimizer/docs/DELIVERY_CHECKLIST.md)
