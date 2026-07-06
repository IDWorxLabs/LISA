package com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.audit

import com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.metadata.LaunchScreenExactSimpleMetadata
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object LaunchScreenExactSimpleAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-launch-screen-exact-simple/README.md")

    fun firstLaunchEnforced(): Boolean {
        val store = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt"
        ) ?: return false
        return store.contains("WelcomeStatePriorityGate") &&
            store.contains("applyForColdLaunch") &&
            store.contains("KEY_FIRST_LAUNCH_CHOICE")
    }

    fun launchScreenLayoutExact(): Boolean {
        val welcome = readWelcomeScreen() ?: return false
        return welcome.contains("TrainingFirstLaunchChoiceScreen") &&
            welcome.contains("Welcome to Lisa") &&
            welcome.contains(LaunchScreenExactSimpleMetadata.SUBTITLE) &&
            welcome.contains("TrainingCard") &&
            welcome.contains("TrainingPrimaryButton") &&
            welcome.contains("Start Guided Learning") &&
            welcome.contains("TrainingSecondaryButton") &&
            welcome.contains("Skip to Communication Workspace") &&
            welcome.contains("TrainingLisaLogo") &&
            !welcome.contains("LargeGestureChoiceCard") &&
            !welcome.contains("GestureChoiceCard")
    }

    fun noGestureLabelsOnLaunch(): Boolean {
        val block = firstLaunchBlock() ?: return false
        return !block.contains("\"L2\"") &&
            !block.contains("\"R2\"") &&
            !block.contains("L1 R1 to continue")
    }

    fun noLaunchClutter(): Boolean {
        val block = firstLaunchBlock() ?: return false
        return !block.contains("TrainingProgressIndicator") &&
            !block.contains("CaregiverCoachProgressStrip") &&
            !block.contains("CaregiverSupportStrip") &&
            !block.contains("Lisa is speaking")
    }

    fun startRoutesToTeaching(): Boolean {
        val flow = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        ) ?: return false
        val navigator = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedTrainingNavigator.kt"
        ) ?: return false
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        val beginBlock = beginLearningNavigatorBlock(navigator) ?: return false
        val ctlStart = controller.indexOf("TrainingEvent.BeginLearning ->")
        val ctlEnd = controller.indexOf("TrainingEvent.ConfirmSkip ->", ctlStart)
        val ctlBlock = if (ctlStart >= 0 && ctlEnd > ctlStart) controller.substring(ctlStart, ctlEnd) else return false
        return flow.contains("onStartGuidedLearning") &&
            flow.contains("TrainingEvent.BeginLearning") &&
            beginBlock.contains("currentPhase = TrainingPhase.Setup") &&
            !beginBlock.contains("currentPhase = TrainingPhase.Welcome") &&
            !ctlBlock.contains("setupNarration()")
    }

    fun skipRoutesToWorkspace(): Boolean {
        val flow = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        ) ?: return false
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return flow.contains("onSkipToWorkspace") &&
            flow.contains("TrainingEvent.ConfirmSkip") &&
            controller.contains("TrainingEvent.ConfirmSkip") &&
            controller.contains("onTrainingFinished")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/LaunchScreenExactSimpleAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaLaunchScreenExactSimpleV1")
    }

    private fun readWelcomeScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
    )

    private fun firstLaunchBlock(): String? {
        val welcome = readWelcomeScreen() ?: return null
        val start = welcome.indexOf("fun TrainingFirstLaunchChoiceScreen")
        if (start < 0) return null
        val end = welcome.indexOf("fun TrainingSkipConfirmScreen", start)
        if (end < 0) return null
        return welcome.substring(start, end)
    }

    private fun beginLearningNavigatorBlock(navigator: String): String? {
        val start = navigator.indexOf("TrainingEvent.BeginLearning ->")
        if (start < 0) return null
        val end = navigator.indexOf("TrainingEvent.SkipTutorial ->", start)
        if (end < 0) return null
        return navigator.substring(start, end)
    }
}
