package com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.audit

import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedPartialTimeoutAndWrongEyeFeedbackAuditor {

    fun partialSequenceStartsCorrectly(): Boolean {
        val yes = TrainingLessonCatalog.communicationLessonAt(1) ?: return false
        val afterOne = LessonInteractionEngine.progressLabel(0, 1, listOf(false), yes)
        return afterOne == "Right blink 1 of 2" &&
            LessonInteractionEngine.isPartialSequenceInProgress(0, 1, listOf(false), yes)
    }

    fun partialSequenceResetsAfterFiveSeconds(): Boolean {
        val main = readMainActivity() ?: return false
        val engine = readLessonInteractionEngine() ?: return false
        return engine.contains("PARTIAL_SEQUENCE_IDLE_MS") &&
            engine.contains("5_000L") &&
            main.contains("lessonPartialSequenceTimeoutRunnable") &&
            main.contains("applyPartialSequenceTimeout")
    }

    fun resetKeepsUserOnSameLesson(): Boolean {
        val controller = readControllerSource() ?: return false
        val resetStart = controller.indexOf("fun applyPartialSequenceTimeout()")
        if (resetStart < 0) return false
        val block = controller.substring(resetStart, resetStart + 700)
        return !block.contains("SequenceSuccess") &&
            !block.contains("communicationLessonIndex +") &&
            !block.contains("navigator.reduce")
    }

    fun resetClearsBlinkCountsAndProgress(): Boolean {
        val controller = readControllerSource() ?: return false
        return controller.contains("applyPartialSequenceTimeout") &&
            controller.contains("detectedProgress = null") &&
            controller.contains("liveLeftBlinks = 0") &&
            controller.contains("liveRightBlinks = 0")
    }

    fun resetDoesNotTriggerSpeech(): Boolean {
        val controller = readControllerSource() ?: return false
        val resetStart = controller.indexOf("fun applyPartialSequenceTimeout()")
        if (resetStart < 0) return false
        val block = controller.substring(resetStart, resetStart + 700)
        return !block.contains("speakPhrase") &&
            !block.contains("narration.speak") &&
            !block.contains("speak(")
    }

    fun wrongEyeBlinkDoesNotRegister(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("rejectLessonWrongEyeBlink") &&
            main.contains("isWrongEyeBlink") &&
            main.contains("applyWrongEyeFeedback") &&
            main.contains("resetSequence()")
    }

    fun wrongEyeShowsRedFeedback(): Boolean {
        val lessons = readLessonScreens() ?: return false
        return lessons.contains("wrongEyeMessage") &&
            lessons.contains("LisaEmergencyRed")
    }

    fun wrongEyeFeedbackTellsUserToRestart(): Boolean {
        val engine = readLessonInteractionEngine() ?: return false
        return engine.contains("Wrong eye — start again") &&
            engine.contains("Wrong eye — blink left to start again") &&
            engine.contains("wrongEyeRestartFeedbackMessage")
    }

    fun wrongBlinkResetsProgressToZero(): Boolean {
        val controller = readControllerSource() ?: return false
        val resetStart = controller.indexOf("fun applyWrongEyeFeedback()")
        if (resetStart < 0) return false
        val block = controller.substring(resetStart, resetStart + 600)
        return block.contains("detectedProgress = null") &&
            block.contains("liveLeftBlinks = 0") &&
            block.contains("liveRightBlinks = 0")
    }

    fun userCanCompleteSequenceAfterCorrection(): Boolean {
        val yes = TrainingLessonCatalog.communicationLessonAt(1) ?: return false
        val afterTwoRights = LessonInteractionEngine.lessonMatchesGesture(
            yes,
            left = 0,
            right = 2,
            blinkOrder = listOf(false, false)
        )
        val please = TrainingLessonCatalog.communicationLessonAt(3) ?: return false
        val pleaseComplete = LessonInteractionEngine.lessonMatchesGesture(
            please,
            left = 1,
            right = 2,
            blinkOrder = listOf(true, false, false)
        )
        return afterTwoRights && pleaseComplete
    }

    fun phraseSpeechAfterDefaultResponseTimeFinalization(): Boolean {
        val main = readMainActivity() ?: return false
        val controller = readControllerSource() ?: return false
        return main.contains("shouldFinalizeSequence") &&
            SEQUENCE_IDLE_TIMEOUT_MS == SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS) &&
            controller.contains("beginInteractiveLessonSuccess") &&
            controller.contains("SUCCESS_VISUAL_PAUSE_MS")
    }

    fun noNonPhraseNarrationInLessonPath(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY && !LisaSpeechPolicy.allowsNarration()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedPartialTimeoutAndWrongEyeFeedbackV1")
    }

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readControllerSource(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readLessonInteractionEngine(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/lessoninteraction/LessonInteractionEngine.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )
}
