# Cinnamon Learning Focus — Canonical Correction Contract

Status: implementation contract, 2026-07-21

## Product truth

Cinnamon is a personal advanced English and Medical English learning companion. The feature corrected here is a **Learning Focus** layered onto the existing Foundation plan. It is not a campaign, expedition, clinical simulation, medical-care workflow, or second journey.

## Canonical active vocabulary

| Concept | Canonical name |
|---|---|
| Aggregate | `LearningFocusDefinition` |
| Selectable item | `LearningFocusOption` |
| Persisted choice | `LearningFocusSelection` |
| UI state | `LearningFocusUiState` |
| Action | `selectLearningFocus` |
| User section | `LEARNING FOCUS` |
| Precision option | `Language Precision` |
| Breadth option | `Recall Range` |
| Progress unit | `Milestone` |

The active product and code vocabulary must not use Campaign, Expedition, Trail, Circuit, Route, Chapter, Focus Cycle, or Mastery Track.

## Evidence contract

### Language Precision

1. Use three distinct practice kinds.
2. Bring two distinct items to delayed mastery.
3. Record meaningful learning activity on five distinct days.

### Recall Range

1. Review eight distinct items.
2. Use four distinct practice kinds.
3. Record meaningful learning activity on five distinct days.

Selection itself grants **0 XP**, settles no evidence, and creates no reward receipt. XP is earned only from completed milestones. No option may claim clinical competence, fluency, adaptation, synchronization, or complete language mastery.

## Behavior contract

- The Foundation prerequisite remains versioned and evidence-backed.
- Only the selected focus progression may materialize.
- A same-option replay is idempotent and creates no new event, row, XP, or receipt.
- A different-option replay is a typed conflict and rolls back completely.
- The v1 selection is saved and cannot be changed; the UI must say this before confirmation.
- There is no promised next focus, recurring cycle, or switch action until those behaviors exist.
- Loading, locked, choosing, saving, active, completed, incompatible-record, and save-failure states must be explicit and truthful.
- Historical evidence must not silently count as post-selection work unless the milestone definition explicitly allows it. New focus progress is anchored to the selection timestamp.

## Legacy-data contract

The v4 database and immutable event history are authoritative user data. Migration must preserve, byte-for-byte where applicable:

- selection/choice ID;
- actor ID;
- definition ID and version;
- option/route ID;
- linked journey definition and instance/stages;
- source event ID and event history;
- timestamp;
- reward summaries/transactions and total balance.

Legacy Campaign identifiers may exist only in frozen v3→v4 SQL/schema, the v4→v5 mapping, migration fixtures, immutable historical rows, and one bounded Learning Focus compatibility adapter. The former Foundation wire id `journey.foundation.expedition` follows the same rule through its own bounded adapter. None of these identities may appear in active domain types, repository/DAO/ViewModel APIs, accessibility text, visible UI, current report prose, or new events.

Fresh v5 Foundation plans, Learning Focus selections, milestone plans, and stage rows use only canonical identifiers. Compatibility adapters may read and project exact persisted identities, but they must never mint a legacy identity for new data.

Migration is transactional and non-destructive: copy, prove count and row equality, prove uniqueness and foreign keys, then remove the legacy table. Any failed invariant throws and rolls the entire migration back. `fallbackToDestructiveMigration` is forbidden.

## UX contract

- Preserve the selected toasted-dark Cinnamon identity, warm spice orange, mint/cyan accents, assets, and mascot system.
- Show Learning Focus immediately after the prerequisite milestone instead of burying it below locked Foundation milestones.
- Present it as a compact complementary module, not a second equal progression spine.
- Compact widths stack options; medium widths may place them side by side.
- Interactive targets are at least 48dp; body copy is at least 14sp where space permits; state changes are announced accessibly.
- Motion is brief and state-driven, with a fully static reduced-motion path.
- Errors never claim repair, restoration, synchronization, or unchanged state unless the implementation proves it.

Approved visual direction: [`assets/learning-focus-preview-v1.png`](assets/learning-focus-preview-v1.png).

## Completion evidence

Completion requires source search gates, unit tests, Room migration tests for every supported version chain, ledger replay/conflict tests, lint, release build, real `adb install -r` upgrade over a populated v4 database, before/after database conservation checks, cold-relaunch proof, screenshots of all critical states, and visually inspected Markdown plus Persian RTL PDF reports.
