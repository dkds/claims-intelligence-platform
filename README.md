# Claims Intelligence Platform

A portfolio project demonstrating a clean, event-driven, polyglot microservices architecture for processing **veterinary insurance claims**. The platform deliberately corrects two common anti-patterns: a database shared across services, and business logic encoded in the database.

> Domain and all data are generic and synthetic.

---

## Overview

The platform is a third-party administrator sitting between veterinary clinics and an insurer. Claims arrive through two intake paths and converge into one pipeline:

```
Vet logs session ──► Clinic manager verifies ──► Session-derived claim ──┐
                                                                          ├──► Fraud score ──► Triage ──► Adjudication ──► Insurer
Clinic manager submits manually ─────────────────────────────────────────┘
```

- **Session-derived**: a vet logs a treatment session → clinic manager verifies → auto-generates claim lines.
- **Manual**: clinic manager submits a claim directly (meds, transport, boarding), bypassing sessions.
- **Convergence**: every claim is fraud/risk-scored, then triaged — verified low-risk claims auto-adjudicate; manual or high-risk go to adjuster review — then submitted to the insurer. Verified sessions also trigger a vet payment.

---

## Architecture Principles

| Principle | How it is applied |
|---|---|
| **Database per service** | Each service owns its store privately; no service reads or writes another's database. |
| **Logic in code** | Business rules live in a rich domain model and rules engine — never in stored procedures or schema. |
| **Event-driven** | Services communicate via domain events on Kafka; the transactional outbox ensures reliable publishing. |
| **CQRS** | A single Projection service is the only writer of the read-only view store; everything else reads through the BFF. |
| **Eventual consistency** | Decisions read strongly-consistent local data; display and reporting tolerate eventual consistency. |

---

## Services

| Service | Stack | Database | Role |
|---|---|---|---|
| Enrollment & Policy | Java / Spring Boot | PostgreSQL | Master data: clinics, pets, vets, catalogue, policies |
| Sessions | Java / Spring Boot | PostgreSQL | Treatment session logging and verification |
| Claims | Java / Spring Boot | PostgreSQL | Claim assembly, validation, triage, adjudication, state machine |
| Fraud Detection | Go | — | Rules-based fraud/risk scoring |
| Projection | Node.js / NestJS | MongoDB | Builds and owns the CQRS read model |
| BFF / Gateway | Node.js / NestJS | — | Auth, aggregation; reads view store, dispatches commands |
| Clinic Workbench | React + Vite + TypeScript | — | Verify sessions, submit claims, adjuster queue, dashboard |

---

## Event Topology

Services communicate through domain events on Kafka. All events use a standard envelope carrying an event ID (for idempotency), correlation/causation IDs (for tracing), and a typed payload.

| Topic | Producer | Key events |
|---|---|---|
| `cip.enrollment.v1` | Enrollment & Policy | `clinic.registered`, `pet.enrolled`, `vet.approved` |
| `cip.sessions.v1` | Sessions | `session.logged`, `session.verified` |
| `cip.claims.v1` | Claims | `claim.assembled`, `claim.adjudicated`, `claim.routed-to-review` |
| `cip.fraud.v1` | Fraud Detection | `claim.fraud-scored` |
| `cip.payments.v1` | Payment (Tier 2) | `payment.completed` |
| `cip.submissions.v1` | Insurer Submission (Tier 2) | `claim.submitted`, `claim.accepted` |

---

## Local Development

The dev environment runs entirely inside a dev container. All infrastructure is managed by Docker Compose.

**Infrastructure hostnames:**

| Service | Host |
|---|---|
| PostgreSQL | `postgres:5432` |
| Kafka (Redpanda) | `redpanda:9092` |
| Redpanda Console | `localhost:8085` |
| MongoDB | `mongo:27017` |
| Redis | `redis:6379` |

**Run a service:**

```bash
# Java services
cd services/enrollment && ./mvnw spring-boot:run
cd services/sessions   && ./mvnw spring-boot:run
cd services/claims     && ./mvnw spring-boot:run

# Go
cd services/fraud-detection && go run ./...

# Node services
cd services/projection && pnpm run start:dev
cd services/bff        && pnpm run start:dev

# Frontend
cd web/clinic-workbench && pnpm run dev
```

**Run tests:**

```bash
./mvnw test          # Java
go test ./...        # Go
pnpm test            # Node / frontend
```

---

## Build Phases

Tier 1 is built incrementally — something runnable at every step:

| Phase | Focus | Status |
|---|---|---|
| 0 | Foundations: monorepo, dev container, infra, service stubs | Complete |
| 1 | Enrollment & Policy: REST API, master data, vet approval | Complete |
| 2 | Sessions + Kafka: first event flow, session verification | Complete |
| 3 | Claims: assembly, rules engine, state machine | Complete |
| 4 | Fraud Detection (Go) + triage routing | Complete |
| 5 | Projection + BFF: CQRS read model | Complete |
| 6 | Clinic Workbench: end-to-end UI | In progress |
| 7 | Hardening: outbox, schema registry, idempotency, auth | — |

---

## Documentation

- [Project Scope](docs/scope.md) — domain model, functional requirements, service breakdown, and scope tiers
- [Build Plan](docs/build-plan.md) — phased build sequence, principles, and repository layout
- [Event & Messaging Design](docs/event-design.md) — Kafka topology, standard envelope, outbox pattern, and idempotency
- [Architecture Decision Records](docs/adr/README.md) — key design decisions and their rationale
