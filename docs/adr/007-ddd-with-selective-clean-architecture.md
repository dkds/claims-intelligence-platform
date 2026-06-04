# ADR-007: DDD with selective Clean Architecture

**Status:** Accepted

**Context:**
The project uses Domain-Driven Design (DDD) as its modelling approach. A question arises about how much architectural ceremony to apply uniformly: full Clean Architecture (explicit ports, adapters, and separated domain/persistence models) adds rigour but also significant boilerplate. Not all services have equal domain complexity — the enrollment service is largely CRUD, while auto-adjudication is rule-heavy.

**Decision:**
The level of architectural ceremony is matched to domain complexity:

**Full Clean Architecture (ports and adapters)** is used for services with complex domain logic:
- Claims / Adjudication service — rule engine, state machine, complex invariants.
- Fraud Detection service — scoring strategies, rule evaluation.

These services have explicit input ports (use case interfaces called by controllers) and output ports (repository and messaging interfaces defined by the application layer, implemented by infrastructure adapters). The domain model is separated from the persistence model.

**Package-by-feature with standard Spring components** is used for services with thin domain logic:
- Enrollment & Policy service — structured as `clinic/`, `vet/`, `pet/`, `policy/` packages, each containing its Controller, Service, Repository, and JPA entity. Business behaviour is pushed into entity methods where possible, but formal ports and adapters are not used.
- Sessions service — similar package-by-feature structure.

**Simple orchestration structure** is used for infrastructure-heavy services:
- Payment, Projection, BFF, Document, Insurer Submission — these are predominantly I/O orchestration with little domain logic. A straightforward service-layer structure is appropriate.

All services, regardless of architecture tier, share two invariants:
1. Dependencies point inward — no domain or application code imports infrastructure or framework types.
2. Business logic lives in the domain model, not in controllers or persistence layers (ADR-002).

**Consequences:**
- Complex domains get the isolation and testability benefits of Clean Architecture without imposing that cost on simpler services.
- The variation in structure across services is itself a portfolio talking point — it demonstrates architectural judgment rather than uniform pattern application.
- Developers must understand which tier a service falls into and follow the corresponding conventions.
- If a simple service's domain grows complex over time, it can be refactored toward full Clean Architecture — extracting an interface from a concrete class is a low-cost IDE operation.

**References:**
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* (2019).
- Oliver Drotbohm, Spring Modulith — package-by-feature with enforced module boundaries.
- Robert C. Martin, *Clean Architecture* (2017).
