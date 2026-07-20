package com.idworx.lisa.features.silentwelcome.audit

import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.silentwelcome.metadata.SilentWelcomeLaunchFlowMetadata
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object SilentWelcomeLaunchFlowAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/silent-welcome-launch-flow/README.md")

    fun speechPolicyPhraseTranslationOnly(): Boolean =
        LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            !LisaSpeechPolicy.allowsNarration() &&
            LisaSpeechPolicy.allowsPhraseTranslation()

    fun welcomeGateEnforced(): Boolean {
        val store = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt"
        ) ?: return false
        return store.contains("WelcomeStatePriorityGate.applyForColdLaunch") &&
            store.contains("TrainingPhase.FirstLaunchChoice")
    }

    fun launchScreenExact(): Boolean {
        val welcome = readWelcomeScreen() ?: return false
        val hasDestinationCard = welcome.contains("TrainingCard") ||
            welcome.contains("WelcomeDestinationLayoutStyle")
        return welcome.contains("TrainingFirstLaunchChoiceScreen") &&
            welcome.contains("welcomeToLisa") &&
            welcome.contains("SilentWelcomeLaunchFlowMetadata.SUBTITLE") &&
            welcome.contains("startGuidedLearning") &&
            welcome.contains("skipToCommunication") &&
            hasDestinationCard &&
            !welcome.contains("LargeGestureChoiceCard")
    }

    fun onboardingFlowDoesNotBlockTraining(): Boolean {
        val ui = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
        ) ?: return false
        return ui.contains("!onboardingCompleted && !guidedTrainingActive")
    }

    fun startRoutesToTeachingWithoutNarration(): Boolean {
        val navigator = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedTrainingNavigator.kt"
        ) ?: return false
        val controller = readController() ?: return false
        val navStart = navigator.indexOf("TrainingEvent.BeginLearning ->")
        val navEnd = navigator.indexOf("TrainingEvent.SkipTutorial ->", navStart)
        val navBlock = if (navStart >= 0 && navEnd > navStart) navigator.substring(navStart, navEnd) else return false
        val ctlStart = controller.indexOf("TrainingEvent.BeginLearning ->")
        val ctlEnd = controller.indexOf("TrainingEvent.ConfirmSkip ->", ctlStart)
        val ctlBlock = if (ctlStart >= 0 && ctlEnd > ctlStart) controller.substring(ctlStart, ctlEnd) else return false
        return navBlock.contains("TrainingPhase.Setup") &&
            !ctlBlock.contains("setupNarration()")
    }

    fun skipRoutesToWorkspace(): Boolean {
        val flow = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        ) ?: return false
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return flow.contains("TrainingEvent.ConfirmSkip") &&
            controller.contains("onTrainingFinished()")
    }

    fun narrationGatedInController(): Boolean {
        val narration = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/audio/OnboardingNarrationController.kt"
        ) ?: return false
        val controller = readController() ?: return false
        return narration.contains("LisaSpeechPolicy.allowsNarration()") &&
            controller.contains("LisaSpeechPolicy.allowsNarration()") &&
            controller.contains("speakPhrase(phrase)")
    }

    fun phraseTranslationPreserved(): Boolean {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: return false
        return main.contains("speakTranslatedPhrase") &&
            main.contains("speakPhrase = { text -> speakTranslatedPhrase(text) }") &&
            main.contains("LISA_SPEAK")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/SilentWelcomeLaunchFlowAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaSilentWelcomeLaunchFlowV1")
    }

    private fun readWelcomeScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
    )

    private fun readController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )
}
