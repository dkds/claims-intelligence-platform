# ADR-012: Anti-corruption layer for insurer integration

**Status:** Accepted

**Context:**
The external insurer has its own contract, terminology, data shapes, and error semantics that differ from the platform's internal domain model. Allowing the external contract to leak into the core domain would couple internal evolution to external changes.

**Decision:**
The insurer integration is isolated behind an anti-corruption layer (ACL). The ACL translates between the internal domain model (claims, policies, coverage) and the external insurer contract (submission formats, status codes, reconciliation responses). It handles batching, retry logic, idempotent submission, and error mapping. Internally, the rest of the system never sees insurer-specific types.

**Consequences:**
- Internal domain evolution is decoupled from external contract changes.
- The ACL is the single place where external integration complexity lives — mapping, retries, error translation.
- If the insurer changes their API, only the ACL adapter needs updating.
- The ACL adds a translation layer with associated mapping code and potential for mapping bugs.

**References:**
- Eric Evans, *Domain-Driven Design*, Chapter 14 — Anti-Corruption Layer.
