# LISA Experience Polish — Phase A: First Five Minutes V1

Calm, zero-touch, guided first five minutes after caregiver setup.

## Scope

- First launch choice (L2 / R2) with confirmation (L1 R1) and cancel (R1 L1)
- Meet Lisa conversational introduction
- Getting Ready face detection guidance
- Calibration introduction, visuals, and result feedback
- First phrase (HELLO = L2) with meaningful celebration
- Gentle failure recovery
- Progressive early fundamentals

## Universal gestures (Brain 1 interaction commands)

| Gesture | Meaning |
|---------|---------|
| L2 | Option A / Primary |
| R2 | Option B / Secondary |
| L1 R1 | Confirm (left blink first) |
| R1 L1 | Cancel (right blink first) |

**No L2 R2 cancel** in Brain 1 interaction commands.

Phrase vocabulary (NO = L1 R1, PLEASE = R1 L1) uses the same order model inside lesson mode only.

## Validation

```bash
./gradlew validateLisaExperiencePhaseAFirstFiveMinutesV1
```

Pass token: `LISA_EXPERIENCE_PHASE_A_FIRST_FIVE_MINUTES_V1_PASS`
