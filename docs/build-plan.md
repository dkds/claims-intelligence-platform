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
  docker-compose.yml
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

Use **docker-compose** for development, not Kubernetes — Kubernetes (Kind/Minikube) is a Tier 3 concern once the system runs. Compose brings up:

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
- **Build:** the monorepo skeleton; `docker-compose` with Kafka/Redpanda, Postgres, Mongo, Redis; a `/health` endpoint per service stub; a basic CI workflow (build + test).
- **Runnable:** `docker compose up` brings up infra; each stub answers on `/health`.

### Phase 1 — Walking skeleton: Enrollment & Policy
- **Build:** the Enrollment & Policy service with its own Postgres; REST + JPA for clinics, pets, vets, catalogue, and policies; the vet approval workflow (pending → approved/rejected). No Kafka yet.
- **Runnable:** register clinics, pets, and vets, and approve a vet, via curl/Postman.

### Phase 2 — First event flow: + Sessions + Kafka
- **Build:** Enrollment publishes domain events (naive publish-after-save, plain JSON) to `cip.enrollment`; the Sessions service consumes them to maintain its local master-data slice; Sessions exposes session logging and human verification, publishing `session.logged` and `session.verified`.
- **Runnable:** enrol a pet and watch Sessions receive it; a vet logs a session and a clinic manager verifies it.

### Phase 3 — Core claim path: + Claims
- **Build:** Claims consumes `session.verified` and assembles a claim; a simple rules engine auto-adjudicates; the lifecycle **state machine**; the manual-claim submission command. Fraud is stubbed (assume low risk) for now.
- **Runnable:** a verified session becomes an assembled, then adjudicated, claim; a manual claim adjudicates too.

### Phase 4 — Fraud + triage (Go enters)
- **Build:** the Fraud service in Go consumes `claim.assembled`, scores it (rules first), and publishes `claim.fraud-scored`; Claims uses the score for triage — auto-adjudicate vs route-to-review.
- **Runnable:** low-risk claims auto-adjudicate; high-risk or manual claims land in a review state.

### Phase 5 — Read side (CQRS): + Projection + BFF
- **Build:** the Projection service (NestJS) consumes all topics and builds the MongoDB read model (denormalised claim/session/pet views); the BFF (NestJS) reads the view store and dispatches commands to the owning services.
- **Runnable:** query consolidated claim views through the BFF; commands route to the correct service.

### Phase 6 — UI: clinic workbench
- **Build:** the React workbench — verify sessions, submit manual claims, work the review queue, and a simple dashboard, all through the BFF.
- **Runnable:** the entire Tier 1 path demoable in a browser.

### Phase 7 — Harden (the deliberate upgrades)
- **Build:** the transactional outbox + relay (polling first, optionally Debezium) replacing naive publish; Schema Registry + Avro replacing ad-hoc JSON; idempotent consumers (dedup by `eventId`) and dead-letter topics; real auth (reuse your Spring Authorization Server + PKCE) replacing the stub; basic observability (structured logs, tracing).
- **Runnable:** identical behaviour, now reliable — and each change is an ADR.

---

## 7. Suggested rhythm (loose, ~4 weeks part-time)

- **Week 1:** Phases 0–1.
- **Week 2:** Phases 2–3.
- **Week 3:** Phases 4–5.
- **Week 4:** Phase 6, then begin Phase 7.

Hardening (Phase 7) is ongoing and partly optional; the system is demoable after Phase 6. Adjust freely — the order matters more than the dates.

---

## 8. Portfolio artifacts to produce alongside

- **ADRs** for each major decision: database-per-service, CQRS read model, dual intake, Go for fraud, the outbox. A few paragraphs each; they show judgement.
- **README** with the architecture diagram, the run instructions, and a short tour of the flow.
- **The diagrams** already produced (architecture, dual-intake pipeline, event flow, this roadmap).

---

## 9. Explicitly out of scope for Tier 1

Payment, Insurer Submission (beyond a stub), Document service and AI features, Notification, Kubernetes and Terraform, and the Analytics Engine. These are Tier 2 and 3.

---

## 10. References

- Alistair Cockburn — the "walking skeleton".
- Martin Fowler — vertical slices and the Strangler Fig pattern (martinfowler.com).
- Redpanda (redpanda.com) — lightweight local Kafka-compatible broker.
- Chris Richardson, microservices.io — the patterns underpinning each phase.
