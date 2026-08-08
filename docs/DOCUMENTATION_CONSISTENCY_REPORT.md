# Documentation Consistency Report - Reveal Feature

Project: Private Mutual Connection Platform  
Date: 2026-07-04  
Scope: Documentation consistency for the V1 Reveal feature only.

## Canonical Reveal Model

Reveal is not a later mutual-consent workflow in V1. At the beginning of the anonymous chat lifecycle, both participants independently select 3, 5, or 7 days. The system keeps individual selections private, waits until both selections exist, calculates the arithmetic mean, persists an immutable UTC `revealAt` timestamp, and automatically reveals identity at the deadline. There is no reveal request, accept action, decline action, cancellation, postponement, early reveal, or timer reset in V1.

## Files Inspected

- `AGENTS.md`
- `PROJECT_VISION.md`
- `ARCHITECTURE_RULES.md`
- `CODING_STANDARDS.md`
- `DECISION_LOG.md`
- `.github/copilot-instructions.md`
- `docs/Feature_Specification.md`
- `docs/prd/PRD_v1_Private_Mutual_Connection_Platform.docx`
- `docs/prd/User_Journey_V1_Private_Mutual_Connection_Platform.docx`
- `docs/prd/HLD_Master_Document_Structure.docx`
- `docs/hld/HLD_Chapter_1_Executive_Summary_Expanded.docx`
- `docs/hld/HLD_Chapter_2_Product_Overview.docx`
- `docs/hld/HLD_Chapter_3_Architecture_Principles.docx`
- `docs/hld/HLD_Chapter_4_System_Context_C4_Level1.docx`
- `docs/hld/HLD_Chapter_5_Container_Architecture_C4_Level2.docx`

## Files Automatically Updated

- `docs/Feature_Specification.md`
  - Replaced the request/accept/decline Reveal Service model with the canonical automatic scheduled reveal model.
  - Updated core product rules, User Profile, Anonymous Chat, Fair Play, Notification Service, cross-feature workflows, and implementation readiness references.
  - Added validation, API, database impact, edge case, security, and acceptance criteria language for independent 3/5/7 duration selection and automatic reveal.
- `DECISION_LOG.md`
  - Added ADR-011 for the independent reveal-duration selection and automatic scheduled reveal decision.

## Files Requiring Manual Update

Binary `.docx` files were inspected but not modified to avoid silently damaging formatting.

### `docs/prd/PRD_v1_Private_Mutual_Connection_Platform.docx`

Conflicting concept:

- "They chat anonymously and identities are revealed only after mutual consent."
- "Reveal requires mutual consent."
- "Reveal requests and acceptance rate"

Recommended corrections:

- Replace with: "They chat anonymously until the system-calculated reveal deadline. Identity reveal occurs automatically at `revealAt` after both users independently select 3, 5, or 7 days."
- Replace with: "Reveal timing is calculated from both participants' independent 3/5/7 day selections and identity reveal is automatic at the immutable deadline."
- Replace metric with: "Reveal schedule finalization rate, automatic reveal completion rate, and pending single-selection count."

### `docs/prd/User_Journey_V1_Private_Mutual_Connection_Platform.docx`

Conflicting concept:

- "After the configured period (3/5/7 days), users can initiate reveal."
- "If both accept, identities and profiles are revealed."
- "If either declines, anonymity continues."
- "No identity disclosure before mutual reveal."

Recommended corrections:

- Replace with: "When users first enter a newly created anonymous chat, each independently selects 3, 5, or 7 days."
- Replace with: "After both selections are submitted, the system calculates the arithmetic mean, finalizes an immutable UTC `revealAt`, and reveals identities automatically at that deadline."
- Replace with: "There is no decline action in V1; anonymity continues only until the scheduled automatic reveal deadline."
- Replace with: "No identity disclosure before automatic reveal completion at `revealAt`."

### `docs/hld/HLD_Chapter_2_Product_Overview.docx`

Conflicting concept:

- "protecting identities until both users choose to reveal themselves."
- "Reveal - Mutual consent process for identity disclosure."
- "Identity remains hidden until mutual reveal."
- "The product is intentionally differentiated from conventional dating platforms by centering every interaction around the Private List, mutual consent and anonymous communication."

Recommended corrections:

- Replace with: "protecting identities until the system-calculated automatic reveal deadline."
- Replace with: "Reveal - Independent duration selection and automatic scheduled identity reveal."
- Replace with: "Identity remains hidden until automatic reveal completion at `revealAt`."
- Replace with: "The product is intentionally differentiated from conventional dating platforms by centering every interaction around the Private List, privacy-preserving mutual interest, independently scheduled reveal, and anonymous communication."

### `docs/hld/HLD_Chapter_3_Architecture_Principles.docx`

Conflicting concept:

- "No feature may expose user identity before a successful mutual reveal."

Recommended correction:

- Replace with: "No feature may expose user identity before automatic reveal completion at the immutable `revealAt` timestamp."

## Files With No Reveal Conflict Found

- `AGENTS.md`
- `PROJECT_VISION.md`
  - Contains "Consent" as a core value, not as the obsolete Reveal Service workflow.
- `ARCHITECTURE_RULES.md`
- `CODING_STANDARDS.md`
- `.github/copilot-instructions.md`
- `docs/prd/HLD_Master_Document_Structure.docx`
  - Mentions Reveal as a service/topic only.
- `docs/hld/HLD_Chapter_1_Executive_Summary_Expanded.docx`
  - The word "accepted" appears in an unrelated operational-complexity sentence.
- `docs/hld/HLD_Chapter_4_System_Context_C4_Level1.docx`
  - Mentions "Reveals Identity" and "until reveal" only.
- `docs/hld/HLD_Chapter_5_Container_Architecture_C4_Level2.docx`
  - Mentions Reveal service/container/API only.

## Remaining Reveal-Related Conflicts

- The `.docx` conflicts listed above remain because they require manual update in Word or a safe document-generation process that preserves formatting.

## Unresolved Product Questions

- What should happen if one participant never submits a reveal-duration selection? V1 documentation now marks this as an OPEN PRODUCT QUESTION and does not define a timeout, reminder, fallback, or match-expiration policy.
- What exact User Service and Fair Play behavior should apply if a user account is suspended or deleted before scheduled reveal completion?

## Final Validation Notes

Repository searches were performed for:

- `mutual consent`
- `reveal request`
- `accept reveal`
- `decline reveal`
- `ACCEPT`
- `DECLINE`

Remaining Markdown occurrences in `docs/Feature_Specification.md` are either unrelated to Reveal Service, such as OTP acceptance, or explicit prohibitions documenting that reveal request, accept, and decline actions do not exist in V1.
