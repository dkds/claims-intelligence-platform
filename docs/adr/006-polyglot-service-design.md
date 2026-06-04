# ADR-006: Polyglot service design

**Status:** Accepted

**Context:**
The project aims to demonstrate proficiency across Java/Spring Boot, Go, and Node.js/TypeScript. Rather than using one language everywhere or assigning languages arbitrarily, each service should use the language that best fits its domain characteristics.

**Decision:**
Language assignment follows domain fit:

| Service | Language | Rationale |
|---|---|---|
| Enrollment & Policy | Java / Spring Boot | Rich domain model, complex entity relationships, benefits from strong type system and mature ORM. |
| Sessions | Java / Spring Boot | Shares domain context with Enrollment; benefits from same DDD tooling. |
| Claims (incl. Adjudication) | Java / Spring Boot | Rule-heavy domain, state machine, benefits from type system for invariant enforcement. |
| Fraud Detection | Go | Computationally focused, high-throughput scoring, stateless — Go's performance and concurrency model fit well. |
| Projection | Node.js / NestJS | I/O-heavy event consumption and MongoDB writes; async model is a natural fit. Provides TypeScript stack breadth. |
| BFF / API Gateway | Node.js / NestJS | Request aggregation, auth, serving React — I/O-bound, benefits from async model. |
| Payment, Insurer Submission, Document | Node.js / TypeScript | Infrastructure-heavy orchestration, external API integration — async I/O fits. |

**Consequences:**
- Each service uses idiomatic patterns for its language (see ADR-007 for per-service architecture).
- The polyglot approach demonstrates language selection as an architectural decision, not a preference.
- Operational complexity increases — three build toolchains, three dependency ecosystems.
- Shared concerns (event envelope parsing, auth, logging) require per-language implementations or shared libraries.
