# ADR-008: Interfaces at architectural boundaries only

**Status:** Accepted

**Context:**
Traditional Spring Boot practice creates an interface for every service class (e.g. `ClaimService` interface + `ClaimServiceImpl`), justified by the Dependency Inversion Principle, testability, and Spring's proxy mechanism. However, modern tooling has weakened each of these justifications: Mockito mocks concrete classes, Spring Boot 2.x+ defaults to CGLIB proxies (no interface required), and the second implementation rarely materialises.

**Decision:**
Interfaces are created only where they serve a genuine architectural purpose:

- **At Clean Architecture port boundaries** — input ports (use case interfaces called by controllers) and output ports (repository and messaging interfaces defined by the application layer, implemented by infrastructure adapters). These enforce the dependency rule at compile time.
- **Where genuine polymorphism exists** — e.g. `FraudScoringStrategy` with `RuleBasedScorer` and `MlModelScorer`. The implementations have distinct names because they do distinct things.
- **At module or deployment boundaries** — if a service is consumed by another separately compiled module, the interface acts as a published contract.

Interfaces are **not** created reflexively for application-internal service classes with a single implementation. If a naming pattern like `XService` + `XServiceImpl` emerges, that is a signal the interface is not earning its place.

**Consequences:**
- Fewer files, less indirection, simpler navigation. Every interface that exists has a clear reason.
- The Clean Architecture port pattern replaces the blanket interface pattern — ports are the interfaces that matter, and they exist at layer boundaries.
- Domain services (e.g. `EligibilityDomainService`) are concrete classes, directly mockable via Mockito.
- If a second implementation is genuinely needed later, extracting an interface is a trivial IDE refactoring.

**References:**
- Martin Fowler, "InterfaceImplementationPair" (martinfowler.com).
- Spring Boot documentation — no longer recommends blanket service interfaces.
