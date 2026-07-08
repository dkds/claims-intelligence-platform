# Claims Intelligence Platform — Event & Messaging Design (Kafka)

*The messaging contract and reliability design for the Tier 1 services. Companion to the v2 scope.*

---

## 1. Principles and headline decisions

1. **One topic per aggregate stream**, keyed by the aggregate id, so all events about one entity stay ordered on one partition.
2. **A standard event envelope** wraps every payload, carrying an event ID (for idempotency), version, timestamps, tenant, and correlation/causation IDs (for tracing).
3. **Event-carried state transfer**: events include enough denormalised data for consumers to act without calling back to the producer — but not the entire aggregate.
4. **Avro + Schema Registry** for enforced, cross-language contracts (recommended), with a JSON-first option for a faster start.
5. **Reliable publishing via the transactional outbox**: the state change and the outbox row commit in one transaction; a relay publishes to Kafka.
6. **At-least-once delivery + idempotent consumers = effectively-once processing**; deduplicate by event ID.
7. **One consumer group per service**; instances of a service share its group for partition-parallel consumption.
8. **Dead-letter topics and bounded retries** for poison messages.

---

## 2. Serialization and schema contracts

The system is polyglot (Java, Go, Node), so a registry-enforced binary contract prevents producers and consumers drifting apart.

- **Recommended:** Avro with the Confluent Schema Registry. Avro is the most idiomatic Kafka format and has solid libraries in all three languages; the registry enforces compatibility and stores schemas centrally.
- **Equally valid:** Protobuf, which has especially strong Go tooling — a reasonable choice given the fraud service is in Go.
- **Simplest start:** JSON with a documented JSON Schema. Easiest to read and debug, no registry needed initially, at the cost of no enforced contract. A pragmatic path is JSON-first, then introduce the registry once the flow works.

Compatibility mode: **BACKWARD** (new consumers can read old events). Add only optional fields; never repurpose or remove a field in place.

---

## 3. Standard event envelope

Every message is an envelope plus a typed payload:

```json
{
  "eventId": "0f9b2c7e-8a4d-4f1e-9c2a-2b6d5e1f7a90",
  "eventType": "claim.assembled",
  "eventVersion": 1,
  "occurredAt": "2026-05-22T09:14:03Z",
  "producer": "claims-service",
  "tenantId": "clinic-3185",
  "aggregateType": "claim",
  "aggregateId": "claim-77421",
  "correlationId": "req-5d2a...",
  "causationId": "0a1b...event-that-caused-this",
  "payload": { }
}
```

- `eventId` — unique per event; the deduplication key for idempotent consumers.
- `correlationId` — constant across a whole business flow (e.g. one claim's journey), for tracing.
- `causationId` — the id of the event/command that directly caused this one, for reconstructing causal chains.
- `tenantId` — the clinic; lets consumers enforce tenant isolation.

---

## 4. Topic topology

Names use a `cip.<aggregate>.v<major>` convention; the version suffix changes only on a breaking schema change. Start at **6 partitions** for active streams (claims, sessions, fraud) and **3** for low-volume ones (enrollment, documents); partitions can be increased later, but doing so disturbs key-to-partition mapping, so leave headroom.

| Topic | Partition key | Producer | Key events |
|---|---|---|---|
| `cip.enrollment.v1` | entity id (clinic/pet/vet/policy) | Enrollment & Policy | `clinic.registered`, `pet.enrolled`, `policy.assigned`, `vet.registered`, `vet.approved`, `catalogue.updated` |
| `cip.sessions.v1` | sessionId | Sessions | `session.logged`, `session.verified` |
| `cip.claims.v1` | claimId | Claims | `claim.assembled`, `claim.adjudicated`, `claim.rejected`, `claim.routed-to-review`, `claim.ready-for-submission` |
| `cip.fraud.v1` | claimId | Fraud | `claim.fraud-scored` |
| `cip.payments.v1` | paymentId | Payment | `payment.requested`, `payment.completed`, `payment.failed` |
| `cip.submissions.v1` | claimId | Insurer Submission (module) | `claim.submitted`, `claim.accepted`, `claim.declined` |
| `cip.documents.v1` | documentId | Document | `document.processed` |

Each topic also has a companion `*.DLT` dead-letter topic (Section 9).

---

## 5. Event catalogue — example schemas

Payloads only (the envelope wraps each). Representative, not exhaustive.

**`session.verified`** (Sessions → consumed by Claims, Projection)
```json
{
  "sessionId": "sess-9920",
  "petId": "pet-4471",
  "vetId": "vet-220",
  "clinicId": "clinic-3185",
  "verifiedBy": "user-coord-58",
  "verifiedAt": "2026-05-22T09:10:00Z",
  "lines": [
    { "procedureCode": "DENT-CLEAN", "quantity": 1, "notes": "routine" },
    { "procedureCode": "ANALGESIA", "quantity": 1 }
  ]
}
```

**`claim.assembled`** (Claims → consumed by Fraud, Projection)
```json
{
  "claimId": "claim-77421",
  "clinicId": "clinic-3185",
  "petId": "pet-4471",
  "origin": "session",
  "sourceSessionId": "sess-9920",
  "policyId": "pol-1002",
  "lines": [ { "procedureCode": "DENT-CLEAN", "quantity": 1, "requestedAmount": 120.00 } ],
  "totalRequested": 120.00,
  "submittedBy": "user-coord-58",
  "assembledAt": "2026-07-06T10:15:00Z"
}
```
For a manual claim, `origin` is `"manual"` and `sourceSessionId` is absent.

**`claim.fraud-scored`** (Fraud → consumed by Claims, Projection)
```json
{
  "claimId": "claim-77421",
  "score": 0.18,
  "riskLevel": "low",
  "flags": [],
  "modelVersion": "rules-2026.04",
  "scoredAt": "2026-05-22T09:10:30Z"
}
```

**`claim.routed-to-review`** (Claims → consumed by Projection)
```json
{
  "claimId": "claim-77421",
  "reasons": ["Fraud risk level: high"],
  "routedBy": "auto",
  "origin": "session",
  "updatedAt": "2026-05-22T09:10:31Z"
}
```
Manual claims are always routed to review (`reasons: ["Manual claims require adjuster review"]`), regardless of fraud score.

**`claim.adjudicated`** (Claims → consumed by Projection, Submission module, Payment via saga)
```json
{
  "claimId": "claim-77421",
  "decision": "approved",
  "approvedAmount": 120.00,
  "reasons": [],
  "adjudicatedBy": "auto",
  "origin": "session",
  "sourceSessionId": "sess-9920"
}
```
`adjudicatedBy` (and `claim.rejected`'s `rejectedBy`) is `"auto"` for the rules-engine path, or the adjuster's user id when a `PENDING_REVIEW` claim is resolved manually via `POST /claims/{id}/approve|reject`.

**`payment.completed`** (Payment → consumed by Projection, Notification)
```json
{
  "paymentId": "pay-5510",
  "vetId": "vet-220",
  "claimId": "claim-77421",
  "amount": 84.00,
  "currency": "USD",
  "status": "completed"
}
```

---

## 6. Partitioning and ordering

- Keying by the aggregate id guarantees ordering **per entity within a topic** — all events for `claim-77421` on `cip.claims.v1` are processed in order.
- There is **no ordering across topics**, and that is fine: each consumer reacts to an event and updates its own state. The Claims service's state machine, not message order across services, is the source of truth for claim status.
- The cross-service claim events (`claim.fraud-scored`, submissions) are keyed by `claimId` so a per-claim consumer stays ordered.
- Consumer parallelism is capped by partition count: with 6 partitions, up to 6 instances of a service consume in parallel.

---

## 7. Consumer groups

One group per service (`group.id = cip-<service>`). Instances of a service share its group; each topic-partition is delivered to exactly one instance in the group.

| Group | Subscribes to | Purpose |
|---|---|---|
| `cip-sessions` | enrollment | maintain local master-data slice |
| `cip-claims` | sessions, enrollment, fraud + manual-claim commands | assemble, triage, adjudicate |
| `cip-fraud` | claims | score assembled claims |
| `cip-payment` | sessions (via saga) | pay vets for verified sessions |
| `cip-submission` | claims | submit ready claims to the insurer |
| `cip-projection` | all topics | build the read model |
| `cip-notification` | claims, payments, submissions | dispatch notifications |
| `cip-document` | document commands | process uploads |

The **saga** for verified-session-to-payment is an orchestration: a coordinator (initially inside the Payment service) consumes `session.verified`, issues the payment, tracks saga state, and compensates on failure.

---

## 8. Reliable publishing — transactional outbox

A service must not "save to the database, then publish to Kafka" as two steps — a crash between them loses or duplicates events. Instead:

1. In **one database transaction**, write the domain change *and* insert a row into an `outbox` table (the serialized event).
2. A **relay** reads new outbox rows and publishes them to Kafka, marking them sent.

Two relay options:

- **Polling publisher (recommended start):** a small scheduled job in the service polls unsent outbox rows and publishes them. Language-agnostic, easy to reason about, easy to demo.
- **Debezium CDC (showcase upgrade):** Debezium tails the database transaction log via Kafka Connect and streams outbox inserts to Kafka with no polling. A strong portfolio signal; introduce it once the pattern is proven with polling.

The event flow end to end is shown in the diagram in the chat: atomic write, relay, publish, consume, idempotent apply.

---

## 9. Idempotent consumers, delivery, and errors

- **Delivery is at-least-once.** Consumers must therefore be idempotent: before applying an event, check whether its `eventId` has been seen.
- **Recommended dedup store:** a `processed_events` table written in the **same transaction** as the consumer's state change — atomic, no race. Use Redis with a TTL only for lightweight consumers that do not write to a database.
- **Effectively-once** processing is the result: at-least-once delivery plus idempotent apply. (Kafka exactly-once semantics with transactions exists but is unnecessary complexity here.)
- **Errors:** retry transient failures with bounded backoff; after the limit, route the message to the topic's `*.DLT` for inspection and replay. Keep poison messages out of the main partition so they do not block the queue.

---

## 10. Schema evolution and versioning

- Evolve within a major version by adding **optional** fields only (BACKWARD compatibility).
- A breaking change bumps the topic's major version (`cip.claims.v2`); run old and new in parallel during migration.
- The `eventVersion` field lets consumers branch if they must support more than one shape briefly.

---

## 11. Open choices to confirm

1. **Contract format:** Avro + Schema Registry (recommended) vs Protobuf vs JSON-first.
2. **Outbox relay:** polling publisher first (recommended) vs straight to Debezium CDC.
3. **Idempotency store:** `processed_events` table (recommended) vs Redis with TTL.
4. **Topic granularity:** per-aggregate stream as proposed — confirm, or split/merge any.

---

## 12. References

- Confluent — Apache Kafka and Schema Registry documentation (docs.confluent.io).
- Debezium — change-data-capture for the outbox (debezium.io).
- Chris Richardson, microservices.io — Transactional Outbox, Saga, Event-Driven patterns.
- Martin Fowler — event-carried state transfer (martinfowler.com/articles/201701-event-driven.html).
