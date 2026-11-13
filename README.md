# FreightFox Transporter Assignment Optimizer

[![Java CI](https://github.com/shivam-shukla888/freightfox-transporter-optimizer/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/shivam-shukla888/freightfox-transporter-optimizer/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-10-red.svg)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)

A mathematically optimal, production-engineered backend service in **Java 21 LTS** and **Spring Boot 3.3.5** that assigns freight carrier transporters to shipping lanes using submitted quote matrices under user-defined constraints.

---

## 1. Live Production Deployment (Render)

The application and a managed PostgreSQL 17 database are deployed on Render via Infrastructure-as-Code (`render.yaml`):

- **Base API**: [https://freightfox-transporter-optimizer.onrender.com](https://freightfox-transporter-optimizer.onrender.com)
- **Interactive Swagger UI**: [https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html](https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html)
- **OpenAPI 3 JSON Spec**: [https://freightfox-transporter-optimizer.onrender.com/v3/api-docs](https://freightfox-transporter-optimizer.onrender.com/v3/api-docs)
- **Actuator Health Endpoint**: [https://freightfox-transporter-optimizer.onrender.com/actuator/health](https://freightfox-transporter-optimizer.onrender.com/actuator/health)

> [!NOTE]
> **Render Free-Tier Spin-Down**:
> Render free web instances spin down after 15 minutes of inactivity. When accessed after an idle period, the first request may experience a cold-start delay (~50 seconds) while the container boots and validates the database connection. Subsequent requests respond with normal sub-second latencies.

---

## 2. Reviewer Quick-Start Guide

Reviewers can verify the application either via the [Live Render Swagger UI](https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html) or locally via Docker Compose.

### Step-by-Step Verification Flow

1. **Open Swagger UI**:
   - Production: [https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html](https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html)
   - Local: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

2. **Ingest a Dataset Snapshot (`POST /api/v1/transporters/input`)**:
   Submit the lane and quote matrix payload.

   > [!WARNING]
   > **Atomic Snapshot Replacement**:
   > Submitting data to `POST /api/v1/transporters/input` atomically deactivates any existing dataset snapshot and replaces it with the newly submitted payload. All subsequent optimization requests will compute against this new active snapshot. Do not overwrite if you wish to evaluate the currently loaded dataset.

   <details>
   <summary><b>Click to expand Minimal 2-Lane Sample Payload</b></summary>

   ```json
   {
     "lanes": [
       { "id": 1, "origin": "Mumbai", "destination": "Delhi" },
       { "id": 2, "origin": "Chennai", "destination": "Kolkata" }
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
   **Expected Response (`200 OK`)**:
   ```json
   {
     "status": "success",
     "message": "Input data saved successfully."
   }
   ```
   </details>

3. **Execute Transporter Assignment (`POST /api/v1/transporters/assignment`)**:
   Request an optimal assignment with a specified transporter ceiling:
   ```json
   {
     "maxTransporters": 2
   }
   ```

4. **Inspect Result**:
   ```json
   {
     "status": "success",
     "totalCost": 22500.00,
     "assignments": [
       { "laneId": 1, "transporterId": 2 },
       { "laneId": 2, "transporterId": 1 }
     ],
     "selectedTransporters": [1, 2]
   }
   ```

---

## 3. Optimization Objective Hierarchy

The service models lane allocation as a **lexicographic combinatorial optimization problem**:

1. **Priority 1 (Primary)**: **Maximize** the count of distinct transporters actually assigned to lanes, up to $K = \text{maxTransporters}$.
2. **Priority 2 (Secondary)**: Among all feasible assignments achieving that maximal transporter count, **minimize** total transportation cost.
3. **Hard Constraints**:
   - **Exact Lane Coverage**: Every lane must receive exactly one transporter.
   - **Transporter Utilization Integrity**: Every transporter in `selectedTransporters` must be assigned to at least one lane ($y_t = 1 \iff \sum_l x_{l,t} \ge 1$).
   - **Strict Quote Grounding**: Only submitted quotes are eligible. The engine **never** fabricates missing quotes or assigns an unquoted carrier to a lane.
   - **Numerical Safety**: All currency values use `BigDecimal` with scale 2 (`HALF_UP`) to eliminate binary floating-point drift.

*Note: This lexicographic hierarchy reflects the explicit assignment requirements to encourage carrier diversity before minimizing expenditure.*

---

## 4. Technology Stack & Architectural Principles

- **Language**: Java 21 LTS (Eclipse Temurin)
- **Framework**: Spring Boot 3.3.5
- **Persistence**: Spring Data JPA, Hibernate ORM, PostgreSQL 17
- **Database Migrations**: Flyway 10
- **Documentation**: SpringDoc OpenAPI 3 / Swagger UI 2.6.0
- **Testing**: JUnit 5, AssertJ, Mockito, Spring Boot Test, H2 (isolated migration compatibility profile)
- **Containerization**: Multi-stage Dockerfile (Alpine JRE 21, unprivileged non-root user `appuser`), Docker Compose
- **Cloud Deployment**: Render (`render.yaml` Infrastructure-as-Code Blueprint)

### Architecture Highlights
- **Modular Monolith**: Strict layer boundaries between input ingestion (`dataset`), assignment orchestration (`assignment`), and mathematical optimization (`optimizer`).
- **Framework-Agnostic Core**: The core solver in `com.freightfox.optimizer.*` is pure Java with **zero** dependencies on Spring, Hibernate, JPA, or SQL (verified via reflection tests).
- **Snapshot Isolation**: Each input payload is validated and persisted as an immutable `DatasetSnapshot`. Assignment requests read the active snapshot within a `READ_COMMITTED` transaction, avoiding data races during concurrent ingestion.

---

## 5. Local Quick Start with Docker Compose

To run the complete stack (Spring Boot Application + PostgreSQL 17) locally:

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

## 6. Local Development (Maven + PostgreSQL)

### 1. Start PostgreSQL Container
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
Or directly with Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 7. REST API Specification

### Endpoint 1: Ingest Lane & Transporter Quotes

- **Method**: `POST`
- **Route**: `/api/v1/transporters/input`
- **Content-Type**: `application/json`

#### Request Payload
```json
{
  "lanes": [
    { "id": 1, "origin": "Mumbai", "destination": "Delhi" },
    { "id": 2, "origin": "Chennai", "destination": "Kolkata" }
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

#### Success Response (`200 OK`) — 9-Lane Official Matrix Fixture
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

All client and server errors return a consistent RFC-compliant payload:

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

| HTTP Status | Trigger Condition |
|---|---|
| `400 Bad Request` | Validation constraint violations (negative quotes, duplicate IDs, `maxTransporters < 1`). |
| `404 Not Found` | Assignment requested before an active dataset snapshot exists. |
| `422 Unprocessable Entity` | Infeasible problem (e.g. unquoted lanes preventing full coverage). |
| `500 Internal Server Error` | Unexpected server runtime exceptions. |

---

## 8. Testing & Quality Assurance

The repository includes **31 automated tests** covering unit, integration, oracle comparison, and concurrency:

```bash
mvn clean verify
```

### Test Suite Highlights
- **Brute-Force Oracle Comparison**: `AssignmentOptimizerOracleComparisonTest` validates the dynamic programming engine against an independent brute-force solver over **50 randomized problem instances** with 100% agreement.
- **Architectural Isolation**: `PureJavaOptimizerIsolationTest` uses reflection to verify that `com.freightfox.optimizer.*` contains 0 dependencies on Spring, Hibernate, or SQL.
- **Official 9-Lane Matrix**: Verified against the golden 9-lane tender matrix ($N=9, M=7, K=3$), achieving exact cost `134,876.00` with transporters `[1, 4, 6]`.
- **PostgreSQL 17 Integration**: `PostgresDatabaseIntegrationTest` connects to genuine PostgreSQL 17 on port 5434 and validates JDBC major version 17 and Flyway migrations.
- **Concurrency Safety**: `ConcurrentDatasetAndAssignmentIntegrationTest` tests concurrent assignment queries during active snapshot replacement transactions.

---

## 9. Optimization Algorithm & Computational Characteristics

The solver uses **Transporter Subset Enumeration with Bitmask Dynamic Programming over Lanes**:

1. **Descending Cardinality Search**:
   Iterates $k = \min(K, M)$ down to $1$. For each $k$, it evaluates all $\binom{M}{k}$ transporter subsets $S \subseteq \mathcal{T}$ in lexicographical order.
2. **Bitmask DP per Subset**:
   For a fixed subset $S$ of size $k$, a bitmask of length $k$ tracks which carriers in $S$ have been assigned at least one lane.
   - States: $\text{DP}[i][\text{mask}] = \text{minimal cost to assign lanes } 0 \dots i-1 \text{ covering carrier mask } \text{mask}$.
   - Acceptance: Only solutions reaching the full mask $2^k - 1$ at lane $N$ are accepted, guaranteeing that every carrier in $S$ is utilized.

### Computational Complexity & Practical Scale
- **Time Complexity**: $\mathcal{O}\left( \binom{M}{k} \cdot N \cdot k \cdot 2^k \right)$ per cardinality $k$.
- **Space Complexity**:
  - Rolling DP cost transition state: $\mathcal{O}(2^k)$ values.
  - Reconstruction backpointer table: $\mathcal{O}(N \cdot 2^k)$ to trace parent masks and assigned carriers.
- **Practical Problem Scale**:
  Because subset enumeration grows combinatorially with $\binom{M}{k}$, this exact algorithm is tailored for practical freight tender matrices ($M \le 20, K \le 6, N \le 100$). Extremely large bidding problems ($M > 50, K > 15$) would require mixed-integer linear programming (MILP) or column generation solvers.

### Local Benchmark Measurements
Measured on a standard developer workstation via `AssignmentOptimizerBenchmarkTest`:

| Problem Instance | Lanes ($N$) | Transporters ($M$) | Submitted Quotes | $K$ (`maxTransporters`) | Optimization Latency | Memory Allocated |
|---|---|---|---|---|---|---|
| **Official 9-Lane Matrix** | 9 | 7 | 63 | 3 | **1.82 ms** | 1.0 MB |
| **Medium Scale** | 15 | 8 | 106 | 4 | **1.69 ms** | 2.9 MB |
| **Large Scale** | 20 | 10 | 168 | 5 | **8.86 ms** | 20.4 MB |
| **High Density Scale** | 25 | 12 | 246 | 4 | **12.24 ms** | 20.4 MB |

*Note: Benchmarks reflect standalone algorithm execution time on tested problem sizes in a controlled local JVM and do not represent end-to-end network latency or an SLA guarantee.*

---

## 10. Documentation Index

- [Architecture & Design Details](docs/ARCHITECTURE.md)
- [Optimization Algorithm & Complexity](docs/ALGORITHM.md)
- [Requirements Traceability Matrix](docs/REQUIREMENTS_TRACEABILITY.md)
- [Assumptions & PDF Discrepancies Disclosures](docs/ASSUMPTIONS.md)
- [Testing Strategy & Test Catalog](docs/TESTING.md)
- [Production Deployment Guide](docs/DEPLOYMENT.md)
- [Delivery Checklist](docs/DELIVERY_CHECKLIST.md)
- [Engineering Handover Report](docs/FINAL_REPORT.md)
