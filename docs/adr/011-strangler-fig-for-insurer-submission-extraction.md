# ADR-011: Strangler Fig for Insurer Submission extraction

**Status:** Accepted

**Context:**
The Insurer Submission functionality is architecturally distinct from core claims processing — it is an anti-corruption layer translating internal models to an external contract. However, building it as a separate service from day one increases the service count and operational overhead during the early phases of development.

**Decision:**
Insurer Submission starts as a module within the Claims service (Tier 1), behind a well-defined internal interface. When the module matures and the external integration patterns stabilise, it is extracted into its own service (Tier 2) using the Strangler Fig pattern — the internal interface becomes a Kafka-based event contract, and the module becomes a standalone consumer.

**Consequences:**
- Early development is simpler — fewer services to deploy and manage.
- The internal interface forces clean separation even within the monolith, making later extraction straightforward.
- The extraction itself becomes a demonstrable architectural evolution — a strong interview talking point.
- Risk: if the internal interface is not kept clean, extraction becomes a larger refactoring effort.

**References:**
- Martin Fowler, "StranglerFigApplication" (martinfowler.com).
