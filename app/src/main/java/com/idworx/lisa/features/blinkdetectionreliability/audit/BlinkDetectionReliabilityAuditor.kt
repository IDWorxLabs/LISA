package com.idworx.lisa.features.blinkdetectionreliability.audit

import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.simulateAcceptedLeftWink
import com.idworx.lisa.features.blinkdetectionreliability.simulateAcceptedRightWink
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object BlinkDetectionReliabilityAuditor {

    fun processorModuleExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/blinkdetectionreliability/BlinkDetectionProcessor.kt"
        )

    fun mainActivityUsesProcessor(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("BlinkDetectionProcessor") &&
            main.contains("blinkProcessor.processFrame")
    }

    fun cooldownReducedFromLegacy(): Boolean =
        BlinkDetectionTuning.WINK_COOLDOWN_MS <= 550L

    fun quickValidBlinkAccepted(): Boolean {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        return processor.simulateAcceptedLeftWink(tuning).acceptLeft
    }

    fun normalBlinkRequiresMinimalFrames(): Boolean {
        val tuning = BlinkDetectionTuning.forSensitivityLevel(3)
        return tuning.requiredWinkFrames <= 2
    }

    fun cooldownDoesNotBlockIntentionalDoubleBlink(): Boolean {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = BlinkDetectionTuning.WINK_COOLDOWN_MS
        )
        val processor = BlinkDetectionProcessor(tuning)
        val first = processor.simulateAcceptedRightWink(tuning, nowMs = 1000L)
        val second = processor.simulateAcceptedRightWink(
            tuning,
            nowMs = 1000L + BlinkDetectionTuning.WINK_COOLDOWN_MS + 20L,
            acceptedRight = 1
        )
        return first.acceptRight && second.acceptRight
    }

    fun jitterTolerantDuringActiveSequence(): Boolean {
        val tuning = BlinkDetectionTuning.default
        val processor = BlinkDetectionProcessor(tuning)
        processor.processFrame(BlinkEyeProbabilities(0.85f, 0.88f), 0L, 1, 0)
        val jittery = processor.processFrame(BlinkEyeProbabilities(0.15f, 0.90f), 100L, 1, 0)
        return !jittery.skippedForJitter || jittery.leftCandidate
    }

    fun lessonFeedbackUpdatesOnWink(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("onLessonWink") &&
            main.contains("trainingSession.updateWinkDots")
    }

    fun noisyBlinkDoesNotFinalizeEarly(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("shouldFinalizeSequence") &&
            SEQUENCE_IDLE_TIMEOUT_MS == 3000L &&
            main.contains("hasCountedWinks")
    }

    fun diagnosticsPanelBehindDeveloperMode(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val main = readMainActivity() ?: return false
        return lessons.contains("BlinkDetectionDiagnosticsPanel") &&
            lessons.contains("showBlinkDiagnostics") &&
            main.contains("showBlinkDiagnostics = uiDeveloperMode.value")
    }

    fun eyesNotDetectedFeedback(): Boolean {
        val lessons = readLessonScreens() ?: return false
        return lessons.contains("Eyes not detected")
    }

    fun phraseSpeechOnly(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY && !LisaSpeechPolicy.allowsNarration()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/BlinkDetectionReliabilityTuningAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaBlinkDetectionReliabilityTuningV1")
    }

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )
}
