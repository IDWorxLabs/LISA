package com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.audit

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.GuidedLearningSimplificationExperience
import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.metadata.GuidedLearningSimplificationMetadata
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedLearningSimplificationAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-guided-learning-simplification/README.md")

    fun firstLaunchEnforced(): Boolean {
        val store = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt"
        ) ?: return false
        return store.contains("FirstLaunchChoice") &&
            store.contains("enforceFirstLaunchChoice") &&
            store.contains("KEY_FIRST_LAUNCH_CHOICE")
    }

    fun welcomeScreenSimplified(): Boolean {
        val welcome = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
        ) ?: return false
        return welcome.contains("Welcome to Lisa") &&
            welcome.contains("Lisa will guide the primary user") &&
            welcome.contains("TrainingCard") &&
            welcome.contains("Start Guided Learning") &&
            welcome.contains("Skip to Communication Workspace") &&
            !welcome.contains("LargeGestureChoiceCard")
    }

    fun lessonScreenMinimal(): Boolean {
        val screens = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        ) ?: return false
        val ui = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedLearningUi.kt"
        ) ?: return false
        return screens.contains("SimplifiedGestureDisplay") &&
            ui.contains("formatWinkInstruction") &&
            !screens.contains("NarrationControls") &&
            !screens.contains("CaregiverSupportStrip") &&
            !screens.contains("TrainingProgressIndicator(")
    }

    fun noLessonDashboardInFlow(): Boolean {
        val flow = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        ) ?: return false
        return flow.contains("formatWinkInstruction") &&
            !flow.contains("onPauseNarration") &&
            !flow.contains("onSkipNarration") &&
            !flow.contains("CaregiverCoachProgressStrip") &&
            !flow.contains("TrainingProgressIndicator") &&
            !flow.contains("NarrationControls")
    }

    fun workspaceTransitionDialogues(): Boolean =
        GuidedLearningSimplificationExperience.workspaceTransitionDialogues().size >= 3

    fun completionScreenSimplified(): Boolean {
        val welcome = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
        ) ?: return false
        return welcome.contains("You now know the basics") &&
            welcome.contains("Communication Workspace")
    }

    fun friendlyGestureLabels(): Boolean {
        val ui = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedLearningUi.kt"
        ) ?: return false
        return ui.contains("formatWinkGestureFriendly") &&
            ui.contains("Blink Left")
    }

    fun completionNarrationWired(): Boolean {
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return controller.contains("GuidedLearningSimplificationExperience") &&
            controller.contains("workspaceTransitionDialogues")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedLearningSimplificationAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedLearningSimplificationV1")
    }

    fun metadataDefinesPhilosophy(): Boolean =
        GuidedLearningSimplificationMetadata.SIMPLIFICATION_PHILOSOPHY.contains("three questions")
}
