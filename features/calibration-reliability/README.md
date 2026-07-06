# LISA Calibration Reliability Authority V1

Calibration is the foundation of communication. This module validates, monitors, scores, and safeguards calibration using **observable evidence only** — never fabricated accuracy.

## Purpose

Ensure LISA never silently operates on poor calibration data. When calibration quality becomes unreliable, Lisa detects it, explains it calmly, and guides recovery through the Personality Engine.

## Architecture

```
features/calibration-reliability/README.md
app/.../features/calibrationreliability/
├── model/           Sessions, scores, health states, recommendations
├── engine/          CalibrationReliabilityEngine + verifier
├── scoring/         Completeness, stability, repeatability, drift
├── recovery/        Retry policy, resume manager, recovery planner
├── monitoring/      Health monitor, lifecycle tracker, diagnostics
├── integration/     Personality, memory, guided learning, CCR bridge
├── validation/      CalibrationReliabilityAuthorityV1
└── metadata/        Version metadata
```

## Scoring (0–100)

| Category | Score |
|----------|-------|
| Excellent | 90–100 |
| Good | 75–89 |
| Acceptable | 60–74 |
| Poor | 40–59 |
| Failed | 0–39 |

Derived from: sample completeness, eye stability, consistency, repeatability, coverage, tracking continuity.

## Health States

| State | Communication |
|-------|---------------|
| Healthy | Allowed |
| Monitor | Allowed |
| Recommend Recalibration | Allowed (with guidance) |
| Calibration Required | Blocked |
| Calibration Invalid | Blocked |

## Reused Systems

| System | Role |
|--------|------|
| Existing sensitivity / calibration UI | Wrapped, not replaced |
| `MainActivity` eye tracking | Input evidence source |
| Blink detection thresholds | Observable calibration context |
| `LisaPersonalityEngine` | All user-facing calibration dialogue |
| `CompanionMemoryEngine` | Milestones only (first calibration, improvement, recalibration) |
| `CoreCommunicationReliabilityEngine` | Consumes calibration health |
| Guided Learning | Pauses lessons when calibration invalid |

## Validation

```bash
./gradlew validateCalibrationReliabilityAuthorityV1
```

Pass token: `CALIBRATION_RELIABILITY_AUTHORITY_V1_PASS`
