# LISA Core Communication Reliability V1

Dependable eye movement → blink sequence → phrase → speech before any advanced intelligence.

## Purpose

Verify and strengthen the existing communication path without duplicating eye tracking, blink detection, TTS, or the phrase catalog. This module wraps, validates, and records the core loop.

## Architecture

```
features/core-communication-reliability/README.md
app/.../features/corecommunicationreliability/
├── model/           Reports, attempts, outcomes, metrics
├── engine/          CoreCommunicationReliabilityEngine + path verifier
├── sequence/        Normalize, validate, debounce, confidence score
├── phrase/          Match verify, selection guard, confirmation policy
├── speech/          Output verify, TTS adapter
├── emergency/       Safety guard, confirmation policy
├── history/         Spoken phrase reliability history (not companion memory)
├── diagnostics/     Developer diagnostics and logging
├── validation/      CoreCommunicationReliabilityAuthorityV1
└── metadata/        Version metadata
```

## Verified Path

1. Raw blink input (existing detection)
2. Normalize → `L{left} R{right}`
3. Debounce duplicate firing
4. Validate sequence (complete, allowed, unambiguous)
5. Score confidence (deterministic indicators only)
6. Match phrase (exact catalog lookup)
7. Selection guard (block unsafe/ambiguous/low-confidence)
8. Confirmation policy (countdown for normal; stricter for emergency)
9. Existing TTS speaks phrase
10. History records attempt; Companion Memory records milestones only

## Reused Systems

| System | Role |
|--------|------|
| `findExactMapping` / `LisaCoreVocabulary` | Phrase catalog |
| `LisaSystemLanguage` | Reserved sequences |
| `TextToSpeech` via `SpeechReliabilityAdapter` | Speech output |
| `LisaPersonalityEngine` | User-facing feedback |
| `CompanionMemoryEngine` | Milestones only (first phrase, etc.) |

## Outcomes

| Outcome | Meaning |
|---------|---------|
| PASS | Valid path completed or safely handled |
| WARN | Handled with caution (e.g. low confidence requiring confirmation) |
| BLOCKED | Safely prevented (ambiguous, duplicate, training emergency) |
| FAIL | Valid phrase but speech/history failed |

## Validation

```bash
./gradlew validateCoreCommunicationReliabilityAuthorityV1
```

Pass token: `CORE_COMMUNICATION_RELIABILITY_AUTHORITY_V1_PASS`
