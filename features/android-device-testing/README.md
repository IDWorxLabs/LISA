# LISA Android Device Testing Protocol V1

Production-grade real Android device testing protocol for moving Brain 1 from structurally validated to practically tested on hardware.

## Purpose

Create a clear, repeatable, evidence-based testing process for validating LISA on real Android devices.

**This is device testing readiness, not clinical validation.** Do not claim patient readiness.

## Core Principle

A green build is not the same as a working assistive communication device.

All protocol steps default to **NOT_TESTED** until a tester records evidence on real hardware.

## Architecture

```
features/android-device-testing/README.md
app/.../features/androiddevicetesting/
├── model/           Sessions, profiles, cases, steps, results, reports
├── protocol/        AndroidDeviceTestingProtocol + plan builder
├── suites/          13 device test suites
├── reporting/       Report generator, summary, checklist
├── diagnostics/     Developer diagnostics
├── integration/     Brain 1 Readiness bridge
├── validation/      AndroidDeviceTestingProtocolAuthorityV1
└── metadata/        Standard phrases, lighting, positions
```

## Test Suites (13)

Launch & Permission · Calibration · Eye Tracking · Blink Detection · Communication Path · Guided Learning · Emergency Safety · Offline · Accessibility · Lighting · Phone Position · Long Session · Performance

## Test Outcomes

| Outcome | Meaning |
|---------|---------|
| `PASS` | Evidence recorded; step passed on device |
| `PASS_WITH_WARNING` | Passed with noted concern |
| `FAIL` | Failed on device; evidence required |
| `BLOCKED` | Critical blocker; evidence required |
| `NOT_TESTED` | Default — no evidence yet |

**PASS requires non-blank evidence.** The protocol rejects pass recordings without evidence.

## Report Readiness Outcomes

| Outcome | When |
|---------|------|
| `DEVICE_TESTING_NOT_STARTED` | Default — no evidence |
| `DEVICE_TESTING_IN_PROGRESS` | Partial evidence |
| `READY_FOR_MORE_DEVICE_TESTING` | ≥50% coverage |
| `READY_FOR_CONTROLLED_USER_TESTING_WITH_SUPERVISION` | ≥90% pass coverage |
| `NOT_READY_FOR_USER_TESTING` | Failures recorded |
| `BLOCKED_BY_CRITICAL_DEVICE_FAILURE` | Blocked steps recorded |

## Brain 1 Readiness Integration

Brain 1 Readiness Review detects whether real device evidence exists via `Brain1DeviceTestingIntegration`. Until evidence is recorded, Brain 1 remains **READY_WITH_WARNINGS**.

## Validation

```bash
./gradlew validateAndroidDeviceTestingProtocolAuthorityV1
```

Pass token: `ANDROID_DEVICE_TESTING_PROTOCOL_AUTHORITY_V1_PASS`

## Usage (Developer)

1. Create session with device profile
2. Print checklist: `DeviceTestingChecklist.format(plan)`
3. Execute tests on real Android hardware
4. Record each step with evidence via `recordStepResult()`
5. Generate report: `generateReport(sessionId)`
