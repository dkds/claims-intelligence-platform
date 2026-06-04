# ADR-009: Eventual consistency strategy and Kafka failure handling

**Status:** Accepted

**Context:**
With database-per-service and event-driven communication (ADR-001, ADR-003), data consistency across services is eventual, not immediate. Kafka serves as the event backbone, raising a concern: if Kafka becomes unavailable, events stop flowing and cross-service data slices become stale. In the worst case, if events are lost, consistency gaps become permanent.

**Decision:**
The system is designed around the following consistency guarantees:

1. **Business decisions read strongly consistent local data.** The Claims service reads its own PostgreSQL database (which it owns) when adjudicating a claim. It does not depend on an eventually consistent projection for decision-making.

2. **Display and reporting tolerate eventual consistency.** The CQRS read model (ADR-005) may lag behind the source of truth. The UI reflects this by design.

3. **The transactional outbox (ADR-004) ensures no event is lost due to Kafka unavailability.** Events are persisted atomically with business data; Kafka downtime causes delay, not data loss.

4. **Consumers are idempotent.** Every event consumer uses a deduplication mechanism (processed-event table or Redis with TTL) to handle at-least-once delivery. Receiving the same event twice does not corrupt state.

5. **Dead-letter topics capture unprocessable events.** Events that fail consumer processing after retries are routed to a dead-letter topic for manual inspection rather than blocking the consumer's partition.

6. **Partition keys ensure per-aggregate ordering.** All events for a given aggregate (e.g. claim ID) are written to the same Kafka partition, guaranteeing they arrive at the consumer in the order they were produced.

7. **Kafka cluster resilience is configured properly.** Replication factor of 3, `min.insync.replicas=2`, producer `acks=all`. Partial broker failures result in no data loss and no downtime.

Under these constraints, a complete Kafka cluster failure degrades the system to "consistent but delayed" — each service continues to function with its local data, business operations succeed (events queue in the outbox), and when Kafka recovers, the event backlog is drained and consistency is restored. The system never enters a state of permanent inconsistency from an infrastructure failure alone.

**Consequences:**
- Every new consumer must be designed with idempotency from the start — this is a discipline, not a one-time setup.
- The eventual consistency window is normally milliseconds to seconds (CDC relay latency). During Kafka outages, it widens to the duration of the outage.
- Monitoring must track outbox table size, consumer lag, and dead-letter topic depth to detect consistency issues early.
- Developers must understand which data is strongly consistent (local) versus eventually consistent (projected) and choose the correct source for each use case.

**References:**
- Chris Richardson, *Microservices Patterns*, Chapters 3–4.
- Martin Kleppmann, *Designing Data-Intensive Applications*, Chapter 11.
