# LISA Personality Engine V1

Centralized personality layer that defines who Lisa is and how she speaks across the entire application.

## What It Is

The Personality Engine generates `LisaDialogue` objects — structured speech content with tone, timing, and category metadata. It is **not** a Text-to-Speech engine. TTS remains in the existing narration layer; the Personality Engine decides **what** Lisa says.

## Why It Exists

Without a central personality layer, dialogue drifts across screens — different encouragement in Practice Mode vs Guided Learning vs Settings. The Personality Engine is the single source of truth for Lisa's warm, patient, respectful voice.

## Architecture

```
features/personality/README.md
app/.../features/personality/
├── model/           Dialogue models, profile, context, timing
├── engine/          LisaPersonalityEngine interface + default impl
├── dialogue/        Catalog and provider interfaces
├── encouragement/   EncouragementDialogueProvider
├── comfort/         ComfortDialogueProvider
├── celebration/     CelebrationDialogueProvider
├── greetings/       GreetingDialogueProvider
├── waiting/         WaitingDialogueProvider
├── instruction/     InstructionDialogueProvider
├── navigation/      NavigationDialogueProvider
├── practice/        PracticeDialogueProvider
├── state/           DialogueHistoryTracker
├── metadata/        PersonalityMetadata
└── validation/      PersonalityEngineAuthorityV1
```

## How Guided Learning Uses It

Guided Learning requests dialogue through `LisaPersonalityEngine`:

- Welcome → `generateGreeting(context)`
- Lesson instructions → `generateInstruction(context)`
- Success → `generateEncouragement(context)` / `generateCelebration(context)`
- Struggle → `generateComfort(context)`
- Completion → `generateCompletionMessage(context)`

## Dialogue Categories

Greetings, welcome, instructions, encouragement, comfort, waiting, navigation, practice, minor/major/milestone celebrations, graduation, session completion, return messages, and settings guidance.

## Forbidden Phrases

Enforced by `LisaPersonalityProfile.forbiddenPhrases` and validated by `PersonalityEngineAuthorityV1`. Lisa never uses punitive language, pretends to know unobservable emotions, or makes medical claims.

## Repetition Prevention

`DialogueHistoryTracker` records recently used dialogue IDs. `LisaDialogueSelector` prevents immediate and category-level repetition. Deterministic mode supports tests.

## Future AI Dialogue

Implement `LisaDialogueGenerator` or replace `LisaDialogueProvider` without changing calling code. `DefaultLisaPersonalityEngine` uses static catalogs today; a future AI provider can plug in behind the same interface.

## Future Localisation

Every `LisaDialogue` includes a `locale` field. Catalogs are locale-aware via `DialogueCatalogProvider`.

## Validation

```bash
./gradlew validatePersonalityEngineAuthorityV1
```

Pass token: `PERSONALITY_ENGINE_AUTHORITY_V1_PASS`
