# LISA Offline Reliability Authority V1

Permanent offline-first guardian for Brain 1 — validates that communication never depends on internet connectivity.

## Purpose

If a user loses internet access, they must not lose their ability to communicate. This authority continuously verifies that all Brain 1 capabilities remain fully operational offline.

## Core Principle

Offline communication is not an optional feature. It is a core reliability guarantee.

## Architecture

```
features/offline-reliability/README.md
app/.../features/offlinereliability/
├── model/           Capabilities, status, scores, reports
├── engine/          OfflineReliabilityEngine + capability runner
├── validators/      Per-subsystem offline validators
├── diagnostics/     Developer diagnostics
├── validation/      OfflineReliabilityAuthorityV1
└── metadata/        Score bands, dependency patterns
```

## Validated Capabilities

Eye tracking · Blink detection · Calibration · Sequence recognition · Phrase matching · Phrase selection · Phrase speech · Communication history · Practice mode · Guided Learning · Navigation · Accessibility · Personality dialogue · Companion Memory · Emergency · Settings · TTS

## Communication Pipeline (must remain offline)

```
Eye Tracking → Blink Detection → Calibration → Sequence Recognition
→ Phrase Selection → Existing TTS → Communication History
→ Companion Memory → Personality feedback
```

## Scoring (0–100)

| Band | Score |
|------|-------|
| Excellent | 90–100 |
| Good | 75–89 |
| Acceptable | 60–74 |
| Needs Improvement | 40–59 |
| Critical | 0–39 |

Scores are evidence-based from validator check pass rates and dependency audit results.

## Reused Systems

| System | Role |
|--------|------|
| `MainActivity` / ML Kit | On-device eye tracking |
| Core Communication Reliability | Offline phrase path |
| `LisaTtsVoiceManager` | Local TTS with offline voice preference |
| Calibration Reliability | Local calibration health |
| Guided Learning | Local progress store |
| Personality Engine | Local dialogue catalog |
| Companion Memory | Local milestone storage |
| Accessibility Consistency | Local accessibility validation |

## What This Does NOT Do

- Does not disable networking
- Does not add cloud features or Brain 2
- Does not add LLM integration
- Does not duplicate existing implementations
- Does not change communication behavior at runtime

## Validation

```bash
./gradlew validateOfflineReliabilityAuthorityV1
```

Pass token: `OFFLINE_RELIABILITY_AUTHORITY_V1_PASS`
