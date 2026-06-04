# ADR-001: Database per service

**Status:** Accepted

**Context:**
The system this project is modelled against uses a single shared database across all services. This creates tight coupling — schema changes in one service risk breaking others, deployments must be coordinated, and services cannot be scaled or evolved independently. Business logic migrates into stored procedures and views because the database is the only shared integration point.

**Decision:**
Each service owns its database privately. No other service may read from or write to it directly. Cross-service data needs are served by either event-carried state transfer (local data slices populated via Kafka events) or synchronous API calls through the owning service.

**Consequences:**
- Services can be deployed, scaled, and migrated independently.
- Each service may choose the storage technology that best fits its domain (PostgreSQL for transactional data, MongoDB for document metadata and read models).
- Cross-service queries become harder — they require either a CQRS projection (ADR-005) or API composition.
- Data consistency across services is eventual, not immediate (see ADR-009).
- Local data slices must be maintained and kept current via event consumption.

**References:**
- Chris Richardson, *Microservices Patterns*, Chapter 2 — Database per Service pattern.
