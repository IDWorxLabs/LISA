package com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.audit

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation.GuidedLearningSimplificationAuthorityV1
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.guidedlearninginteractivelessons.validation.GuidedLearningInteractiveLessonsAuthorityV1
import com.idworx.lisa.features.guidedlearningsetupbeforehello.validation.GuidedLearningSetupBeforeHelloAuthorityV1
import com.idworx.lisa.features.guidedlessonprogresslabel.validation.GuidedLessonProgressLabelAuthorityV1
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1
import com.idworx.lisa.features.guidedsuccesstimingfix.validation.GuidedSuccessTimingFixAuthorityV1
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation.GuidedUiOverlapAndFalseBlinkFixAuthorityV1
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation.GuidedWrongBlinkRestartsSequenceAuthorityV1
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.validation.ValidationOutcome

object GuidedBlinkAcceptanceVisualFeedbackAuditor {

    private fun blinkCounterCallBlocks(components: String): List<String> {
        val panelStart = components.indexOf("fun LessonEyeStatusPanel")
        val panelEnd = components.indexOf("fun AnimatedBlinkCounterRow", panelStart)
        if (panelStart < 0 || panelEnd < 0) return emptyList()
        val panel = components.substring(panelStart, panelEnd)
        return Regex("AnimatedBlinkCounterRow\\(([^)]*)\\)")
            .findAll(panel)
            .map { it.groupValues[1] }
            .toList()
    }

    fun acceptedLeftBlinkAnimatesOnlyLeftCounter(): Boolean {
        val components = readComponents() ?: return false
        val leftCall = blinkCounterCallBlocks(components).firstOrNull { it.contains("\"Left blinks\"") }
            ?: return false
        return leftCall.contains("eyeTracking.leftBlinkCount") && !leftCall.contains("rightBlinkCount")
    }

    fun acceptedRightBlinkAnimatesOnlyRightCounter(): Boolean {
        val components = readComponents() ?: return false
        val rightCall = blinkCounterCallBlocks(components).firstOrNull { it.contains("\"Right blinks\"") }
            ?: return false
        return rightCall.contains("eyeTracking.rightBlinkCount") && !rightCall.contains("leftBlinkCount")
    }

    fun counterPulseCompletesSuccessfully(): Boolean {
        val block = counterRowBlock() ?: return false
        val durations = Regex("durationMillis = (\\d+)").findAll(block)
            .map { it.groupValues[1].toInt() }
            .toList()
        val totalDuration = durations.take(2).sum()
        return block.contains("count > previousCount") &&
            block.contains("pulse.animateTo(1f") &&
            block.contains("pulse.animateTo(0f") &&
            block.contains("previousCount = count") &&
            block.contains("1f + pulse.value * 0.18f") &&
            totalDuration in 250..300
    }

    fun indicatorFlashOccursOnlyForDetectedEye(): Boolean {
        val block = counterRowBlock() ?: return false
        return block.contains("CircleShape") &&
            block.contains("lerp(LisaGray.copy(alpha = 0.35f), LisaBlue, pulse.value)") &&
            block.contains("lerp(LisaBlueDark, LisaBlue, pulse.value)")
    }

    fun acceptedBlinkMessageAppearsAndAutoHides(): Boolean {
        val components = readComponents() ?: return false
        val start = components.indexOf("fun AcceptedBlinkMessage")
        if (start < 0) return false
        val block = components.substring(start, minOf(start + 1800, components.length))
        val lessons = readLessonScreens() ?: return false
        return block.contains("fadeIn(") &&
            block.contains("fadeOut(") &&
            block.contains("delay(600L)") &&
            block.contains("visible = true") &&
            block.contains("visible = false") &&
            block.contains("Left blink detected") &&
            block.contains("Right blink detected") &&
            lessons.contains("AcceptedBlinkMessage(")
    }

    fun multipleAcceptedBlinksTriggerMultipleAnimations(): Boolean {
        val components = readComponents() ?: return false
        val rowStart = components.indexOf("fun AnimatedBlinkCounterRow")
        val msgStart = components.indexOf("fun AcceptedBlinkMessage")
        if (rowStart < 0 || msgStart < 0) return false
        val rowBlock = components.substring(rowStart, msgStart)
        val msgBlock = components.substring(msgStart, minOf(msgStart + 1800, components.length))
        return rowBlock.contains("LaunchedEffect(count)") &&
            rowBlock.contains("count > previousCount") &&
            msgBlock.contains("LaunchedEffect(leftCount, rightCount)") &&
            msgBlock.contains("leftCount > previousLeft") &&
            msgBlock.contains("rightCount > previousRight")
    }

    fun wrongEyeBlinkDoesNotTriggerAcceptedAnimation(): Boolean {
        val main = readMainActivity() ?: return false
        val start = main.indexOf("private fun processSequenceWinks")
        if (start < 0) return false
        val end = main.indexOf("private fun updateSequencePauseState", start)
        if (end < 0) return false
        val block = main.substring(start, end)
        val leftRejectIdx = block.indexOf("rejectLessonWrongEyeBlink(isLeft = true)")
        val leftIncrementIdx = block.indexOf("leftWinks += 1")
        val rightRejectIdx = block.indexOf("rejectLessonWrongEyeBlink(isLeft = false)")
        val rightIncrementIdx = block.indexOf("rightWinks += 1")
        return leftRejectIdx in 0 until leftIncrementIdx &&
            rightRejectIdx in 0 until rightIncrementIdx &&
            block.contains("if (rejectLessonWrongEyeBlink(isLeft = true)) return") &&
            block.contains("if (rejectLessonWrongEyeBlink(isLeft = false)) return")
    }

    fun partialTimeoutResetStillWorks(): Boolean {
        val controller = readController() ?: return false
        val start = controller.indexOf("fun applyPartialSequenceTimeout()")
        if (start < 0) return false
        val block = controller.substring(start, minOf(start + 700, controller.length))
        val resetsCounts = block.contains("liveLeftBlinks = 0") && block.contains("liveRightBlinks = 0")
        val rowBlock = counterRowBlock() ?: return false
        return resetsCounts && rowBlock.contains("count > previousCount")
    }

    fun phraseOnlySpeechUnchanged(): Boolean {
        val components = readComponents() ?: return false
        val rowStart = components.indexOf("fun AnimatedBlinkCounterRow")
        if (rowStart < 0) return false
        val newFeatureEnd = components.indexOf("fun TrainingSensitivityControls", rowStart)
        if (newFeatureEnd < 0) return false
        val newFeatureBlock = components.substring(rowStart, newFeatureEnd)
        return LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            !LisaSpeechPolicy.allowsNarration() &&
            !newFeatureBlock.contains("speak(") &&
            !newFeatureBlock.contains("MediaPlayer") &&
            !newFeatureBlock.contains("Vibrator")
    }

    fun existingGuidedLearningValidationsRemainGreen(): Boolean {
        val outcomes = listOf(
            GuidedLearningInteractiveLessonsAuthorityV1.validate().outcome,
            GuidedTrainingAuthorityV1.validate().outcome,
            GuidedCurriculumAndNavigationContextAuthorityV1.validate().outcome,
            GuidedSuccessTimingFixAuthorityV1.validate().outcome,
            GuidedWrongBlinkRestartsSequenceAuthorityV1.validate().outcome,
            GuidedUiOverlapAndFalseBlinkFixAuthorityV1.validate().outcome,
            GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1.validate().outcome,
            GuidedLessonProgressLabelAuthorityV1.validate().outcome,
            GuidedLearningSetupBeforeHelloAuthorityV1.validate().outcome,
            GuidedLearningSimplificationAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedBlinkAcceptanceVisualFeedbackAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedBlinkAcceptanceVisualFeedbackV1")
    }

    private fun counterRowBlock(): String? {
        val components = readComponents() ?: return null
        val start = components.indexOf("fun AnimatedBlinkCounterRow")
        if (start < 0) return null
        val end = components.indexOf("fun AcceptedBlinkMessage", start)
        if (end < 0) return null
        return components.substring(start, end)
    }

    private fun readComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )
}
