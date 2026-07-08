package com.idworx.lisa.features.guidedsuccesstimingfix.audit

import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedSuccessTimingFixAuditor {

    fun phraseSpeechHappensBeforeSuccessMessage(): Boolean {
        val controller = readController() ?: return false
        val block = beginSuccessBlock(controller) ?: return false
        val speakIndex = block.indexOf("speakPhrase(phrase)")
        if (speakIndex < 0) return false
        val successAssignIndex = block.indexOf("successVisualMessage = successMsg")
        val successNullIndex = block.indexOf("successVisualMessage = null")
        return successNullIndex in 0 until speakIndex && successAssignIndex < 0
    }

    fun wellDoneAppearsOnlyAfterSpeechCompletes(): Boolean {
        val controller = readController() ?: return false
        val main = readMainActivity() ?: return false
        val finishedBlock = onPhraseSpeechFinishedBlock(controller) ?: return false
        return finishedBlock.contains("successVisualMessage = successMsg") &&
            main.contains("if (trainingSession.hasPendingInteractiveLessonSuccess())") &&
            main.contains("trainingSession.onPhraseSpeechFinished")
    }

    fun noNonPhraseNarrationAdded(): Boolean {
        val controller = readController() ?: return false
        val interactiveStart = controller.indexOf("private fun handleInteractiveSequence")
        if (interactiveStart < 0) return false
        val interactiveBlock = controller.substring(interactiveStart, interactiveStart + 700)
        val successBlock = beginSuccessBlock(controller) ?: return false
        return !interactiveBlock.contains("narration.speak") &&
            !successBlock.contains("narration.speak") &&
            LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY
    }

    fun lessonAdvancesAfterSuccessMessagePause(): Boolean {
        val controller = readController() ?: return false
        val finishedBlock = onPhraseSpeechFinishedBlock(controller) ?: return false
        return finishedBlock.contains("mainThreadDelayed(SUCCESS_VISUAL_PAUSE_MS)") &&
            finishedBlock.contains("completePendingInteractiveLessonSuccess()") &&
            finishedBlock.indexOf("successVisualMessage = successMsg") <
                finishedBlock.indexOf("completePendingInteractiveLessonSuccess()")
    }

    fun sequenceFinalizationTimeoutUnchanged(): Boolean =
        SEQUENCE_IDLE_TIMEOUT_MS == SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)

    fun lessonPhraseStaysVisibleDuringSpeech(): Boolean {
        val lessons = readLessonScreens() ?: return false
        return lessons.contains("lessonInteraction.successVisualMessage == null &&") &&
            lessons.contains("GuidedLessonPhraseTitle")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedSuccessTimingFixAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedSuccessTimingFixV1")
    }

    private fun beginSuccessBlock(controller: String): String? {
        val start = controller.indexOf("private fun beginInteractiveLessonSuccess")
        if (start < 0) return null
        val end = controller.indexOf("fun onPhraseSpeechFinished", start)
        if (end < 0) return null
        return controller.substring(start, end)
    }

    private fun onPhraseSpeechFinishedBlock(controller: String): String? {
        val start = controller.indexOf("fun onPhraseSpeechFinished")
        if (start < 0) return null
        val end = controller.indexOf("fun hasActiveBrain1Decision", start)
        if (end < 0) return null
        return controller.substring(start, end)
    }

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )
}
