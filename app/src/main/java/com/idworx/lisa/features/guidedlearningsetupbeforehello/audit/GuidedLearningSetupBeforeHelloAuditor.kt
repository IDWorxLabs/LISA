package com.idworx.lisa.features.guidedlearningsetupbeforehello.audit

import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedLearningSetupBeforeHelloAuditor {

    private val navigator = GuidedTrainingNavigator()

    fun beginLearningRoutesToSetupNotHello(): Boolean {
        val progress = navigator.reduce(TrainingProgress(), TrainingEvent.BeginLearning)
        return progress.currentPhase == TrainingPhase.Setup &&
            progress.currentPhase != TrainingPhase.CommunicationLesson
    }

    fun completeSetupRoutesToHello(): Boolean {
        val progress = navigator.reduce(
            TrainingProgress(currentPhase = TrainingPhase.Setup),
            TrainingEvent.CompleteSetup
        )
        return progress.currentPhase == TrainingPhase.CommunicationLesson &&
            progress.communicationLessonIndex == 0
    }

    fun faceDetectDoesNotAutoCompleteSetup(): Boolean {
        val controller = readController() ?: return false
        val blockStart = controller.indexOf("fun onFaceDetectedDuringSetup()")
        if (blockStart < 0) return false
        val blockEnd = controller.indexOf("fun onFaceLostDuringSetup()", blockStart)
        if (blockEnd < 0) return false
        val block = controller.substring(blockStart, blockEnd)
        return !block.contains("dispatch(TrainingEvent.CompleteSetup)") &&
            block.contains("if (!LisaSpeechPolicy.allowsNarration()) return")
    }

    fun setupScreenHasEyeStatus(): Boolean {
        val setup = readSetupScreen() ?: return false
        return setup.contains("SetupDetectionStatusRow") &&
            setup.contains("Let's get ready") &&
            setup.contains("EyeTrackingStatusPill") &&
            setup.contains("Watching your eyes")
    }

    fun readyStepRequiresContinue(): Boolean {
        val setup = readSetupScreen() ?: return false
        val flow = readGuidedTrainingFlow() ?: return false
        return setup.contains("Continue to first lesson") &&
            setup.contains("enabled = eyesReady") &&
            flow.contains("TrainingEvent.CompleteSetup") &&
            flow.contains("SETUP_STEP_READY")
    }

    fun lessonHasEyeIndicator(): Boolean {
        val lessons = readLessonScreens() ?: return false
        return lessons.contains("EyeTrackingStatusPill") &&
            lessons.contains("eyeTracking")
    }

    fun cameraComposedDuringTraining(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        return ui.contains("cameraView()") &&
            ui.contains("trainingBlocksMainUi") &&
            ui.contains("alpha(0f)")
    }

    fun setupStepConstantsDefined(): Boolean {
        val controller = readController() ?: return false
        return controller.contains("SETUP_STEP_EYE_DETECTION") &&
            controller.contains("SETUP_STEP_READY") &&
            controller.contains("advanceSetupToReadyCheck")
    }

    fun phraseTranslationOnly(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            !LisaSpeechPolicy.allowsNarration()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedLearningSetupBeforeHelloAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedLearningSetupBeforeHelloV1")
    }

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readSetupScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingSetupScreen.kt"
    )

    private fun readGuidedTrainingFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )
}
