# LISA Experience Polish — Phase B: Communication Workspace V1

Daily communication workspace polish — calm, simple, zero-touch, and impossible to confuse after the First Five Minutes.

## Scope

- Workspace entry narration after Guided Learning or skip
- Navigation clarity with context hints per screen mode
- Category menu and phrase selection guidance (Personality Engine)
- Back behavior: **L2 R2** returns one step in workspace navigation
- Gesture layer separation: Brain 1 decisions vs workspace navigation
- Fatigue reduction patience hints
- Emergency L6 R0 with confirmation (Brain 1 standard)
- Caregiver-visible gesture legend on overlay

## Workspace navigation gestures

| Gesture | Meaning |
|---------|---------|
| L2 R0 | Scroll up / previous |
| L0 R2 | Scroll down / next |
| L1 R1 | Select phrase / save adjustment |
| L2 R2 | Back |
| L4 R4 | Categories |
| L6 R0 | Emergency (confirm required) |

Brain 1 decision gestures (L2/R2 choice, L1 R1 confirm, R1 L1 cancel) apply only in decision flows — not while browsing phrases.

## Validation

```bash
./gradlew validateLisaExperiencePhaseBCommunicationWorkspaceV1
```

Pass token: `LISA_EXPERIENCE_PHASE_B_COMMUNICATION_WORKSPACE_V1_PASS`
