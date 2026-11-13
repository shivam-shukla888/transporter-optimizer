# Architecture

## Architectural style

A modular monolith is the deliberate choice for this assignment. The scope has two REST endpoints and one optimization capability; microservices would add deployment and operational complexity without a demonstrated need.

## Modules

- **controller** — HTTP contract, request/response mapping, status codes.
- **service** — use-case orchestration and transaction boundaries.
- **optimizer** — pure Java optimization logic and result reconstruction.
- **domain** — core lane, transporter, quote and assignment concepts.
- **repository** — persistence interfaces and database access.
- **dto** — API request and response records.
- **exception** — consistent error mapping.
- **config** — application and OpenAPI configuration.

## Dependency direction

HTTP/controller -> application service -> domain/optimizer and repositories.

The optimizer must not depend on Spring, JPA, PostgreSQL, web classes or external services. It accepts validated in-memory input and returns a deterministic result.

## Persistence model

Initial relational entities:

- DatasetSnapshot
- Lane
- Transporter
- LaneQuote

A snapshot groups one complete input submission. Lane and transporter records are scoped to that snapshot. A quote references one lane and one transporter within the same snapshot. Enforce uniqueness and foreign-key constraints in PostgreSQL.

A single active snapshot is maintained for the assignment's API contract because the assignment request contains no snapshot identifier. Input replacement must be transactional. The assignment use case must capture one active snapshot and read all its data consistently.

## API contract

Preserve the assignment's required routes:

- POST /api/v1/transporters/input
- POST /api/v1/transporters/assignment

Do not invent extra required routes or alter required response fields. Any additive response fields must be checked against the assignment contract first.

## Reliability

- Bean Validation for request shape and basic constraints.
- Domain validation for cross-record constraints.
- Global exception handler with stable error payloads.
- PostgreSQL constraints as the final integrity guard.
- Transaction boundaries around dataset activation and persistence.
- Structured logs without credentials or sensitive data.
- Health endpoint for deployment checks.

## Deliberately excluded

No frontend, message broker, cache, AI service or microservice split unless a measured requirement justifies one.
