# FreightFox Transporter Assignment Optimizer — Engineering Handover Report

- **Author**: Senior Backend & Systems Engineer
- **Date**: November 13, 2026
- **Repository**: [https://github.com/shivam-shukla888/freightfox-transporter-optimizer](https://github.com/shivam-shukla888/freightfox-transporter-optimizer)
- **Status**: **Verified Candidate Implementation — Automated Test Suite & Remote CI Passing**

---

## 1. Executive Summary

This engineering project implements a backend system for FreightFox that solves the **Transporter Assignment on Lanes** problem using submitted quote matrices.

The engine enforces a strict two-tier lexicographic objective hierarchy:
1. **Priority 1**: Maximize the number of distinct transporters actually assigned to lanes, up to the user-supplied ceiling `maxTransporters`.
2. **Priority 2**: Minimize total transportation cost among all feasible assignments achieving that maximal transporter count.
3. **Hard Constraints**: Full lane coverage, transporter utilization integrity (every carrier in `selectedTransporters` receives at least one lane), strict quote grounding (no fabricated quotes), deterministic tie-breaking, and numerical safety (`BigDecimal`).

The architecture is implemented as a **Modular Monolith** using **Java 21 LTS**, **Spring Boot 3.3.5**, **PostgreSQL 17**, and **Flyway 10**, containerized via **Docker** and **Docker Compose**, with CI/CD automation via **GitHub Actions** and cloud deployment blueprints for **Render**.

---

## 2. Verification & Test Evidence

### A. Automated Test Suite & Local Verification
- **Test Command**: `mvn clean verify`
- **Total Tests Executed**: 31
- **Passed**: 31 (0 failures, 0 errors, 0 skipped)
- **Local Execution Time**: ~25–27 seconds

#### Test Suite Breakdown:
1. **Optimization Unit Tests (`AssignmentOptimizerTest`)**: 10 tests verifying base cases, edge conditions, tie resolution, monetary scale, infeasible problem handling, and reference fixtures.
2. **Oracle Comparison (`AssignmentOptimizerOracleComparisonTest`)**: 50 randomized problem instances verified against an independent brute-force reference solver with 100% agreement.
3. **Architectural Purity (`PureJavaOptimizerIsolationTest`)**: Reflection assertions confirming 0 imports or references to Spring, Hibernate, JPA, or SQL inside `com.freightfox.optimizer.*`.
4. **Performance Benchmarking (`AssignmentOptimizerBenchmarkTest`)**:
   - 9 Lanes / 7 Transporters (Official Matrix): **1.82 ms** (1.0 MB memory)
   - 15 Lanes / 8 Transporters: **1.69 ms** (2.9 MB memory)
   - 20 Lanes / 10 Transporters: **8.86 ms** (20.4 MB memory)
   - 25 Lanes / 12 Transporters: **12.24 ms** (20.4 MB memory)
   *(Measurements reflect standalone engine latency on developer hardware, not end-to-end network SLAs).*
5. **Relational Persistence (`DatasetRepositoryIntegrationTest`)**: Validates entity mappings, cascade semantics, non-negative quote check constraints, and atomic snapshot deactivation/activation on PostgreSQL 17.
6. **API Contracts (`DatasetInputApiIntegrationTest`, `AssignmentApiIntegrationTest`)**:
   - `POST /api/v1/transporters/input`: Valid submission returns 200 OK; invalid submissions return 400 Bad Request with structured field details.
   - `POST /api/v1/transporters/assignment`: 9-lane matrix fixture yields exact golden optimum (`totalCost: 134876.00`, transporters `[1, 4, 6]`). Uninitialized requests return 404 Not Found; invalid parameters return 400 Bad Request; infeasible problems return 422 Unprocessable Entity.
7. **PostgreSQL 17 Compatibility (`PostgresDatabaseIntegrationTest`)**: Validates real PostgreSQL 17.12 database metadata (`getDatabaseMajorVersion() == 17`) and Flyway schema initialization on port 5434.
8. **Flyway Migration Compatibility (`DatabaseMigrationTest`)**: Validates clean Flyway V1 schema migration on an in-memory H2 database under a dedicated `h2-test` profile.
9. **Concurrency Safety (`ConcurrentDatasetAndAssignmentIntegrationTest`)**: Multi-threaded execution validating that active snapshot replacement transactions do not corrupt concurrent assignment reads.

### B. Remote GitHub Actions CI Verification
- **Verified Commit**: `f60a5940a2a10aa5aebe5583a3ac180487c358d8`
- **Workflow Run**: [https://github.com/shivam-shukla888/freightfox-transporter-optimizer/actions/runs/43382287648](https://github.com/shivam-shukla888/freightfox-transporter-optimizer/actions/runs/43382287648)
- **Workflow Status**: `completed`
- **Conclusion**: `success`
- **Verified Steps**:
  1. Checkout source code: Success
  2. Setup Eclipse Temurin JDK 21: Success
  3. Initialize PostgreSQL 17 service container: Success (healthcheck verified via `pg_isready`)
  4. Run `mvn --batch-mode clean verify`: Success (all 31 tests passed, including `PostgresDatabaseIntegrationTest` and `DatabaseMigrationTest`)
  5. Multi-stage Docker build (`docker build -t freightfox-optimizer:ci .`): Success

### C. Live Cloud PaaS Verification (Render)
The following live endpoints on Render were independently verified active and returning HTTP 200:
- **Base Application**: `https://freightfox-transporter-optimizer.onrender.com`
- **Health Check**: `https://freightfox-transporter-optimizer.onrender.com/actuator/health` $\rightarrow$ `{"status":"UP","groups":["liveness","readiness"]}`
- **Interactive Swagger UI**: `https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html` $\rightarrow$ HTTP 200
- **OpenAPI 3 Specification**: `https://freightfox-transporter-optimizer.onrender.com/v3/api-docs` $\rightarrow$ HTTP 200 (OpenAPI 3.0.1)

### D. Unverified Aspects & Operational Boundaries
- **Cold-Start Delays**: Render free-tier instances sleep after 15 minutes of inactivity. Initial request latency (~50s) during spin-up is an expected platform behavior, not an application defect.
- **Data Persistence Limits**: Render's free PostgreSQL database is subject to provider policies (storage limits, potential 30-day trial expiration on free accounts). Permanent storage guarantees require an upgraded managed database tier.
- **Active Snapshot Replacement**: `POST /api/v1/transporters/input` atomically replaces the single active dataset. In a shared public demo environment, any visitor submitting an input payload alters the dataset evaluated by subsequent assignment requests.

---

## 3. Disclosed PDF Discrepancies & Architectural Assumptions

1. **Assignment PDF Sample Arithmetic Discrepancy**:
   - The assignment PDF displayed a sample total cost of `75,000`, but summing the individual lane assignment costs in that same table yields `69,001` (a `5,999` discrepancy).
   - The sample listed selected transporters `[4, 6, 3]`, yet assigned Lane 3 to Transporter 2 (who was omitted from the selected transporter list).
   - Under the verified lexicographic objective hierarchy, the true mathematical optimum for that 5-lane sample is `58,139.00` using transporters `[1, 4, 5]`.
2. **Missing Quotes in PDF Illustration**:
   - Early assignment examples depicted assignments for lane-transporter pairs without submitted quotes.
   - Grounding Resolution: The application enforces strict quote grounding; an assignment is only permissible if an explicit, non-negative quote was submitted for that lane and carrier.

---

## 4. Requirements Traceability Summary

| ID | Requirement | Implementation Artifact | Verification Status |
|---|---|---|---|
| REQ-01 | Maximize Distinct Transporters (Priority 1) | `AssignmentOptimizer.java` | Verified (Descending cardinality search) |
| REQ-02 | Minimize Cost at Max Transporters (Priority 2) | `AssignmentOptimizer.java` | Verified (Bitmask DP cost minimization) |
| REQ-03 | Full Lane Coverage | `AssignmentOptimizer.java` | Verified (Sequential DP transitions over all $N$ lanes) |
| REQ-04 | Selected Transporter Utilization Integrity | `AssignmentOptimizer.java` | Verified (Accepts only full-mask $2^k-1$ solutions) |
| REQ-05 | Strict Quote Grounding | `AssignmentOptimizer.java` | Verified (Unquoted assignments impossible) |
| REQ-06 | Infeasible Problem Detection (422) | `TransporterAssignmentService.java` | Verified (Integration test `testInfeasibleAssignmentReturns422`) |
| REQ-07 | Deterministic Tie-Breaking | `AssignmentOptimizer.java` | Verified (Lexicographical subset and lane ordering) |
| REQ-08 | Monetary Precision (`BigDecimal`) | `com.freightfox.optimizer.model.*` | Verified (Scale 2, `HALF_UP`, no binary float drift) |
| REQ-09 | Pure Java Optimization Engine | `com.freightfox.optimizer.*` | Verified (Reflection test `PureJavaOptimizerIsolationTest`) |
| REQ-10 | Brute-Force Oracle Comparison | `AssignmentOptimizerOracleComparisonTest.java` | Verified (50 randomized instances, 100% agreement) |
| REQ-11 | 9-Lane Golden Matrix Fixture | `AssignmentApiIntegrationTest.java` | Verified (Exact cost `134876.00`, carriers `[1, 4, 6]`) |
| REQ-12 | Relational Schema & Flyway Migrations | `V1__init_schema.sql` | Verified (Flyway migrations on PostgreSQL 17 and H2) |
| REQ-13 | Atomic Snapshot Replacement | `DatasetSnapshotService.java` | Verified (Single active snapshot per transaction) |
| REQ-14 | Input Ingestion REST API | `DatasetInputController.java` | Verified (HTTP 200 on success, HTTP 400 on validation failure) |
| REQ-15 | Transporter Assignment REST API | `AssignmentController.java` | Verified (Exact API contract matching assignment schema) |
| REQ-16 | Centralized Global Exception Handling | `GlobalExceptionHandler.java` | Verified (Standard error format across 400, 404, 422, 500) |
| REQ-17 | OpenAPI 3 / Interactive Swagger Documentation | `OpenApiConfig.java` | Verified (`/swagger-ui/index.html` and `/v3/api-docs`) |
| REQ-18 | Production Containerization | `Dockerfile`, `docker-compose.yml` | Verified (Multi-stage build, unprivileged user `appuser`) |
| REQ-19 | GitHub Actions CI/CD Automation | `.github/workflows/ci.yml` | Verified (Run 43382287648 completed successfully) |
| REQ-20 | Performance Profiling | `AssignmentOptimizerBenchmarkTest.java` | Verified (Local benchmarks measured < 15ms on tested sizes) |
| REQ-21 | Architecture & Operational Documentation | `docs/*.md`, `README.md` | Verified (All documentation updated and cross-linked) |

---

## 5. Conclusion

The application is thoroughly verified across unit tests, oracle validation, database integration, containerization, remote CI, and cloud deployment. All architectural boundaries and assumptions are explicitly documented and reproducible.
