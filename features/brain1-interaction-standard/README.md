# LISA Brain 1 Interaction Standard V1

Universal two-blink interaction model with decision confirmation and progressive learning difficulty.

## Constitutional rule

**No single-blink commands.** Brain 1 interaction commands require at least two deliberate blinks. Phrase vocabulary may retain its own learned gesture sequences.

## Universal gestures

| Gesture | Meaning |
|---------|---------|
| L2 (L2 R0) | Option A / Primary choice |
| R2 (L0 R2) | Option B / Secondary choice |
| L1 R1 | Confirm / Yes / Proceed (left blink first) |
| R1 L1 | Cancel / Go back (right blink first) |

## Decision model

Every important Brain 1 action follows:

1. User chooses
2. Lisa repeats the detected choice (Personality Engine dialogue)
3. User confirms (L1 R1) or cancels (R1 L1)
4. Action executes

## Progressive learning

Communication fundamentals begin at Level 1 (two-blink gestures only) and advance through five difficulty levels.

## Validation

```bash
./gradlew validateBrain1InteractionStandardAuthorityV1
```

Pass token: `BRAIN1_INTERACTION_STANDARD_AUTHORITY_V1_PASS`
