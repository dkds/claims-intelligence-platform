# CLAUDE.md

Project context for Claude Code. Read this at the start of each session before proposing changes.

## Project

The **Claims Intelligence Platform** is an event-driven, polyglot microservices system for processing **veterinary insurance claims**. It is a portfolio project built to demonstrate a clean, modern architecture, and it deliberately fixes two anti-patterns: a database shared across services, and business logic encoded in the database.

The domain is generic and synthetic.

## Domain

The platform is a third-party administrator between veterinary clinics and an insurer.

- **Clinic** — a registered tenant organisation.
- **Clinic manager** — runs a clinic: enrols pets, manages vets, verifies sessions, submits manual claims.
- **Veterinarian (vet)** — a provider who delivers treatment; paid for verified sessions.
- **Pet** — the insured patient. **Pet owner** — policyholder; read-only access (added later).
- **Insurer** — the external payer.

Claims arrive two ways and converge into one pipeline:
- **Session-derived**: a vet logs a treatment session → the clinic manager verifies it → verified sessions auto-generate claim lines.
- **Manual**: the clinic manager submits a claim directly (meds, transport, food, boarding), bypassing sessions.
- **Convergence**: every claim is fraud/risk-scored, then triaged — verified + low-risk auto-adjudicates; manual or high-risk goes to adjuster review — then is submitted to the insurer. Verified sessions also trigger an internal payment to the vet.

## Architecture principles (do not violate)

- **Database per service.** Each service owns its database privately. Never read or write another service's database, and never write cross-database joins.
- **Logic in code.** Business rules live in a rich domain model and a rules engine — never in stored procedures, triggers, or schema.
- **Event-driven.** Services communicate through domain events on Kafka.
- **CQRS.** A single Projection service is the only writer of the read-only view store; everything else reads through the BFF. Never let two services write the same store.
- **Consistency.** Decisions read strongly-consistent local data; display and reporting tolerate eventual consistency.
- **Start simple, harden later.** Begin with naive publish-after-save and plain JSON; add the transactional outbox, schema registry, and idempotent consumers as deliberate later steps (build phase 7).

## Services (Tier 1 in focus)

| Service | Stack | Owns | Role |
|---|---|---|---|
| Enrollment & Policy | Java / Spring Boot | Postgres `enrollment` | Master data: clinics, pets, vets, catalogue, policies; vet approval |
| Sessions | Java / Spring Boot | Postgres `sessions` | Logs and verifies treatment sessions; emits `session.verified` |
| Claims | Java / Spring Boot | Postgres `claims` | Claim assembly (both origins), validation, triage/routing, adjudication, state machine |
| Fraud Detection | Go | small store | Scores claims (rules first, ML later); consumer |
| Projection | Node / NestJS | MongoDB | Builds the read model; the only writer of the view store |
| BFF / Gateway | Node / NestJS | — | Auth, aggregation; reads the view store, dispatches commands |
| Clinic workbench | React + Vite + TS | — | Verify sessions, submit manual claims, adjuster queue, dashboard |

Insurer Submission starts as a **module inside Claims** and is extracted into its own service later.

## Tech and wiring conventions

- Java: Spring Boot, Java 25, Maven, Spring Data JPA, Spring for Apache Kafka, PostgreSQL.
- Go: lean Kafka consumer for fraud detection.
- Node: NestJS with KafkaJS and Mongoose.
- Frontend: React + Vite + TypeScript, TanStack Query, reading via the BFF.
- Each Java service connects only to its **own** database with its **own** user, e.g. claims → `claims_user@postgres:5432/claims`.
- Infra hostnames inside the dev container: Postgres `postgres:5432`, Kafka `redpanda:9092`, Mongo `mongo:27017`, Redis `redis:6379`.

## Events and messaging

- One topic per aggregate stream, keyed by aggregate id: `cip.enrollment.v1`, `cip.sessions.v1`, `cip.claims.v1`, `cip.fraud.v1`, `cip.payments.v1`, `cip.submissions.v1`, `cip.documents.v1`.
- Every event uses the standard envelope: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `producer`, `tenantId`, `aggregateType`, `aggregateId`, `correlationId`, `causationId`, `payload`.
- Consumers are idempotent (deduplicate by `eventId`); one consumer group per service.
- See `docs/event-design.md` for full schemas and topology.

## Build plan and current state

Building **Tier 1** in phases; keep something runnable at each step:

0. Foundations (repo, dev container, infra) → 1. Enrollment & Policy → 2. Sessions + Kafka → 3. Claims → 4. Fraud Detection (Go) + triage → 5. Projection + BFF → 6. React workbench → 7. Hardening (outbox, schema registry, idempotency, auth).

**Current focus: Phase 0 → Phase 1.** Follow the Explore → Plan → Implement → Commit rhythm. See `docs/build-plan.md`.

## Scope

- **In (Tier 1):** the seven services above.
- **Deferred (Tier 2/3):** Payment, Insurer Submission as a standalone service, Document service and AI features, Notification, Kubernetes/Terraform, the Analytics Engine.

## Commands

- Java service: `cd services/<name> && ./mvnw spring-boot:run`; tests: `./mvnw test`.
- Go: `cd services/fraud-detection && go run ./...`; tests: `go test ./...`.
- Node service: `cd services/<name> && pnpm run start:dev`; tests: `pnpm test`.
- Frontend: `cd web/clinic-workbench && pnpm run dev`.
- Infra: `docker compose up` (file is `compose.yml`); the Redpanda console is at `localhost:8085`.

## Working conventions

- Keep services independent: communicate via events or the BFF, never direct database access.
- Write business rules as testable code, and add tests alongside logic.
- Prefer small, reviewable changes; commit at checkpoints (commits are SSH-signed).
- When adding an event, define it with the standard envelope and key it by aggregate id.
- Record significant decisions as short ADRs in `docs/adr/`.
- Use plan mode for any non-trivial change.

## Reference docs

The full design lives in `docs/`: the scope and service breakdown, the event/messaging design, and the phased build plan. Consult them before large changes.
