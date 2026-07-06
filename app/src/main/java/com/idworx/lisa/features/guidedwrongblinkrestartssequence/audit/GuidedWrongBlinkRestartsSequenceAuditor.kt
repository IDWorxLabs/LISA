package com.idworx.lisa.features.guidedwrongblinkrestartssequence.audit

import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedWrongBlinkRestartsSequenceAuditor {

    fun wrongBlinkResetsProgressToZero(): Boolean {
        val controller = readControllerSource() ?: return false
        val resetStart = controller.indexOf("fun applyWrongEyeFeedback()")
        if (resetStart < 0) return false
        val block = controller.substring(resetStart, resetStart + 600)
        return block.contains("detectedProgress = null") &&
            block.contains("liveLeftBlinks = 0") &&
            block.contains("liveRightBlinks = 0")
    }

    fun wrongBlinkClearsLessonBuffer(): Boolean {
        val main = readMainActivity() ?: return false
        val rejectStart = main.indexOf("private fun rejectLessonWrongEyeBlink")
        if (rejectStart < 0) return false
        val block = main.substring(rejectStart, rejectStart + 500)
        return block.contains("resetSequence()") &&
            block.contains("cancelLessonPartialSequenceTimeout()")
    }

    fun wrongBlinkKeepsSameLesson(): Boolean {
        val controller = readControllerSource() ?: return false
        val resetStart = controller.indexOf("fun applyWrongEyeFeedback()")
        if (resetStart < 0) return false
        val block = controller.substring(resetStart, resetStart + 600)
        return !block.contains("SequenceSuccess") &&
            !block.contains("navigator.reduce") &&
            !block.contains("communicationLessonIndex +")
    }

    fun wrongBlinkShowsRedFeedback(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val engine = readLessonInteractionEngine() ?: return false
        return lessons.contains("wrongEyeMessage") &&
            lessons.contains("LisaEmergencyRed") &&
            engine.contains("Wrong eye — start again")
    }

    fun userMustRestartFullSequence(): Boolean {
        val food = TrainingLessonCatalog.communicationFundamentals
            .firstOrNull { it.vocabularyId == "i_need_food" } ?: return false
        val afterTwoLefts = LessonInteractionEngine.isWrongEyeBlink(
            isLeft = true,
            left = 2,
            right = 0,
            blinkOrder = listOf(true, true),
            lesson = food
        )
        val restartMsg = LessonInteractionEngine.wrongEyeRestartFeedbackMessage(food, 0)
        return afterTwoLefts && restartMsg.contains("blink left to start again")
    }

    fun fullSequenceAfterRestartCompletesNormally(): Boolean {
        val food = TrainingLessonCatalog.communicationFundamentals
            .firstOrNull { it.vocabularyId == "i_need_food" } ?: return false
        return LessonInteractionEngine.lessonMatchesGesture(
            food,
            left = 2,
            right = 1,
            blinkOrder = listOf(true, true, false)
        )
    }

    fun noPhraseSpeechOnWrongBlink(): Boolean {
        val controller = readControllerSource() ?: return false
        val resetStart = controller.indexOf("fun applyWrongEyeFeedback()")
        if (resetStart < 0) return false
        val block = controller.substring(resetStart, resetStart + 600)
        return !block.contains("speakPhrase") &&
            !block.contains("narration.speak") &&
            LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY
    }

    fun workspacePhraseResolverUnchanged(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("WorkspacePhraseResolver") &&
            main.contains("rejectLessonWrongEyeBlink") &&
            main.contains("isCommunicationLessonPhase()")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedWrongBlinkRestartsSequenceAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedWrongBlinkRestartsSequenceV1")
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
