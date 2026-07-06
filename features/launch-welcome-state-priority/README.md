# LISA Launch Welcome State Priority V1

Welcome to Lisa has **highest priority** on cold app launch until Guided Learning is completed or skipped.

Saved HELLO lesson, setup, calibration, or mid-training state must **not** bypass Welcome.

## Rule

If `!tutorialCompleted && !tutorialSkipped` → always show Welcome to Lisa.

## Validation

```bash
./gradlew validateLisaLaunchWelcomeStatePriorityV1
```

Pass token: `LISA_LAUNCH_WELCOME_STATE_PRIORITY_V1_PASS`
