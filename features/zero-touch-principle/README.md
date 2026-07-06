# LISA Zero-Touch Principle (ZTP)

**The highest constitutional design principle of Brain 1.**

## The Principle

LISA shall always assume that the primary user has **no voluntary hand movement** and cannot physically interact with the device.

Every Brain 1 interaction must be completable using only:

- Eye movement
- Blink sequences
- Vision
- Hearing

No Brain 1 workflow shall require touching the device after initial caregiver setup.

## Caregiver Responsibilities (Once)

- Mount the phone
- Position it correctly
- Grant Android permissions during first installation
- Press Start once

After that, **Lisa performs everything else.**

## Conversation Drives the Interface

```
Lisa speaks → Interface changes → User looks → User blinks
→ Lisa understands → Lisa responds → Next interaction
```

The conversation drives the interface. The interface never drives the conversation.

## First Conversation Experience

The user is not completing a tutorial. They are having their **first conversation with Lisa**.

Stages: Meet Lisa → Getting Ready → Calibration → First Communication → Building Confidence → Celebration → Navigation → Lesson Complete.

## Waiting Is a Feature

Lisa waits patiently — 30 seconds, 5 minutes, or longer. No arbitrary timeout pressure.

## What ZTP Forbids in Brain 1 UX Copy

- "Tap Continue" / "Press Next" / "Swipe"
- Blaming the user ("Failed", "Incorrect", "Invalid", "Retry")
- Empty screens where Lisa says nothing

## Validation

```bash
./gradlew validateZeroTouchPrincipleAuthorityV1
```

Pass token: `ZERO_TOUCH_PRINCIPLE_AUTHORITY_V1_PASS`
