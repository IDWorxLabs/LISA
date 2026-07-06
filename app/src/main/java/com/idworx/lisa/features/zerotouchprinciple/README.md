# LISA Zero-Touch Principle (ZTP) — Brain 1 Module

**Constitutional priority:** 1 — the first design principle engineers encounter for Brain 1.

See also: [features/zero-touch-principle/README.md](../../../../../../../../features/zero-touch-principle/README.md)

## Rule

Every Brain 1 workflow must be completable using only eye movement, blink sequences, vision, and hearing after initial caregiver setup.

## Package layout

| Package | Role |
|---------|------|
| `metadata` | Constitutional rule, forbidden phrases |
| `experience` | First Conversation Experience stage dialogue |
| `audit` | Static compliance probes for validation |
| `validation` | `ZeroTouchPrincipleAuthorityV1` |

## Validation

```bash
./gradlew validateZeroTouchPrincipleAuthorityV1
```

Pass token: `ZERO_TOUCH_PRINCIPLE_AUTHORITY_V1_PASS`

## Integration

- **Guided Learning** — welcome/completion auto-narration, patience prompts
- **Personality Engine** — greeting sequences, comfort on missed blinks
- **Companion Memory** — returning-user personalization
- **Calibration Reliability** — conversational recalibration offers

Every new Brain 1 feature must reference and comply with ZTP.
