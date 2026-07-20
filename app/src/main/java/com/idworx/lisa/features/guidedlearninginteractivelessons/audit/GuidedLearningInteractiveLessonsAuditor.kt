package com.idworx.lisa.features.guidedlearninginteractivelessons.audit

import com.idworx.lisa.features.brain1interactionstandard.model.BlinkSequenceOrder
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedLearningInteractiveLessonsAuditor {

    private val navigator = GuidedTrainingNavigator()

    fun lessonScreenShowsEyeStatus(): Boolean {
        val lessons = readLessonScreens() ?: return false
        return (lessons.contains("EyeTrackingStatusPill") ||
            lessons.contains("CompactEyeTrackingHeader")) &&
            lessons.contains("LessonEyeStatusPanel") &&
            (lessons.contains("Watching your eyes") || lessons.contains("watchingLabel"))
    }

    fun lessonScreenShowsLiveBlinkFeedback(): Boolean {
        val lessons = readLessonScreens() ?: return false
        val components = readTrainingComponents() ?: return false
        return lessons.contains("Detected Progress") &&
            lessons.contains("detectedProgress") &&
            components.contains("Left blinks") &&
            components.contains("Right blinks")
    }

    fun partialSequenceUpdatesProgress(): Boolean {
        val yes = TrainingLessonCatalog.communicationLessonAt(1) ?: return false
        val afterOne = LessonInteractionEngine.progressLabel(0, 1, listOf(false), yes)
        return afterOne == "Right blink 1 of 2"
    }

    fun completedSequenceUsesInteractiveSuccessFlow(): Boolean {
        val controller = readController() ?: return false
        return controller.contains("beginInteractiveLessonSuccess") &&
            controller.contains("onPhraseSpeechFinished") &&
            controller.contains("completePendingInteractiveLessonSuccess") &&
            controller.contains("hasPendingInteractiveLessonSuccess") &&
            controller.contains("SUCCESS_VISUAL_PAUSE_MS")
    }

    fun successShowsVisualWellDone(): Boolean {
        val engine = readLessonInteractionEngine() ?: return false
        return engine.contains("Well done") && engine.contains("You did it")
    }

    fun onlyPhraseSpeechOnSuccess(): Boolean {
        val controller = readController() ?: return false
        val main = readMainActivity() ?: return false
        return controller.contains("LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY") &&
            controller.contains("beginInteractiveLessonSuccess") &&
            controller.contains("speakPhrase(phrase)") &&
            main.contains("onPhraseSpeechFinished")
    }

    fun noCoachingNarrationInInteractivePath(): Boolean {
        val controller = readController() ?: return false
        val interactiveStart = controller.indexOf("private fun handleInteractiveSequence")
        if (interactiveStart < 0) return false
        val interactiveBlock = controller.substring(interactiveStart, interactiveStart + 600)
        return !interactiveBlock.contains("narration.speak") &&
            !interactiveBlock.contains("gentleMissedBlinkNarration")
    }

    fun wrongBlinkResetsWithVisualOnly(): Boolean {
        val controller = readController() ?: return false
        val engine = readLessonInteractionEngine() ?: return false
        return controller.contains("applyInteractiveRetryVisual") &&
            engine.contains("Try again") &&
            engine.contains("That blink was not part of this one") &&
            controller.contains("onLessonWink")
    }

    fun lessonAdvancesAfterSpeech(): Boolean {
        val main = readMainActivity() ?: return false
        val controller = readController() ?: return false
        return main.contains("hasPendingInteractiveLessonSuccess") &&
            main.contains("onPhraseSpeechFinished") &&
            controller.contains("completePendingInteractiveLessonSuccess")
    }

    fun progressesFromPhrasesToNavigation(): Boolean {
        val essentials = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_VOCABULARY_IDS
        val catalogIds = TrainingLessonCatalog.communicationFundamentals
            .take(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT)
            .map { it.vocabularyId }
        if (catalogIds != essentials) return false
        val progress = TrainingProgress(
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT - 1,
            currentLessonSuccessCount = 0
        )
        val afterSuccess = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
        return afterSuccess.currentPhase == TrainingPhase.NavigationLesson &&
            TrainingLessonCatalog.navigationLessons.isNotEmpty()
    }

    fun uiRemainsMinimal(): Boolean {
        val flow = readGuidedTrainingFlow() ?: return false
        val lessons = readLessonScreens() ?: return false
        val lessonBlockStart = lessons.indexOf("fun CommunicationLessonScreen(")
        if (lessonBlockStart < 0) return false
        val lessonBlockEnd = lessons.indexOf("fun NavigationLessonScreen(", lessonBlockStart)
        if (lessonBlockEnd < 0) return false
        val lessonBlock = lessons.substring(lessonBlockStart, lessonBlockEnd)
        return !flow.contains("CaregiverCoachProgressStrip") &&
            !flow.contains("TrainingProgressIndicator") &&
            !flow.contains("NarrationControls") &&
            !lessonBlock.contains("TrainingProgressIndicator") &&
            !lessonBlock.contains("NarrationControls") &&
            !lessonBlock.contains("CaregiverCoachProgressStrip")
    }

    fun lessonInteractionEngineValidatesOrderedGestures(): Boolean {
        val no = CommunicationLesson("comm_no", "no", 1, 1, 3, difficultyLevel = 1, blinkOrder = "LR")
        val validLeft = LessonInteractionEngine.isValidPartial(1, 0, listOf(true), no)
        val invalidRightFirst = LessonInteractionEngine.isValidPartial(0, 1, listOf(false), no)
        val complete = LessonInteractionEngine.lessonMatchesGesture(no, 1, 1, listOf(true, false))
        return validLeft && !invalidRightFirst && complete &&
            BlinkSequenceOrder.matches(listOf(true, false), "LR")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedLearningInteractiveLessonsAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedLearningInteractiveLessonsV1")
    }

    fun phraseTranslationOnly(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY && !LisaSpeechPolicy.allowsNarration()

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readTrainingComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readGuidedTrainingFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
    )

    private fun readLessonInteractionEngine(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/lessoninteraction/LessonInteractionEngine.kt"
    )
}
