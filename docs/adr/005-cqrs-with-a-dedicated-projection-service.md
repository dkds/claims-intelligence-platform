# ADR-005: CQRS with a dedicated Projection service

**Status:** Accepted

**Context:**
With database-per-service, queries that span multiple domains (e.g. "show me a claim with its pet details, vet information, session history, and fraud score") cannot be answered by a single service's database. API composition (calling multiple services and joining in the BFF) is an option but introduces latency, complexity, and tight runtime coupling.

**Decision:**
A dedicated Projection service (Node.js/NestJS) consumes domain events from all relevant services and maintains a denormalised, query-optimised read model in MongoDB. This is the sole writer to the read store. The BFF reads from this store to serve the React portals. Command operations (writes) are sent directly to the owning service.

**Consequences:**
- Read queries are fast, simple, and do not couple the BFF to multiple upstream services at runtime.
- The read model is eventually consistent with the source-of-truth services — display and reporting tolerate this; decisions that require strong consistency read from the owning service's local database.
- The Projection service must handle event ordering, idempotency, and schema evolution.
- A single dedicated writer avoids write contention and conflicting projections.
- MongoDB is a natural fit for the read model due to its flexible schema and document-oriented structure.
- The Projection service is a consumer of events, not a producer — it does not participate in sagas or command flows.

**References:**
- Martin Fowler, "CQRS" (martinfowler.com/bliki/CQRS.html).
- Greg Young — CQRS and Event Sourcing talks.
