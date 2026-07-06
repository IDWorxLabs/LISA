# LISA Companion Memory System V1

Truthful, observable memory of the user's journey with Lisa.

## Purpose

The Companion Memory System remembers **facts from real application events** — milestones, sessions, preferences, and learning progress — so Lisa can greet users personally without inventing memories or inferring emotions.

## Architecture

```
features/companionmemory/README.md
app/.../features/companionmemory/
├── model/           Memory models, milestones, greeting context
├── engine/          CompanionMemoryEngine interface + default impl
├── repository/      Store, serializer, repository interface
├── integration/     Personality, Guided Learning, Practice adapters
├── analytics/       LearningProgressAnalyzer
├── state/           CompanionMemoryState
├── metadata/        CompanionMemoryMetadata
└── validation/      CompanionMemoryAuthorityV1
```

## Observable Evidence Rules

Every memory must include `observableEvidence` describing the application event that created it.

**Valid:** "Completed communication lesson comm_hello", "Practice session ended after 120s"

**Invalid:** "User was sad", "User enjoys practice", "User misses family"

Emotion keywords are blocked at record time.

## Privacy Rules

Never store: conversation content, personal phrases, medical diagnoses, emotional assumptions, caregiver conversations.

## Integration

| System | Adapter | Records |
|--------|---------|---------|
| Guided Learning | `GuidedLearningMemoryAdapter` | Lessons, milestones, graduation, skip |
| Personality Engine | `PersonalityMemoryAdapter` | Greeting context → `DialogueContext` |
| Practice Mode | `PracticeMemoryAdapter` | Sessions, exercises, streaks |

Training operational state remains in `TrainingProgressStore`. Companion Memory records **journey facts** without duplicating current-lesson pointers.

## Future Extensions

- `exportMemory()` / `importMemory()` — JSON local backup (V1)
- Cloud sync hooks via `CompanionMemoryRepository` interface
- AI memory expansion via observable event stream

## Validation

```bash
./gradlew validateCompanionMemoryAuthorityV1
```

Pass token: `COMPANION_MEMORY_AUTHORITY_V1_PASS`
