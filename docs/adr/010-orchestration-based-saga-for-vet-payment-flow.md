# ADR-010: Orchestration-based Saga for vet payment flow

**Status:** Accepted

**Context:**
When a treatment session is verified by a clinic manager, an internal payment must be issued to the vet. This flow spans multiple services (Sessions, Claims, Payment) and cannot be wrapped in a single database transaction because each service owns its own database. If the payment fails after the session has been marked as verified, the system needs a way to compensate.

**Decision:**
The verified-session-to-vet-payment flow uses an orchestration-based Saga. A saga coordinator (within the Claims service) manages the sequence of steps and issues compensating actions on failure. This is preferred over choreography (where each service reacts to events independently) because the payment flow has a clear linear sequence and compensation requirements that are easier to reason about with a central coordinator.

**Consequences:**
- The coordinator holds the flow logic in one place, making it easier to understand, test, and debug.
- Compensating transactions must be defined for each step (e.g. reversing a payment record if downstream submission fails).
- The coordinator must itself be resilient — it persists saga state to survive restarts.
- The orchestration approach introduces a coordination point; choreography would distribute this but at the cost of harder reasoning about the overall flow.

**References:**
- Chris Richardson, *Microservices Patterns*, Chapter 4 — Saga pattern (orchestration vs choreography).
