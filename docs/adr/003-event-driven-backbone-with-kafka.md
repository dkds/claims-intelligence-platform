# ADR-003: Event-driven backbone with Kafka

**Status:** Accepted

**Context:**
With database-per-service (ADR-001), services need a mechanism to communicate state changes. Synchronous HTTP calls create tight runtime coupling — if the downstream service is unavailable, the upstream operation fails. The system needs a communication backbone that decouples services in both time and availability.

**Decision:**
Apache Kafka (Redpanda locally) serves as the event backbone. Services publish domain events to Kafka topics after state changes. Interested services consume these events to update their own state, build local data slices, or trigger workflows. Topics are organised per aggregate stream (e.g. `claim.lifecycle`, `session.lifecycle`, `enrollment.lifecycle`). Events follow a standard envelope schema with metadata (event ID, type, timestamp, correlation ID, source service) and a payload.

**Consequences:**
- Services are decoupled in time — a consumer being temporarily unavailable does not block the producer.
- Event ordering is guaranteed within a partition; partition key is the aggregate ID (e.g. claim ID) to ensure all events for one aggregate are ordered.
- Kafka becomes critical infrastructure — see ADR-009 for failure handling.
- Schema evolution must be managed carefully — backward-compatible changes only (see event design document).
- Debugging event flows requires distributed tracing and correlation IDs.

**References:**
- Martin Kleppmann, *Designing Data-Intensive Applications* (2017), Chapter 11.
- Confluent documentation — topic design, partitioning, consumer groups.
