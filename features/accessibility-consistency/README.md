# LISA Accessibility Consistency Authority V1

Permanent accessibility guardian for Brain 1 — validates consistency without redesigning the application.

## Purpose

Ensure every screen, workflow, and communication path remains:

- Simple, consistent, readable, reachable, predictable
- Low cognitive load, high accessibility
- Suitable for users with severe physical disabilities

## Architecture

```
features/accessibility-consistency/README.md
app/.../features/accessibilityconsistency/
├── model/           Audits, issues, scores, reports
├── engine/          AccessibilityConsistencyEngine + audit runner
├── validators/      Typography, touch, contrast, layout, navigation, ...
├── diagnostics/     Developer diagnostics
├── integration/     Personality Engine guidance adapter
├── validation/      AccessibilityConsistencyAuthorityV1
└── metadata/        Rule thresholds
```

## Validated Categories

Typography · Touch targets · Contrast · Layout · Navigation · Guided Learning · Communication · Emergency · Settings · Screen consistency · Cognitive load

## Scoring (0–100)

| Band | Score |
|------|-------|
| Excellent | 90–100 |
| Good | 75–89 |
| Acceptable | 60–74 |
| Needs Improvement | 40–59 |
| Critical | 0–39 |

Scores are evidence-based from validator check pass rates and observed issues.

## Reused Systems

| System | Role |
|--------|------|
| `LisaAccessibilityUi` / `LisaSettingsUiState` | Settings and display validation |
| Theme tokens (`Color.kt`, `Type.kt`) | Contrast/typography rules |
| Guided Learning UI | Training accessibility checks |
| Core Communication Reliability | Communication path checks (read-only) |
| Personality Engine | User-facing accessibility guidance |
| Navigation Reachability Authority | Complementary — not duplicated |

## Validation

```bash
./gradlew validateAccessibilityConsistencyAuthorityV1
```

Pass token: `ACCESSIBILITY_CONSISTENCY_AUTHORITY_V1_PASS`
