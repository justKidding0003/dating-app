# Architectural Decision Records

ADR-001

Microservices chosen over Modular Monolith.

ADR-002

Database per Service.

ADR-003

REST communication for Version 1.

ADR-004

Dedicated Social Graph Service.

ADR-005

Batch Matching.

ADR-006

Spring Cloud Gateway.

ADR-007

Flutter Client.

ADR-008

JWT Authentication.

ADR-009

WebSocket Chat.

ADR-010

PlantUML as Architecture Source.

ADR-011

Reveal timing is selected independently at anonymous chat creation using 3, 5, or 7 days. The arithmetic mean determines an immutable reveal schedule. Identity reveal occurs automatically at the deadline without later consent.

Context:

The product requires predictable anonymity for matched users while avoiding later pressure, negotiation, or unilateral reveal initiation. The previous documentation described identity reveal as a later request and response workflow, which conflicts with the intended privacy and fairness model.

Decision:

At the beginning of the anonymous chat lifecycle, both participants independently select 3, 5, or 7 days. The system keeps individual choices private until both selections exist, calculates the arithmetic mean as the reveal period, persists an immutable UTC reveal timestamp, and automatically reveals identity at that deadline.

Alternatives Considered:

- Later reveal request with accept or decline.
- Fixed platform-wide anonymous period.
- User-selected period controlled by the first participant.
- Early reveal or postponement after chat starts.

Consequences:

- Reveal behavior is deterministic and testable.
- Services must support duration selection, schedule finalization, due reveal processing, and idempotent worker retries.
- Clients must gate first chat entry with a mandatory duration-selection modal.
- Product must separately define behavior when one participant never submits a duration.

Privacy Implications:

- Individual duration selections remain private from the other participant.
- Server-side UTC time is authoritative for all reveal calculations.
- Identity access remains blocked before the exact reveal timestamp.
- Audit history must record selections, finalization, calculated schedule, and automatic reveal completion.
