# ADR-004: Transactional outbox for reliable event publishing

**Status:** Accepted

**Context:**
A service that saves a state change to its database and then publishes an event to Kafka faces a dual-write problem: if the database write succeeds but the Kafka publish fails (or vice versa), the system enters an inconsistent state. This cannot be solved by wrapping both in a distributed transaction without unacceptable complexity and performance cost.

**Decision:**
Each service that publishes events uses the Transactional Outbox pattern. The event is written to an `outbox` table in the service's own database within the same local transaction as the business data change. A separate relay process — either a poller or a CDC connector (Debezium) — reads the outbox table and publishes events to Kafka.

**Consequences:**
- The business data change and the event record are atomically committed. No event is lost if the transaction succeeds.
- If Kafka is unavailable, events accumulate in the outbox table and are published when Kafka recovers. Business operations are never blocked by Kafka downtime.
- Kafka ceases to be a single point of failure for data integrity — it becomes a single point of failure for timeliness only (see ADR-009).
- The relay process introduces a small latency between the database commit and the event appearing on Kafka (typically milliseconds with CDC, seconds with polling).
- The outbox table must be periodically cleaned to prevent unbounded growth.
- Events may be published more than once (at-least-once semantics) — consumers must be idempotent (ADR-008).

**References:**
- Chris Richardson, *Microservices Patterns*, Chapter 3 — Transactional Outbox.
- Debezium documentation (debezium.io) — CDC-based outbox relay.
