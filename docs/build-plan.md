# Claims Intelligence Platform — Tier 1 Build Plan

*How to build the core vertical slice incrementally, keeping something runnable at every step.*

---

## 1. What Tier 1 delivers (definition of done)

Tier 1 is complete when both intake paths work end to end, locally:

- A vet logs a session, a clinic manager verifies it, and it auto-generates a claim.
- A clinic manager submits a manual claim directly.
- Every claim is fraud-scored and triaged — verified-low-risk auto-adjudicates, manual or high-risk routes to review.
- All of it is visible in the read model and the clinic workbench UI.
- It runs locally via `docker compose up`, with a README, the architecture diagram, and a handful of ADRs.

This alone demonstrates database-per-service, dual intake, event-driven design, CQRS, triage/routing, logic-in-code, and a polyglot stack.

---

## 2. Build principles

- **Vertical slices, not horizontal layers.** Get one thin path working through the whole stack early, then widen it.
- **Walking skeleton first.** A minimal end-to-end path that exercises the architecture beats a perfectly finished single service.
- **Runnable at every step.** Each phase ends with something you can curl, or later click.
- **Start simple, harden later.** Begin with naive publish-after-save and plain JSON; introduce the outbox, schema registry, and idempotency as deliberate upgrades once messages flow. Each upgrade becomes an ADR and an interview story.

The eight phases below cluster into the five milestones in the roadmap (Foundations; First event flow; Core claim path + fraud; Read side + UI; Harden).

---

## 3. Repository layout

A **monorepo** is recommended for a solo build — one clone, one `docker-compose`, easy cross-service changes — while each service stays fully independent (own build, own Dockerfile, own database) so it could be split to its own repo later. Independence of the *services* is what matters for the architecture story, not the number of repos.

```
claims-intelligence-platform/
  compose.yml
  contracts/                 # shared event schemas (JSON first, Avro later)
  services/
    enrollment-policy/       # Java / Spring Boot
    sessions/                # Java / Spring Boot
    claims/                  # Java / Spring Boot  (Insurer Submission lives here as a module)
    fraud/                   # Go
    projection/              # Node / NestJS
    bff/                     # Node / NestJS
  web/
    clinic-workbench/        # React + Vite + TypeScript
  docs/
    adr/                     # architecture decision records
    diagrams/
  README.md
```

---

## 4. Local infrastructure

Use **Docker Compose** for development, not Kubernetes — Kubernetes (Kind/Minikube) is a Tier 3 concern once the system runs. Compose brings up:

- **Kafka** — a single broker. Consider **Redpanda**, a Kafka-compatible single-binary broker with a built-in console; it removes most of the local Kafka setup friction and is ideal for a learning build.
- **PostgreSQL** — one database (or one schema) per Java service; never shared.
- **MongoDB** — the read model for the Projection service.
- **Redis** — idempotency keys and caching (used from Phase 7).

---

## 5. Tech stack per service

| Service | Stack | Notes |
|---|---|---|
| Enrollment & Policy | Java / Spring Boot, Spring Data JPA, PostgreSQL | Your strongest ground; start here |
| Sessions | Java / Spring Boot, JPA, PostgreSQL | Spring for Apache Kafka for events |
| Claims | Java / Spring Boot, JPA, PostgreSQL | State machine + rules engine in code |
| Fraud | Go, a Kafka client (`kafka-go` or `confluent-kafka-go`) | Your Go practice ground; keep it lean |
| Projection | Node / NestJS, KafkaJS, Mongoose | DI and structure you liked in NestJS |
| BFF | Node / NestJS | Auth, aggregation, command dispatch |
| Clinic workbench | React + Vite + TypeScript, TanStack Query | Reads via the BFF |

---

## 6. Phased build sequence

### Phase 0 — Foundations
- **Build:** the monorepo skeleton; `compose.yml` with Kafka/Redpanda, Postgres, Mongo, Redis; a `/health` endpoint per service stub; a basic CI workflow (build + test).
- **Runnable:** `docker compose up` brings up infra; each stub answers on `/health`.
- **Status:** Complete.

### Phase 1 — Walking skeleton: Enrollment & Policy
- **Build:** the Enrollment & Policy service with its own Postgres; REST + JPA for clinics, pets, vets, catalogue, and policies; the vet approval workflow (pending → approved/rejected). No Kafka yet.
- **Runnable:** register clinics, pets, and vets, and approve a vet, via curl/Postman.
- **Status:** Complete.

### Phase 2 — First event flow: + Sessions + Kafka
- **Build:** Enrollment publishes domain events (naive publish-after-save, plain JSON) to `cip.enrollment`; the Sessions service consumes them to maintain its local master-data slice; Sessions exposes session logging and human verification, publishing `session.logged` and `session.verified`.
- **Runnable:** enrol a pet and watch Sessions receive it; a vet logs a session and a clinic manager verifies it.
- **Status:** Complete.

### Phase 3 — Core claim path: + Claims
- **Build:** Claims consumes `session.verified` and assembles a claim; a simple rules engine auto-adjudicates; the lifecycle **state machine**; the manual-claim submission command. Fraud is stubbed (assume low risk) for now.
- **Runnable:** a verified session becomes an assembled, then adjudicated, claim; a manual claim adjudicates too.
- **Status:** Complete.

### Phase 4 — Fraud + triage (Go enters)
- **Build:** the Fraud service in Go consumes `claim.assembled`, scores it (rules first), and publishes `claim.fraud-scored`; Claims uses the score for triage — auto-adjudicate vs route-to-review.
- **Runnable:** low-risk claims auto-adjudicate; high-risk or manual claims land in a review state.
- **Status:** Complete.

### Phase 5 — Read side (CQRS): + Projection + BFF
- **Build:** the Projection service (NestJS) consumes all topics and builds the MongoDB read model (denormalised claim/session/pet views); the BFF (NestJS) reads the view store and dispatches commands to the owning services.
- **Runnable:** query consolidated claim views through the BFF; commands route to the correct service.
- **Status:** Complete.

### Phase 6 — UI: clinic workbench
- **Build:** the React workbench — verify sessions, submit manual claims, work the review queue, and a simple dashboard, all through the BFF. In practice this phase is wide enough to need its own sub-phases:
  - **6a** — Foundations: routing, dummy auth, API layer, layout. *Complete.*
  - **6b** — Session verification. *Complete.*
  - **6c** — Manual claim submission. *Complete.*
  - **6d** — Review queue (adjuster-only `PENDING_REVIEW` view), plus the adjuster approve/reject action to resolve a claim. *Complete.*
  - **6e** — Dashboard (stat cards linking to list views). *Complete.*
  - **6f** — Master data UI: clinic/vet registration forms, added mid-build since master data otherwise has no UI. *Not started.*
- **Runnable:** the entire Tier 1 path demoable in a browser.
- **Status:** In progress (6a–6e complete).

### Phase 7 — Harden (the deliberate upgrades)
- **Build:** the transactional outbox + relay (polling first, optionally Debezium) replacing naive publish; Schema Registry + Avro replacing ad-hoc JSON; idempotent consumers (dedup by `eventId`) and dead-letter topics; real auth (reuse your Spring Authorization Server + PKCE) replacing the stub; basic observability (structured logs, tracing); proper Kafka batch compression support across consumers, replacing the interim "producers publish uncompressed" fix — either swap Alpine-based JVM images for glibc-based ones (or add `gcompat`) so `snappy-java` can load its native lib, and add a codec package (e.g. `kafkajs-snappy`) to the Node consumers, or standardise on a codec everyone actually supports.
- **Note:** discovered while verifying Phase 3's fraud-triage routing (2026-07-07) — the fraud-detection service's default Snappy compression crashed both the Claims (Alpine musl) and Projection (kafkajs, no codec installed) consumers. Worked around by disabling compression on the Go producer; revisit here.
- **Runnable:** identical behaviour, now reliable — and each change is an ADR.
- **Status:** Not started.

---

## 7. Portfolio artifacts to produce alongside

- **ADRs** for each major decision: database-per-service, CQRS read model, dual intake, Go for fraud, the outbox. A few paragraphs each; they show judgement.
- **README** with the architecture diagram, the run instructions, and a short tour of the flow.
- **The diagrams** already produced (architecture, dual-intake pipeline, event flow, this roadmap).

---

## 8. Explicitly out of scope for Tier 1

Payment, Insurer Submission (beyond a stub), Document service and AI features, Notification, Kubernetes and Terraform, and the Analytics Engine. These are Tier 2 and 3.

---

## 9. References

- Alistair Cockburn — the "walking skeleton".
- Martin Fowler — vertical slices and the Strangler Fig pattern (martinfowler.com).
- Redpanda (redpanda.com) — lightweight local Kafka-compatible broker.
- Chris Richardson, microservices.io — the patterns underpinning each phase.
