package com.idworx.lisa.features.androiddevicetesting.suites

import com.idworx.lisa.features.androiddevicetesting.metadata.AndroidDeviceTestingMetadata
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestCase
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestOutcome
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestStep
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestSuite

private fun steps(vararg pairs: Pair<String, String>): List<DeviceTestStep> =
    pairs.map { (id, desc) ->
        DeviceTestStep(stepId = id, description = desc, outcome = DeviceTestOutcome.NOT_TESTED)
    }

private fun case(id: String, title: String, stepPairs: List<Pair<String, String>>): DeviceTestCase =
    DeviceTestCase(caseId = id, title = title, steps = steps(*stepPairs.toTypedArray()))

private fun suite(id: String, name: String, cases: List<DeviceTestCase>): DeviceTestSuite =
    DeviceTestSuite(suiteId = id, name = name, cases = cases, outcome = DeviceTestOutcome.NOT_TESTED)

object LaunchAndPermissionTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_LAUNCH", "Launch and Permission",
        listOf(
            case("LAUNCH_001", "App install and launch", listOf(
                "LP_001" to "App installs successfully",
                "LP_002" to "App launches without crash",
                "LP_003" to "Main communication screen can open"
            )),
            case("LAUNCH_002", "Camera permission", listOf(
                "LP_004" to "Camera permission requested clearly",
                "LP_005" to "Camera permission denial handled safely",
                "LP_006" to "Camera permission granted flow works"
            )),
            case("LAUNCH_003", "Initial readiness", listOf(
                "LP_007" to "TTS availability checked",
                "LP_008" to "Settings accessible",
                "LP_009" to "Guided Learning can start"
            ))
        )
    )
}

object CalibrationDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_CALIBRATION", "Calibration Device",
        listOf(
            case("CAL_001", "Calibration flow", listOf(
                "CAL_001" to "Calibration starts",
                "CAL_002" to "Calibration instructions are understandable",
                "CAL_003" to "Calibration captures required points"
            )),
            case("CAL_002", "Calibration reliability", listOf(
                "CAL_004" to "Calibration handles interruption",
                "CAL_005" to "Calibration resumes or restarts safely",
                "CAL_006" to "Calibration score generated",
                "CAL_007" to "Poor calibration detected",
                "CAL_008" to "Recalibration guidance shown",
                "CAL_009" to "Calibration health affects communication reliability"
            ))
        )
    )
}

object EyeTrackingDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_EYE_TRACKING", "Eye Tracking Device",
        listOf(
            case("EYE_001", "Camera and detection", listOf(
                "EYE_001" to "Front camera initializes",
                "EYE_002" to "Face/eye detection starts",
                "EYE_003" to "Eye direction detection works under normal lighting"
            )),
            case("EYE_002", "Gaze directions", listOf(
                "EYE_004" to "Left gaze detection works",
                "EYE_005" to "Right gaze detection works",
                "EYE_006" to "Center gaze detection works"
            )),
            case("EYE_003", "Tracking resilience", listOf(
                "EYE_007" to "Tracking loss is handled",
                "EYE_008" to "Tracking recovery works",
                "EYE_009" to "No crash when face leaves frame"
            ))
        )
    )
}

object BlinkDetectionDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_BLINK", "Blink Detection Device",
        listOf(
            case("BLINK_001", "Blink types", listOf(
                "BL_001" to "Single blink detected",
                "BL_002" to "Left blink detected if supported",
                "BL_003" to "Right blink detected if supported",
                "BL_004" to "Both-eye blink detected if supported"
            )),
            case("BLINK_002", "Blink reliability", listOf(
                "BL_005" to "False blink noise is manageable",
                "BL_006" to "Repeated blink sequences detected",
                "BL_007" to "Long blink does not cause duplicate activation",
                "BL_008" to "Blink sensitivity setting affects detection where supported"
            ))
        )
    )
}

object CommunicationPathDeviceTestSuite {
    fun build(): DeviceTestSuite {
        val phraseCases = AndroidDeviceTestingMetadata.STANDARD_PHRASES.mapIndexed { i, phrase ->
            case(
                "COMM_PHRASE_${i + 1}",
                "Phrase: $phrase",
                listOf(
                    "CP_${i}_1" to "Eye movement captured for '$phrase'",
                    "CP_${i}_2" to "Blink sequence recognized for '$phrase'",
                    "CP_${i}_3" to "Phrase match confirmed for '$phrase'",
                    "CP_${i}_4" to "TTS speech output for '$phrase'",
                    "CP_${i}_5" to "Communication history recorded for '$phrase'"
                )
            )
        }
        return suite(
            "SUITE_COMM_PATH", "Communication Path Device",
            phraseCases + case("COMM_PATH", "Full path integration", listOf(
                "CP_FULL_1" to "Analytics record observed (passive)",
                "CP_FULL_2" to "Companion Memory milestone where appropriate",
                "CP_FULL_3" to "Phrase confirmation policy respected where required"
            ))
        )
    }
}

object GuidedLearningDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_GUIDED_LEARNING", "Guided Learning Device",
        listOf(
            case("GL_001", "First launch", listOf(
                "GL_001" to "First launch guided learning appears",
                "GL_002" to "Welcome narration works",
                "GL_003" to "Begin Learning works",
                "GL_004" to "Skip Tutorial works"
            )),
            case("GL_002", "Progress and lessons", listOf(
                "GL_005" to "Progress persists after restart",
                "GL_006" to "Resume incomplete learning works",
                "GL_007" to "Communication lessons work",
                "GL_008" to "Navigation lessons work",
                "GL_009" to "Personality dialogue plays",
                "GL_010" to "Companion Memory records milestones",
                "GL_011" to "Graduation flow works",
                "GL_012" to "Replay tutorial from settings works"
            ))
        )
    )
}

object EmergencySafetyDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_EMERGENCY", "Emergency Safety Device",
        listOf(
            case("EMG_001", "Training isolation", listOf(
                "EM_001" to "Emergency training does not trigger real emergency action",
                "EM_002" to "Emergency sequence requires correct mode",
                "EM_003" to "Emergency duplicate activation prevented"
            )),
            case("EMG_002", "Emergency speech", listOf(
                "EM_004" to "Emergency confirmation policy works",
                "EM_005" to "Emergency speech works",
                "EM_006" to "Emergency cancellation or safety fallback works where available",
                "EM_007" to "Emergency history or analytics recorded",
                "EM_008" to "Offline emergency speech works"
            ))
        )
    )
}

object OfflineDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_OFFLINE", "Offline Device",
        listOf(
            case("OFF_001", "Offline operation", listOf(
                "OFF_001" to "App launches offline",
                "OFF_002" to "Guided Learning works offline",
                "OFF_003" to "Calibration works offline",
                "OFF_004" to "Eye tracking works offline",
                "OFF_005" to "Blink detection works offline",
                "OFF_006" to "Phrase speech works offline",
                "OFF_007" to "Personality dialogue works offline",
                "OFF_008" to "Companion Memory works offline",
                "OFF_009" to "Communication history works offline",
                "OFF_010" to "Emergency speech works offline",
                "OFF_011" to "Settings work offline",
                "OFF_012" to "No blocking cloud dependency appears"
            ))
        )
    )
}

object AccessibilityDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_ACCESSIBILITY", "Accessibility Device",
        listOf(
            case("A11Y_001", "Readability", listOf(
                "A11Y_001" to "Large text readable",
                "A11Y_002" to "High contrast readable",
                "A11Y_003" to "Touch targets usable",
                "A11Y_004" to "No clipped text at large font scale"
            )),
            case("A11Y_002", "Workflow accessibility", listOf(
                "A11Y_005" to "Guided Learning readable",
                "A11Y_006" to "Communication phrases readable",
                "A11Y_007" to "Settings readable",
                "A11Y_008" to "Emergency controls understandable",
                "A11Y_009" to "Screen reader compatibility where applicable",
                "A11Y_010" to "Orientation and screen scaling behavior"
            ))
        )
    )
}

object LightingConditionDeviceTestSuite {
    fun build(): DeviceTestSuite {
        val cases = AndroidDeviceTestingMetadata.LIGHTING_CONDITIONS.mapIndexed { i, condition ->
            case("LIGHT_$i", "Lighting: $condition", listOf(
                "LC_${i}_1" to "Eye tracking under $condition",
                "LC_${i}_2" to "Calibration score under $condition",
                "LC_${i}_3" to "Blink detection under $condition",
                "LC_${i}_4" to "Communication success under $condition"
            ))
        }
        return suite("SUITE_LIGHTING", "Lighting Condition Device", cases)
    }
}

object PhonePositionDeviceTestSuite {
    fun build(): DeviceTestSuite {
        val cases = AndroidDeviceTestingMetadata.PHONE_POSITIONS.mapIndexed { i, position ->
            case("POS_$i", "Position: $position", listOf(
                "PP_${i}_1" to "Calibration impact at $position",
                "PP_${i}_2" to "Communication impact at $position"
            ))
        }
        return suite("SUITE_PHONE_POSITION", "Phone Position Device", cases)
    }
}

object LongSessionStabilityTestSuite {
    fun build(): DeviceTestSuite {
        val cases = AndroidDeviceTestingMetadata.LONG_SESSION_DURATIONS_MIN.map { minutes ->
            case("LONG_$minutes", "${minutes}-minute session", listOf(
                "LS_${minutes}_1" to "App stability after ${minutes} minutes",
                "LS_${minutes}_2" to "Camera stability after ${minutes} minutes",
                "LS_${minutes}_3" to "TTS stability after ${minutes} minutes",
                "LS_${minutes}_4" to "Tracking drift after ${minutes} minutes",
                "LS_${minutes}_5" to "Communication consistency after ${minutes} minutes",
                "LS_${minutes}_6" to "Device heat observation after ${minutes} minutes",
                "LS_${minutes}_7" to "Battery drain observation after ${minutes} minutes"
            ))
        }
        return suite("SUITE_LONG_SESSION", "Long Session Stability", cases)
    }
}

object PerformanceDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_PERFORMANCE", "Performance Device",
        listOf(
            case("PERF_001", "Timing observations", listOf(
                "PF_001" to "App launch time recorded",
                "PF_002" to "Calibration time recorded",
                "PF_003" to "Phrase recognition time recorded",
                "PF_004" to "Time from sequence complete to speech recorded",
                "PF_005" to "UI responsiveness acceptable",
                "PF_006" to "Camera startup time recorded",
                "PF_007" to "Crash-free session confirmed"
            ))
        )
    )
}

object PhaseAFirstFiveMinutesDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_PHASE_A_FIRST_FIVE", "Phase A — First Five Minutes",
        listOf(
            case("PHASE_A_001", "First launch and confirmation", listOf(
                "P5M_001" to "First launch choice is eye-driven (L2 / R2)",
                "P5M_002" to "Choice confirmation works (L1 R1)",
                "P5M_003" to "Cancel returns to choice screen (R1 L1)",
                "P5M_004" to "No single-blink Brain 1 interaction command",
                "P5M_005" to "No L2 R2 cancel remains in Brain 1 decisions"
            )),
            case("PHASE_A_002", "Meet Lisa through calibration", listOf(
                "P5M_006" to "Meet Lisa narration plays",
                "P5M_007" to "Getting Ready narration plays",
                "P5M_008" to "Face detection messaging works",
                "P5M_009" to "Calibration dot is visible and animated",
                "P5M_010" to "Calibration transitions are clear"
            )),
            case("PHASE_A_003", "First communication success", listOf(
                "P5M_011" to "HELLO uses easy L2 gesture",
                "P5M_012" to "First success celebration works",
                "P5M_013" to "Failed attempts handled gently"
            ))
        )
    )
}

object PhaseBCommunicationWorkspaceDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_PHASE_B_WORKSPACE", "Phase B — Communication Workspace",
        listOf(
            case("PHASE_B_001", "Workspace entry and clarity", listOf(
                "P5B_001" to "Workspace entry intro plays after onboarding",
                "P5B_002" to "Context hint visible on vocabulary screen",
                "P5B_003" to "Caregiver gesture legend visible",
                "P5B_004" to "Category menu opens with L4 R4"
            )),
            case("PHASE_B_002", "Navigation and phrases", listOf(
                "P5B_005" to "Phrase selection speaks TTS",
                "P5B_006" to "L2 R2 back returns to previous screen",
                "P5B_007" to "L2/R2 scroll phrase pages",
                "P5B_008" to "No accidental single-blink activation"
            )),
            case("PHASE_B_003", "Safety and fatigue", listOf(
                "P5B_009" to "Emergency L6 R0 requires confirmation",
                "P5B_010" to "Patience hints present without spam",
                "P5B_011" to "Overlay readable on phone screen size"
            ))
        )
    )
}

object PatientCommunicationCoachDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_PATIENT_COACH", "Patient Communication Coach",
        listOf(
            case("PATIENT_COACH_001", "Coach pacing", listOf(
                "PCC_001" to "Lisa repeats phrase before advancing (2 successes)",
                "PCC_002" to "Patience prompt after 3 misses",
                "PCC_003" to "Quiet pause between lessons feels natural"
            )),
            case("PATIENT_COACH_002", "Caregiver visibility", listOf(
                "PCC_004" to "Lesson progress strip visible during training",
                "PCC_005" to "Daily essentials counter shown in settings",
                "PCC_006" to "Coach hint readable without blocking phrase"
            )),
            case("PATIENT_COACH_003", "Gesture progression", listOf(
                "PCC_007" to "First phrases use simple 2-blink sequences",
                "PCC_008" to "Difficulty bridge narration before harder pattern",
                "PCC_009" to "No sudden 3+ level gesture jumps"
            ))
        )
    )
}

object EmotionalPresenceDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_EMOTIONAL_PRESENCE", "Emotional Presence",
        listOf(
            case("EMOTIONAL_PRESENCE_001", "Warm without chatbot", listOf(
                "EP_001" to "Session opening is one calm line",
                "EP_002" to "Return greeting feels warm, not robotic",
                "EP_003" to "Lisa does not overtalk during idle waits"
            )),
            case("EMOTIONAL_PRESENCE_002", "Caregiver reassurance", listOf(
                "EP_004" to "Coach strip shows caregiver-friendly hint",
                "EP_005" to "Fatigue check-in suggests rest without pressure",
                "EP_006" to "Milestone moment feels meaningful, brief"
            ))
        )
    )
}

object CaregiverConfidenceDeviceTestSuite {
    fun build(): DeviceTestSuite = suite(
        "SUITE_CAREGIVER_CONFIDENCE", "Caregiver Confidence",
        listOf(
            case("CAREGIVER_CONFIDENCE_001", "Setup support", listOf(
                "CC_001" to "Phone positioning hint visible during setup",
                "CC_002" to "Lighting guidance is plain language",
                "CC_003" to "What-to-do-now line shown per setup step"
            )),
            case("CAREGIVER_CONFIDENCE_002", "Calibration and recovery", listOf(
                "CC_004" to "Calibration progress visible to caregiver",
                "CC_005" to "Poor calibration shows repositioning hint",
                "CC_006" to "Face-lost recovery hint appears without crash"
            )),
            case("CAREGIVER_CONFIDENCE_003", "Troubleshooting", listOf(
                "CC_007" to "Sensitivity troubleshooting is non-technical",
                "CC_008" to "Recalibration guidance uses Personality Engine",
                "CC_009" to "Hints hidden when tracking is healthy"
            ))
        )
    )
}

object DeviceTestSuiteRegistry {
    fun allSuites(): List<DeviceTestSuite> = listOf(
        LaunchAndPermissionTestSuite.build(),
        CalibrationDeviceTestSuite.build(),
        EyeTrackingDeviceTestSuite.build(),
        BlinkDetectionDeviceTestSuite.build(),
        CommunicationPathDeviceTestSuite.build(),
        GuidedLearningDeviceTestSuite.build(),
        PhaseAFirstFiveMinutesDeviceTestSuite.build(),
        PhaseBCommunicationWorkspaceDeviceTestSuite.build(),
        PatientCommunicationCoachDeviceTestSuite.build(),
        EmotionalPresenceDeviceTestSuite.build(),
        CaregiverConfidenceDeviceTestSuite.build(),
        EmergencySafetyDeviceTestSuite.build(),
        OfflineDeviceTestSuite.build(),
        AccessibilityDeviceTestSuite.build(),
        LightingConditionDeviceTestSuite.build(),
        PhonePositionDeviceTestSuite.build(),
        LongSessionStabilityTestSuite.build(),
        PerformanceDeviceTestSuite.build()
    )
}
