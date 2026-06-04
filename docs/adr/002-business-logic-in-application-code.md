# ADR-002: Business logic in application code

**Status:** Accepted

**Context:**
The predecessor system encodes significant business logic in stored procedures, database triggers, and schema constraints. This makes the logic hard to test, hard to version, hard to reason about, and invisible to code reviews. It also couples business rules to a specific database vendor.

**Decision:**
All business rules live in application code, expressed through a rich domain model (DDD tactical patterns: aggregates, value objects, domain services, domain events). The database is a persistence mechanism only — it stores state but does not compute, validate, or transform it. Schema constraints (NOT NULL, foreign keys) may enforce data integrity at the storage level, but business invariants (e.g. "a claim cannot be approved if coverage is exhausted") are enforced in the domain model.

**Consequences:**
- Business rules are testable with plain unit tests — no database, no framework context required.
- Rules are version-controlled, code-reviewed, and refactorable.
- Domain objects carry behaviour, not just data (avoiding the Anemic Domain Model anti-pattern).
- Developers must resist the convenience of pushing logic into SQL queries or database triggers.

**References:**
- Eric Evans, *Domain-Driven Design* (2003) — rich domain model, aggregates, value objects.
- Martin Fowler, "AnemicDomainModel" (martinfowler.com).
