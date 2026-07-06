package com.idworx.lisa.features.guideduioverlapandfalseblinkfix.audit

import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.blinkdetectionreliability.simulateAcceptedLeftWink
import com.idworx.lisa.features.blinkdetectionreliability.simulateAcceptedRightWink
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.metadata.GuidedUiOverlapAndFalseBlinkFixMetadata
import com.idworx.lisa.features.onboardingguide.ui.guidedLessonPhraseFontSize
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedUiOverlapAndFalseBlinkFixAuditor {

    fun longPhraseTextWrapsWithoutOverlap(): Boolean {
        val components = readTrainingComponents() ?: return false
        val lessons = readLessonScreens() ?: return false
        return components.contains("GuidedLessonPhraseTitle") &&
            components.contains("maxLines = 3") &&
            lessons.contains("GuidedLessonPhraseTitle") &&
            lessons.contains("verticalScroll")
    }

    fun longPhraseSamplesUseSmallerFont(): Boolean =
        GuidedUiOverlapAndFalseBlinkFixMetadata.LONG_PHRASE_SAMPLES.all { phrase ->
            guidedLessonPhraseFontSize(phrase).value <= 28f
        }

    fun lessonScreenHasSectionSpacing(): Boolean {
        val lessons = readLessonScreens() ?: return false
        return lessons.contains("padding(bottom = 24.dp)") &&
            lessons.contains("padding(bottom = 28.dp)") &&
            lessons.contains("Spacer(Modifier.height(28.dp))")
    }

    fun phraseTitleSeparatedFromGestureInstruction(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val phraseIndex = lessons.indexOf("GuidedLessonPhraseTitle")
        val gestureIndex = lessons.indexOf("SimplifiedGestureDisplay")
        return phraseIndex in 0 until gestureIndex
    }

    fun guidedLearningRemainsMinimal(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val start = lessons.indexOf("fun CommunicationLessonScreen(")
        val end = lessons.indexOf("fun NavigationLessonScreen(", start)
        if (start < 0 || end < 0) return false
        val block = lessons.substring(start, end)
        return !block.contains("TrainingProgressIndicator") &&
            !block.contains("NarrationControls") &&
            !block.contains("CaregiverCoachProgressStrip")
    }

    fun blinkRequiresOpenCloseOpenPattern(): Boolean {
        val processor = readProcessorSource() ?: return false
        return processor.contains("openPrimingFrames") &&
            processor.contains("leftClosingFrames") &&
            processor.contains("bothOpen") &&
            processor.contains("leftPrimingFrames")
    }

    fun blinkRejectedWithoutOpenPriming(): Boolean {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 2,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        val closed = BlinkEyeProbabilities(0.08f, 0.92f)
        val reopen = BlinkEyeProbabilities(0.92f, 0.92f)
        val closeResult = processor.processFrame(closed, 100L, 0, 0)
        val reopenResult = processor.processFrame(reopen, 120L, 0, 0)
        return !reopenResult.acceptLeft && closeResult.rejectedUnprimed
    }

    fun blinkRejectedDuringUnstableTracking(): Boolean {
        val tuning = BlinkDetectionTuning.default
        val processor = BlinkDetectionProcessor(tuning)
        val uncertain = BlinkEyeProbabilities(0.50f, 0.52f)
        val result = processor.processFrame(uncertain, 100L, 0, 0)
        return result.skippedUnstable || result.skippedBothUncertain
    }

    fun shortProbabilityDipDoesNotCountAsBlink(): Boolean {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 2,
            openPrimingFrames = 2,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        val open = BlinkEyeProbabilities(0.92f, 0.92f)
        repeat(tuning.openPrimingFrames) {
            processor.processFrame(open, it * 16L, 0, 0)
        }
        processor.processFrame(BlinkEyeProbabilities(0.08f, 0.92f), 100L, 0, 0)
        val reopen = processor.processFrame(open, 120L, 0, 0)
        return !reopen.acceptLeft && reopen.rejectedIncompleteShape
    }

    fun normalIntentionalBlinkAccepted(): Boolean {
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

    fun doubleBlinkStillWorks(): Boolean {
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

    fun sensitivityChangesThresholds(): Boolean {
        val low = BlinkDetectionTuning.forSensitivityLevel(1)
        val high = BlinkDetectionTuning.forSensitivityLevel(10)
        return low.closedEyeThreshold < high.closedEyeThreshold &&
            low.openPrimingFrames >= high.openPrimingFrames
    }

    fun acceptedBlinkUpdatesVisibleFeedback(): Boolean {
        val main = readMainActivity() ?: return false
        val components = readTrainingComponents() ?: return false
        return main.contains("flashAcceptedBlink") &&
            main.contains("acceptedBlinkLabel") &&
            components.contains("acceptedBlinkLabel")
    }

    fun sequenceFinalizesAfterThreeSeconds(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("shouldFinalizeSequence") &&
            SEQUENCE_IDLE_TIMEOUT_MS == 3000L
    }

    fun phraseSpeechOnlyPreserved(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY && !LisaSpeechPolicy.allowsNarration()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedUiOverlapAndFalseBlinkFixAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedUiOverlapAndFalseBlinkFixV1")
    }

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readTrainingComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readProcessorSource(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/blinkdetectionreliability/BlinkDetectionProcessor.kt"
    )
}
