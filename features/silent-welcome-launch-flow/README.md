# LISA Silent Welcome Launch Flow V1

First launch shows the exact simple **Welcome to Lisa** screen. LISA speaks only when translating a completed blink sequence into a phrase.

## Launch UI

- Pale light-blue background, circular LISA logo, white card
- **Start Guided Learning** → visual teaching path (Setup → Calibration → Lessons)
- **Skip to Communication Workspace** → Communication Workspace

## Voice policy

`LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY = true`

Allowed: blink sequence completes → TTS speaks the phrase (e.g. "Hello").

Not allowed: welcome, setup, calibration, lesson, coach, presence, or caregiver narration.

## Validation

```bash
./gradlew validateLisaSilentWelcomeLaunchFlowV1
```

Pass token: `LISA_SILENT_WELCOME_LAUNCH_FLOW_V1_PASS`
