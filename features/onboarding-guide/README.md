# LISA Guided Onboarding & Training System V1

Independent feature module teaching new users how to communicate and navigate LISA.

## Package

`com.idworx.lisa.features.onboardingguide`

## Structure

| Package | Purpose |
|---------|---------|
| `model` | Lesson definitions, progress, preferences, phases |
| `lessons` | Communication and navigation lesson catalogs |
| `services` | Persistence, encouragement, adaptive learning |
| `audio` | Narration layer over existing Text-to-Speech |
| `state` | UI state and event model |
| `navigation` | Phase/lesson navigation coordinator |
| `ui` | Compose screens and training flow |
| `validation` | GuidedTrainingAuthorityV1 compliance checks |
| `metadata` | Feature version and capability metadata |

## First Launch Flow

1. **Welcome** — LISA introduces herself; Begin Learning or Skip Tutorial
2. **Setup** — Camera permission, safety, profile name (condensed onboarding)
3. **Communication Lessons** — Progressive blink-sequence practice (12 lessons)
4. **Navigation Lessons** — Real interface actions (menu, back, categories, etc.)
5. **Completion** — Celebration and transition to main app

## Persistence

Stored in SharedPreferences (`lisa_prefs`) via `TrainingProgressStore`:

- Tutorial started / completed / skipped
- Current phase and lesson index
- Completed lesson IDs
- Practice statistics (attempts, successes)
- Narration preferences (speed, volume, enabled, language)

## Integration Points

- **MainActivity** — Training sequence routing, TTS narration callbacks, navigation challenge verification
- **LisaRootUI** — Gates main UI while training active
- **Settings** — Learning section for replay, practice, reset, voice controls

## Validation

Run: `./gradlew validateGuidedTrainingAuthorityV1`

Pass token: `GUIDED_TRAINING_AUTHORITY_V1_PASS`

## Extension Points

- `AdaptiveLearningService.onReduceSensitivityRequested` — wired to existing sensitivity controls
- `AdaptiveLearningService.onRecalibrateRequested` — extension for future calibration UI
- `TrainingPreferences.narrationLanguage` — prepared for multilingual narration
