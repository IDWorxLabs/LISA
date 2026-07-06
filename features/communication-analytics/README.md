# LISA Communication Accuracy Analytics V1

Objective measurement layer for Brain 1 — observes communication quality without altering runtime behaviour.

## Purpose

Answer evidence-based questions:

- Is communication becoming more accurate?
- Is calibration helping?
- Are false positives increasing?
- Are retries decreasing?
- Is phrase recognition improving?
- Are emergency safeguards performing correctly?

## Architecture

```
features/communication-analytics/README.md
app/.../features/communicationanalytics/
├── model/           Sessions, metrics, reports, trends
├── engine/          CommunicationAnalyticsEngine + aggregator
├── metrics/         Success, retry, accuracy, false +/-, timing, calibration, nav, emergency
├── reporting/       Report generator, trend analyzer, reliability summary
├── diagnostics/     Developer diagnostics and logger
├── integration/     Read-only bridge to CCR + calibration
├── validation/      CommunicationAccuracyAnalyticsAuthorityV1
└── metadata/        Version metadata
```

## Core Principle

**Observer only.** Analytics reads from:

| Source | Data |
|--------|------|
| `CommunicationDiagnostics` | Evaluation outcomes (passive observer) |
| `CommunicationReliabilityHistoryRecorder` | Spoken phrase records |
| `CalibrationReliabilityEngine` | Health/score snapshot at attempt time |

Analytics never modifies blocking, confirmation, TTS, calibration gating, or phrase matching.

## Metrics

- Success rate, retry rate, accuracy rate
- False positive / false negative signals (observable proxies only)
- Phrase timing distributions
- Phrase-level statistics
- Calibration impact correlation
- Navigation and emergency analytics
- Trend analysis over historical attempts

## Validation

```bash
./gradlew validateCommunicationAccuracyAnalyticsAuthorityV1
```

Pass token: `COMMUNICATION_ACCURACY_ANALYTICS_AUTHORITY_V1_PASS`
