# Claims Intelligence Platform — Project Scope v2

*Domain: veterinary insurance claims. A portfolio system for demonstrating event-driven, polyglot microservices done well.*

---

## 1. Overview

The platform is a third-party claims administrator sitting between veterinary clinics and an insurer. Clinics register their pets (the insured patients) and their veterinarians. Vets deliver treatment, and reimbursement claims reach the insurer through **two intake paths**:

- **Session-derived claims** — a vet logs treatment sessions, a clinic manager verifies them (AI-assisted), and verified sessions auto-generate claim lines.
- **Manually submitted claims** — a clinic manager submits claims directly for non-session items such as medication, transport, food, or boarding.

Both paths converge into one pipeline: every claim is fraud/risk-scored, then triaged — verified low-risk claims auto-adjudicate, while manual or high-risk claims go to adjuster review — before submission to the insurer. Verified sessions additionally drive an internal payment to the vet.

The platform is a deliberately idealised re-architecture that fixes two anti-patterns from a real system: a database shared across services, and business logic encoded in the database. The domain and all data are **generic and synthetic**; no employer schemas, names, rules, or code are reproduced.

---

## 2. Goals and non-goals

### Goals
- Database-per-service with no shared persistence.
- Business logic in application code (rich domain model and rules engine), not in the database.
- An event-driven backbone (Kafka) with reliable publishing (transactional outbox) and CQRS read models.
- Polyglot, right-tool-per-job services (Java, Go, Node/TypeScript, React).
- Practical, demoable AI: document intelligence, an AI verification assist, and ML-assisted fraud scoring.
- Clear interview artefacts: an architecture diagram, an ADR per major decision, and one working end-to-end vertical slice.

### Non-goals
- Not production-grade compliance, security hardening, or real payment/insurer rails — these are simulated.
- Not an exhaustive insurance rule set — a representative set is enough to exercise the engine.
- The **Analytics Engine** is a separate, independent module (own repository) that plugs in as a Kafka consumer; it is scoped separately.

---

## 3. Terminology and roles

| Generic role | Veterinary term | Notes |
|---|---|---|
| Operator / platform | The platform (claims administrator) | The system itself |
| Tenant organisation | Veterinary clinic | The registered org |
| Organisation manager | **Clinic manager** | Registers pets and vets, verifies sessions, submits manual claims |
| Provider | **Veterinarian (vet)** | Delivers treatment sessions |
| Member | Pet | The insured patient |
| Policyholder | Pet owner | Holds the policy; read-only access (later) |
| External payer | Insurer | External system |

**User roles (authorization):** admin, clinic manager, veterinarian, and — added later — pet owner (read-only, for statements and reports). See NFR-10 for the model.

---

## 4. Domain model (key entities)

| Entity | Description |
|---|---|
| Clinic | A registered veterinary practice; the tenant. |
| Pet | An insured patient enrolled by a clinic. |
| Owner | The pet's owner / policyholder. |
| Veterinarian | A provider who delivers treatment and is paid for verified sessions. |
| Policy | Coverage attached to a pet, defining what treatments are reimbursable. |
| Coverage period | The time context governing eligibility and reporting. |
| Treatment catalogue item | A billable treatment/procedure with a code and reimbursement rate. |
| Session (encounter) | A vet-logged treatment event, with notes, meds, and procedure codes; the source of session-derived claims. |
| Claim | A reimbursement request, originating from verified sessions or manual submission. |
| Claim line | An individual item within a claim. |
| Payment | An internal disbursement to a vet for verified sessions. |
| Document | Supporting evidence attached to a session or claim. |
| User | Account with a role and per-user permission overrides. |

---

## 5. Functional requirements

**Registration & master data**
- FR-1: Admins/clinic managers register clinics, veterinarians, and treatment-catalogue items.
- FR-2: Clinic managers enrol pets and assign each to one or more coverage policies based on need.

**Vet onboarding**
- FR-3: A clinic manager registers a vet as `pending`; an admin reviews credentials and approves or rejects.
- FR-4: On approval, a vet account is provisioned (login created in the auth server, linked to the vet record).

**Session logging & verification**
- FR-5: A vet logs treatment sessions (notes, meds, procedure codes) against a pet.
- FR-6: A clinic manager reviews and verifies sessions; an AI assist summarises notes, suggests procedure codes, and flags concerns to speed review.
- FR-7: Verified sessions auto-generate claim lines.

**Manual claim submission**
- FR-8: A clinic manager submits claims directly for non-session items (medication, transport, food, boarding), bypassing sessions.

**Fraud / risk scoring**
- FR-9: Every claim is scored for fraud/anomaly risk (rules plus ML) — as an assist for session-derived claims and as the primary gate for manual claims.

**Triage & routing**
- FR-10: Each claim is routed by verification level and risk: verified-and-low-risk to auto-adjudication, manual or high-risk to adjuster review.

**Adjudication & review**
- FR-11: A rules engine auto-adjudicates eligible claims (approve, partial, reject) and computes the reimbursable amount.
- FR-12: Flagged claims appear in an adjuster queue for human approve/reject/request-info, with reasons recorded.
- FR-13: The claim lifecycle is an explicit state machine with recorded transition history.

**Document intelligence**
- FR-14: Users upload supporting documents; structured fields are extracted via an AI/OCR API and linked to the session or claim.

**Payment & submission**
- FR-15: Verified sessions trigger an internal, idempotent payment to the vet.
- FR-16: Adjudicated claims are submitted to the insurer through a reliable, idempotent, retrying adapter (anti-corruption layer); manual claims produce an insurer claim only (no vet payment).

**SLA & analytics**
- FR-17: Claims unprocessed beyond a threshold are flagged and escalated.
- FR-18: Near-real-time operational views are served from pre-aggregated read models produced by the independent Analytics Engine.

---

## 6. Non-functional & architectural requirements

- **NFR-1 (Database per service):** each service owns its store privately; no other service touches it directly.
- **NFR-2 (Logic in code):** business rules live in a rich domain model and a rules engine, not in stored procedures or schema.
- **NFR-3 (Event-driven):** services communicate via domain events on Kafka; published state changes use the **Transactional Outbox** pattern.
- **NFR-4 (CQRS read model):** a single projection service maintains a read-only, denormalised view store, read through the BFF and written by no one else.
- **NFR-5 (Consistency):** decisions read strongly-consistent local data; display and reporting tolerate eventual consistency.
- **NFR-6 (Saga):** the verified-session-to-vet-payment flow is an orchestration-based saga with compensation on failure.
- **NFR-7 (Idempotency):** all event consumers, payments, and insurer submissions are idempotent (Redis dedup keys).
- **NFR-8 (Anti-corruption layer):** the insurer integration is isolated behind an adapter that translates internal models to the external contract and handles retries.
- **NFR-9 (AI as external dependency):** AI/OCR and LLM calls are isolated behind a thin client so they can be stubbed, swapped, or rate-limited; a shared AI-gateway library is a candidate reusable component.
- **NFR-10 (Authorization):** role-based access (admin, clinic manager, vet; later pet owner) granting default permissions, with per-user grant/deny overrides, plus tenant isolation as a separate dimension (a clinic manager sees only their clinic; a vet only their assigned pets). Optionally externalised to a policy engine (OPA or Casbin).
- **NFR-11 (Observability):** structured logging, metrics, distributed tracing.
- **NFR-12 (Deployment):** local Kubernetes (Kind/Minikube), CI/CD, Terraform on AWS as a stretch goal.
- **NFR-13 (Security):** OAuth2 / OIDC with JWTs (reuse your Spring Authorization Server + PKCE work).

---

## 7. Service-by-service breakdown

"Owns" means a private database. Tiers are defined in Section 9.

### 7.1 Enrollment & Policy Service — *Java / Spring Boot · Tier 1*
- **Owns (PostgreSQL):** clinics, pets, owners, veterinarians, treatment catalogue, policies, coverage periods, eligibility. System of record for all master data.
- **Publishes:** `clinic.registered`, `pet.enrolled`, `policy.assigned`, `vet.registered`, `vet.approved`, `catalogue.updated`
- **Key logic:** enrolment and eligibility rules; the vet approval workflow (pending → approved/rejected); publishing master-data changes.

### 7.2 Sessions Service — *Java / Spring Boot · Tier 1 (AI assist Tier 2)*
- **Owns (PostgreSQL):** sessions, verification state and history.
- **Publishes:** `session.logged`, `session.verified`
- **Consumes:** master-data events (for its local decision slice)
- **Key logic:** session capture, the verification gate, and the AI verification assist (note summary, suggested codes, risk flags). Emits `session.verified`, which the Claims service turns into claim lines.

### 7.3 Claims Service — *Java / Spring Boot · Tier 1*
- **Owns (PostgreSQL):** claims, claim lines, claim origin (session-derived vs manual), lifecycle state, transition history.
- **Publishes:** `claim.assembled`, `claim.adjudicated`, `claim.rejected`, `claim.routed-to-review`, `claim.ready-for-submission`
- **Consumes:** `session.verified` (generate claim lines), manual-claim commands, `claim.fraud-scored` (for triage)
- **Key logic:** the heart of the system — claim assembly from both origins, validation, **triage & routing** by verification level and risk, the **rules engine** for auto-adjudication, the lifecycle **state machine**, and SLA timers.

### 7.4 Fraud Detection Service — *Go · Tier 1*
- **Owns:** fraud rules and computed scores/history.
- **Publishes:** `claim.fraud-scored`
- **Consumes:** `claim.assembled`
- **Key logic:** high-throughput rules-plus-ML scoring (frequency anomalies, duplicates, abnormal service mix). Primary gate for manual claims, assist for session-derived ones.

### 7.5 Projection (View) Service — *Node.js / TypeScript · Tier 1*
- **Owns (MongoDB):** the denormalised read model (full claim, pet, vet, session, and document views).
- **Consumes:** all relevant domain events
- **Key logic:** the single writer to the view store; idempotent, order-tolerant projections optimised for the portals.

### 7.6 BFF / API Gateway — *Node.js / TypeScript · Tier 1*
- **Owns:** none (Redis for caching/sessions)
- **Key logic:** auth, request aggregation, serving the React portals. Reads from the view store; dispatches commands to owning services.

### 7.7 Payment Service — *Node.js / TypeScript · Tier 2*
- **Owns (PostgreSQL):** vet payments, status, ledger; Redis for idempotency.
- **Publishes:** `payment.requested`, `payment.completed`, `payment.failed`
- **Consumes:** `session.verified` (via the saga coordinator)
- **Key logic:** idempotent vet payment orchestration; saga participant with compensation.

### 7.8 Insurer Submission Service — *Node.js / TypeScript · Tier 2*
- **Owns (PostgreSQL):** submission records, external statuses.
- **Consumes:** `claim.ready-for-submission`
- **Key logic:** the anti-corruption layer to the insurer — batches/maps claims to the external contract, submits reliably and idempotently, handles retries and reconciliation. *(For a leaner build this can start as a module inside the Claims service and split out later.)*

### 7.9 Document Service — *Node.js / TypeScript · Tier 2*
- **Owns (MongoDB + object storage):** document metadata and extracted fields; files in object storage.
- **Publishes:** `document.processed`
- **Key logic:** stores uploads, calls the AI/OCR API to extract structured fields, links results to sessions/claims.

### 7.10 Notification Service — *Go · Tier 3 (optional)*
- **Consumes:** key lifecycle events
- **Key logic:** email/in-app notifications; Redis rate-limiting.

### 7.11 React Portals — *React + TypeScript*
- **Clinic workbench (Tier 1):** verify sessions, submit manual claims, work the adjuster review queue, view dashboards.
- **Owner portal (Tier 3):** read-only statements and claim status.

### 7.12 Analytics Engine — *separate module (own repository)*
Plugs in as a Kafka consumer to produce near-real-time operational views. Scoped separately.

---

## 8. Event & topic design (high-level)

- `enrollment-events` — `clinic.registered`, `pet.enrolled`, `policy.assigned`, `vet.registered`, `vet.approved`, `catalogue.updated`
- `session-events` — `session.logged`, `session.verified`
- `claim-events` — `claim.assembled`, `claim.adjudicated`, `claim.rejected`, `claim.routed-to-review`, `claim.ready-for-submission`
- `fraud-events` — `claim.fraud-scored`
- `payment-events` — `payment.requested`, `payment.completed`, `payment.failed`
- `submission-events` — `claim.submitted`, `claim.accepted`, `claim.declined`
- `document-events` — `document.processed`

Each event carries an event ID (idempotency), a key (usually claim or pet ID, for partition ordering), a timestamp, and a versioned payload. Detailed schemas, partition keys, and consumer groups are the next deliverable.

---

## 9. Recommended scope tiers

**Tier 1 — Core vertical slice (the showcase):** Enrollment & Policy, Sessions (human verification), Claims (assembly from both origins + triage + adjudication), Fraud (Go), Projection + view store, BFF, and the clinic workbench — wired through Kafka with the outbox and a CQRS read model. Demonstrates database-per-service, dual intake, event-driven design, CQRS, triage/routing, and polyglot services end to end.

**Tier 2 — Workflow & intelligence depth:** Payment service with the session-to-payment saga; Insurer Submission with the anti-corruption layer; Document service plus the AI verification assist and document intelligence.

**Tier 3 — Breadth & polish:** Notification service, owner portal and role, SLA escalation, full Terraform/AWS deployment, and Analytics Engine integration.

---

## 10. Structural decisions to confirm

These are the points where the structure could still change — worth deciding before building:

- **Sessions as a separate service** (proposed) vs folding sessions into the Claims service as a second aggregate. Separate gives cleaner bounded contexts and a natural home for the AI assist; folding reduces the service count for a solo build.
- **Insurer Submission as a separate service** (proposed, Tier 2) vs a module inside Claims. Separate isolates the external dependency and showcases the anti-corruption layer; a module is simpler initially.
- **AI placement:** keep document intelligence in the Document service and the verification assist in the Sessions service, behind a thin shared AI-client (a candidate reusable library), vs a single dedicated Intelligence service.
- **Projection language:** Node/TypeScript (decided) for stack breadth.
- **Service count:** the full decomposition is large; Tier 1 is the only must-build, and several services can be merged if you want a leaner first pass.

---

## 11. Notes on data & intellectual property

Use only synthetic data and the generic veterinary surface. Do not copy employer schemas, rules, identifiers, or code. The value for interviews is the architecture and your reasoning.

---

## 12. References

- Chris Richardson, *Microservices Patterns* and microservices.io — Database per Service, Saga, CQRS, API Composition, Transactional Outbox, Anti-Corruption Layer.
- Martin Fowler — CQRS (martinfowler.com/bliki/CQRS.html) and Anemic Domain Model.
- Eric Evans, *Domain-Driven Design* — bounded contexts and the rich domain model.
- Open Policy Agent (openpolicyagent.org) or Casbin (casbin.org) — externalised authorization.
