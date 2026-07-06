package com.idworx.lisa.features.launchwelcomestatepriority.audit

import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import com.idworx.lisa.features.launchwelcomestatepriority.metadata.LaunchWelcomeStatePriorityMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object LaunchWelcomeStatePriorityAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/launch-welcome-state-priority/README.md")

    fun gateUsesFinishedNotFirstLaunchChoice(): Boolean {
        val gate = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/launchwelcomestatepriority/WelcomeStatePriorityGate.kt"
        ) ?: return false
        return gate.contains("applyForColdLaunch") &&
            gate.contains("mustShowWelcome") &&
            gate.contains("TrainingPhase.FirstLaunchChoice") &&
            gate.contains("firstLaunchChoiceMade = false")
    }

    fun storeAppliesGateOnLoad(): Boolean {
        val store = readStore() ?: return false
        return store.contains("WelcomeStatePriorityGate.applyForColdLaunch") &&
            !store.contains("save(gated)")
    }

    fun runtime_savedHelloWithoutFinishedTraining_returnsWelcome(): Boolean {
        val saved = TrainingProgress(
            tutorialStarted = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0
        )
        val gated = WelcomeStatePriorityGate.apply(saved)
        return gated.currentPhase == TrainingPhase.FirstLaunchChoice &&
            !gated.firstLaunchChoiceMade
    }

    fun runtime_guidedTrainingActiveWithoutFinished_returnsWelcome(): Boolean {
        val saved = TrainingProgress(
            tutorialStarted = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.CommunicationLesson,
            practiceModeOnly = false
        )
        val gated = WelcomeStatePriorityGate.apply(saved)
        return gated.currentPhase == TrainingPhase.FirstLaunchChoice &&
            !saved.isFinished
    }

    fun runtime_oldOnboardingProgressMigratesToWelcome(): Boolean {
        val legacy = TrainingProgress(
            tutorialStarted = true,
            firstLaunchChoiceMade = false,
            currentPhase = TrainingPhase.Setup,
            calibrationCompleted = false
        )
        return WelcomeStatePriorityGate.apply(legacy).currentPhase == TrainingPhase.FirstLaunchChoice
    }

    fun runtime_startRequiredBeforeHello(): Boolean {
        val navigator = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedTrainingNavigator.kt"
        ) ?: return false
        val flow = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        ) ?: return false
        val beginStart = navigator.indexOf("TrainingEvent.BeginLearning ->")
        val beginEnd = navigator.indexOf("TrainingEvent.SkipTutorial ->", beginStart)
        val beginBlock = if (beginStart >= 0 && beginEnd > beginStart) {
            navigator.substring(beginStart, beginEnd)
        } else {
            return false
        }
        return flow.contains("TrainingEvent.BeginLearning") &&
            beginBlock.contains("firstLaunchChoiceMade = true") &&
            runtime_savedHelloWithoutFinishedTraining_returnsWelcome()
    }

    fun runtime_skipRequiredBeforeWorkspace(): Boolean {
        val flow = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        ) ?: return false
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return flow.contains("TrainingEvent.ConfirmSkip") &&
            controller.contains("onTrainingFinished()")
    }

    fun phraseOnlyVoicePolicyActive(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            !LisaSpeechPolicy.allowsNarration() &&
            LisaSpeechPolicy.allowsPhraseTranslation()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/LaunchWelcomeStatePriorityAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaLaunchWelcomeStatePriorityV1")
    }

    private fun readStore(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt"
    )
}
