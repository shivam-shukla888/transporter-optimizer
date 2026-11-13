# Delivery checklist

## Requirements
- [ ] Confirm exact request and response schemas from the assignment PDF.
- [ ] Record all assumptions and sample inconsistencies.
- [ ] Verify all supplied fixture files and expected results.

## Implementation
- [ ] Java 21 and Spring Boot project builds.
- [ ] PostgreSQL schema migrations are repeatable.
- [ ] Input endpoint validates and atomically activates a dataset snapshot.
- [ ] Optimizer is framework-independent.
- [ ] Primary objective: maximize feasible distinct transporter usage.
- [ ] Secondary objective: minimum total cost.
- [ ] Each lane is assigned exactly once.
- [ ] Every selected transporter is actually used.
- [ ] Deterministic tie-breaking is documented.

## Quality
- [ ] Unit tests for solver and validation.
- [ ] Brute-force oracle tests on small generated cases.
- [ ] Integration tests against PostgreSQL.
- [ ] API contract and error-response tests.
- [ ] Concurrency test for dataset replacement and assignment reads.
- [ ] Benchmark report contains reproducible measurements.

## Delivery
- [ ] Docker image builds.
- [ ] Docker Compose starts application and database.
- [ ] GitHub Actions CI passes.
- [ ] Public deployment has been tested from a clean browser/session.
- [ ] Swagger UI is accessible.
- [ ] README includes setup, environment variables, API examples and limitations.
- [ ] Loom walkthrough demonstrates the real deployed service.
- [ ] Google Form submission is confirmed.
