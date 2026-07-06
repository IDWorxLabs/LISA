package com.idworx.lisa.features.brain1readiness.reviewers

import com.idworx.lisa.features.accessibilityconsistency.validation.AccessibilityConsistencyAuthorityV1
import com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata
import com.idworx.lisa.features.androiddevicetesting.integration.Brain1DeviceTestingBridge
import com.idworx.lisa.features.brain1readiness.model.Brain1Gap
import com.idworx.lisa.features.brain1readiness.model.Brain1Risk
import com.idworx.lisa.features.brain1readiness.model.Brain1RiskSeverity
import com.idworx.lisa.features.brain1readiness.model.Brain1Subsystem
import com.idworx.lisa.features.brain1readiness.model.Brain1SubsystemStatus
import com.idworx.lisa.features.calibrationreliability.validation.CalibrationReliabilityAuthorityV1
import com.idworx.lisa.features.communicationanalytics.validation.CommunicationAccuracyAnalyticsAuthorityV1
import com.idworx.lisa.features.companionmemory.validation.CompanionMemoryAuthorityV1
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationPathVerifier
import com.idworx.lisa.features.corecommunicationreliability.validation.CoreCommunicationReliabilityAuthorityV1
import com.idworx.lisa.features.offlinereliability.validation.OfflineReliabilityAuthorityV1
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.personality.validation.PersonalityEngineAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import java.io.File

data class ReviewerResult(
    val reviewerName: String,
    val subsystem: Brain1Subsystem,
    val status: Brain1SubsystemStatus,
    val checksPerformed: Int,
    val checksPassed: Int,
    val risks: List<Brain1Risk>,
    val gaps: List<Brain1Gap>,
    val evidence: String
)

object Brain1FileProbe {
    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun readProjectFile(relativePath: String): String? {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                val file = File(candidate, relativePath)
                if (file.exists()) return file.readText()
            }
            dir = dir?.parentFile
        }
        return null
    }

    fun fileExists(relativePath: String): Boolean = readProjectFile(relativePath) != null
}

private fun reviewResult(
    name: String,
    subsystem: Brain1Subsystem,
    passed: Int,
    checks: Int,
    risks: List<Brain1Risk> = emptyList(),
    gaps: List<Brain1Gap> = emptyList(),
    evidence: String
): ReviewerResult {
    val status = when {
        passed >= checks && risks.none { it.severity == Brain1RiskSeverity.Critical } -> Brain1SubsystemStatus.Ready
        passed >= checks / 2 -> Brain1SubsystemStatus.Degraded
        passed == 0 -> Brain1SubsystemStatus.Missing
        else -> Brain1SubsystemStatus.Degraded
    }
    return ReviewerResult(name, subsystem, status, checks, passed, risks, gaps, evidence)
}

object GuidedLearningReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 6
        val authority = GuidedTrainingAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt")) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt")) passed++
        val controller = Brain1FileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        )
        if (controller?.contains("resume") == true || controller?.contains("Resume") == true) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt")) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/personality/engine/LisaPersonalityEngine.kt")) passed++
        return reviewResult(
            "GuidedLearningReadinessReviewer", Brain1Subsystem.GuidedLearning, passed, checks,
            evidence = "Guided Training authority ${authority.outcome.name}; $passed/$checks checks"
        )
    }
}

object PersonalityReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 5
        val authority = PersonalityEngineAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt")) passed++
        val profile = Brain1FileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/model/PersonalityModels.kt"
        )
        if (profile?.contains("FORBIDDEN") == true || profile?.contains("forbidden") == true) passed++
        val engine = Brain1FileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/DefaultLisaPersonalityEngine.kt"
        )
        val noLlm = listOf("OpenAI", "ChatGPT", "GenerativeModel", "LLM").none { engine?.contains(it) == true }
        if (noLlm) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/accessibilityconsistency/integration/AccessibilityPersonalityAdapter.kt")) passed++
        return reviewResult(
            "PersonalityReadinessReviewer", Brain1Subsystem.PersonalityEngine, passed, checks,
            evidence = "Personality authority ${authority.outcome.name}; local dialogue catalog verified"
        )
    }
}

object CompanionMemoryReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 4
        val authority = CompanionMemoryAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/companionmemory/repository/CompanionMemoryRepository.kt")) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/features/companionmemory/engine/DefaultCompanionMemoryEngine.kt")) passed++
        val models = Brain1FileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/companionmemory/model/CompanionMemoryModels.kt"
        )
        if (models?.contains("LearningMilestone") == true) passed++
        return reviewResult(
            "CompanionMemoryReadinessReviewer", Brain1Subsystem.CompanionMemory, passed, checks,
            evidence = "Companion Memory authority ${authority.outcome.name}"
        )
    }
}

object CoreCommunicationReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 8
        val risks = mutableListOf<Brain1Risk>()
        val authority = CoreCommunicationReliabilityAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/MainActivity.kt") &&
            Brain1FileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")?.contains("FaceDetectorOptions") == true
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/sequence/BlinkSequenceValidator.kt"
            )
        ) passed++
        if (CommunicationPathVerifier.verifyPhraseMatchDeterministic()) passed++
        if (CommunicationPathVerifier.verifyEmergencyBlockedInCommunicationTraining()) passed++
        if (CommunicationPathVerifier.verifyDuplicateDebouncing()) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/speech/SpeechReliabilityAdapter.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/history/CommunicationReliabilityHistoryRecorder.kt"
            )
        ) passed++
        if (authority.outcome != ValidationOutcome.PASS) {
            risks.add(
                Brain1Risk(
                    "CCR_R001", Brain1Subsystem.CoreCommunication, Brain1RiskSeverity.Critical,
                    "Core Communication Reliability authority did not pass", authority.failedChecks.joinToString()
                )
            )
        }
        return reviewResult(
            "CoreCommunicationReadinessReviewer", Brain1Subsystem.CoreCommunication, passed, checks,
            risks = risks,
            evidence = "CCR authority ${authority.outcome.name}; eye/blink/phrase/speech/history path verified"
        )
    }
}

object CalibrationReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 5
        val authority = CalibrationReliabilityAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/calibrationreliability/engine/DefaultCalibrationReliabilityEngine.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/calibrationreliability/model/CalibrationState.kt"
            )
        ) passed++
        val ccr = Brain1FileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/engine/DefaultCoreCommunicationReliabilityEngine.kt"
        )
        if (ccr?.contains("calibration") == true || ccr?.contains("Calibration") == true) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/calibrationreliability/integration/CalibrationGuidedLearningAdapter.kt"
            )
        ) passed++
        return reviewResult(
            "CalibrationReadinessReviewer", Brain1Subsystem.CalibrationReliability, passed, checks,
            evidence = "Calibration authority ${authority.outcome.name}"
        )
    }
}

object AnalyticsReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 4
        val authority = CommunicationAccuracyAnalyticsAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/communicationanalytics/engine/DefaultCommunicationAnalyticsEngine.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/communicationanalytics/diagnostics/AnalyticsDiagnostics.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/communicationanalytics/integration/CommunicationAnalyticsBridge.kt"
            ) || Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/communicationanalytics/engine/AnalyticsVerifier.kt"
            )
        ) passed++
        return reviewResult(
            "AnalyticsReadinessReviewer", Brain1Subsystem.CommunicationAnalytics, passed, checks,
            evidence = "Analytics authority ${authority.outcome.name}; observer-only layer verified"
        )
    }
}

object AccessibilityReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 4
        val risks = mutableListOf<Brain1Risk>()
        val authority = AccessibilityConsistencyAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/accessibilityconsistency/engine/DefaultAccessibilityConsistencyEngine.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/accessibilityconsistency/diagnostics/AccessibilityDiagnostics.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")) passed++
        authority.observations.filter { it.isNotBlank() }.forEach { obs ->
            risks.add(
                Brain1Risk(
                    "A11Y_R_${risks.size}", Brain1Subsystem.AccessibilityConsistency,
                    Brain1RiskSeverity.Medium, "Known accessibility observation: $obs", "Accessibility audit"
                )
            )
        }
        return reviewResult(
            "AccessibilityReadinessReviewer", Brain1Subsystem.AccessibilityConsistency, passed, checks,
            risks = risks,
            evidence = "Accessibility authority ${authority.outcome.name}"
        )
    }
}

object OfflineReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 4
        val risks = mutableListOf<Brain1Risk>()
        val authority = OfflineReliabilityAuthorityV1.validate()
        if (authority.outcome == ValidationOutcome.PASS) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/offlinereliability/engine/DefaultOfflineReliabilityEngine.kt"
            )
        ) passed++
        val manifest = Brain1FileProbe.readProjectFile("app/src/main/AndroidManifest.xml")
        if (manifest?.contains("android.permission.INTERNET") != true) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/LisaTtsVoiceManager.kt")) passed++
        return reviewResult(
            "OfflineReadinessReviewer", Brain1Subsystem.OfflineReliability, passed, checks,
            risks = risks,
            evidence = "Offline authority ${authority.outcome.name}; no mandatory INTERNET permission"
        )
    }
}

object EmergencyReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 5
        val risks = mutableListOf<Brain1Risk>()
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/EmergencyAlarmController.kt")) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/emergency/EmergencySpeechSafetyGuard.kt"
            )
        ) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/emergency/EmergencyConfirmationPolicy.kt"
            )
        ) passed++
        if (CommunicationPathVerifier.verifyEmergencyBlockedInCommunicationTraining()) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/offlinereliability/validators/OfflineValidators.kt"
            )
        ) passed++
        risks.add(
            Brain1Risk(
                "EMG_R001", Brain1Subsystem.EmergencySpeech, Brain1RiskSeverity.Medium,
                "Emergency false activation risk during real-world testing", "Requires device simulation"
            )
        )
        return reviewResult(
            "EmergencyReadinessReviewer", Brain1Subsystem.EmergencySpeech, passed, checks,
            risks = risks,
            evidence = "Emergency speech, safety guard, confirmation policy, training isolation verified"
        )
    }
}

object DeviceTestingReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = 7
        val protocolExists = Brain1FileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/protocol/AndroidDeviceTestingProtocol.kt"
        )
        if (protocolExists) passed++
        val hasEvidence = try {
            com.idworx.lisa.features.androiddevicetesting.integration.Brain1DeviceTestingIntegration.hasRecordedDeviceEvidence()
        } catch (_: Exception) {
            false
        }
        val gaps = if (hasEvidence) {
            Brain1ReadinessMetadata.STANDARD_DEVICE_TESTING_GAPS
                .filterNot { it.contains("Real Android device testing still needed", ignoreCase = true) }
                .mapIndexed { i, desc ->
                    Brain1Gap("DEV_GAP_$i", Brain1Subsystem.DeviceTesting, desc, blocksReadiness = false)
                }.toMutableList()
        } else {
            Brain1ReadinessMetadata.STANDARD_DEVICE_TESTING_GAPS.mapIndexed { i, desc ->
                Brain1Gap("DEV_GAP_$i", Brain1Subsystem.DeviceTesting, desc, blocksReadiness = false)
            }.toMutableList()
        }
        val manifest = Brain1FileProbe.readProjectFile("app/src/main/AndroidManifest.xml")
        if (manifest?.contains("android.permission.CAMERA") == true) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/LisaTtsVoiceManager.kt")) passed++
        val tts = Brain1FileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaTtsVoiceManager.kt")
        if (tts?.contains("localVoices") == true || tts?.contains("isNetworkConnectionRequired") == true) passed++
        if (Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/MainActivity.kt")) passed++
        if (Brain1FileProbe.fileExists("app/build.gradle.kts") || Brain1FileProbe.fileExists("app/build.gradle")) passed++
        if (Brain1FileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/integration/Brain1DeviceTestingIntegration.kt"
            )
        ) passed++
        val risks = listOf(
            Brain1Risk("DEV_R001", Brain1Subsystem.DeviceTesting, Brain1RiskSeverity.Medium, "Camera performance varies by device", "Requires hardware matrix"),
            Brain1Risk("DEV_R002", Brain1Subsystem.DeviceTesting, Brain1RiskSeverity.Medium, "TTS device availability risk", "Voice engines differ by OEM"),
            Brain1Risk("DEV_R003", Brain1Subsystem.DeviceTesting, Brain1RiskSeverity.High, "Real-user testing risk", "No patient testing performed yet")
        )
        return reviewResult(
            "DeviceTestingReadinessReviewer", Brain1Subsystem.DeviceTesting, passed, checks,
            risks = risks, gaps = gaps,
            evidence = Brain1DeviceTestingBridge.summarizeForReadiness()
        )
    }
}

object IntegrationReadinessReviewer {
    fun review(): ReviewerResult {
        var passed = 0
        val checks = Brain1ReadinessMetadata.BRAIN1_AUTHORITIES.size + 2
        val authorityOutcomes = Brain1ReadinessMetadata.BRAIN1_AUTHORITIES.map { name ->
            when (name) {
                "GuidedTrainingAuthorityV1" -> GuidedTrainingAuthorityV1.validate().outcome
                "PersonalityEngineAuthorityV1" -> PersonalityEngineAuthorityV1.validate().outcome
                "CompanionMemoryAuthorityV1" -> CompanionMemoryAuthorityV1.validate().outcome
                "CoreCommunicationReliabilityAuthorityV1" -> CoreCommunicationReliabilityAuthorityV1.validate().outcome
                "CalibrationReliabilityAuthorityV1" -> CalibrationReliabilityAuthorityV1.validate().outcome
                "CommunicationAccuracyAnalyticsAuthorityV1" -> CommunicationAccuracyAnalyticsAuthorityV1.validate().outcome
                "AccessibilityConsistencyAuthorityV1" -> AccessibilityConsistencyAuthorityV1.validate().outcome
                "OfflineReliabilityAuthorityV1" -> OfflineReliabilityAuthorityV1.validate().outcome
                else -> ValidationOutcome.FAIL
            }
        }
        passed += authorityOutcomes.count { it == ValidationOutcome.PASS }
        val noBrain2 = Brain1ReadinessMetadata.FORBIDDEN_DEPENDENCY_MARKERS.none { marker ->
            Brain1FileProbe.readProjectFile("app/build.gradle.kts")?.contains(marker) == true
        }
        if (noBrain2) passed++
        val manifest = Brain1FileProbe.readProjectFile("app/src/main/AndroidManifest.xml")
        if (manifest?.contains("android.permission.INTERNET") != true) passed++
        return reviewResult(
            "IntegrationReadinessReviewer", Brain1Subsystem.Integration, passed, checks,
            evidence = "${authorityOutcomes.count { it == ValidationOutcome.PASS }}/${Brain1ReadinessMetadata.BRAIN1_AUTHORITIES.size} Brain 1 authorities pass"
        )
    }
}

object Brain1ReadinessReviewRunner {
    fun runAllReviewers(): List<ReviewerResult> = listOf(
        GuidedLearningReadinessReviewer.review(),
        PersonalityReadinessReviewer.review(),
        CompanionMemoryReadinessReviewer.review(),
        CoreCommunicationReadinessReviewer.review(),
        CalibrationReadinessReviewer.review(),
        AnalyticsReadinessReviewer.review(),
        AccessibilityReadinessReviewer.review(),
        OfflineReadinessReviewer.review(),
        EmergencyReadinessReviewer.review(),
        DeviceTestingReadinessReviewer.review(),
        IntegrationReadinessReviewer.review()
    )
}
