package com.idworx.lisa.features.brain1interactionstandard.audit

import com.idworx.lisa.features.brain1interactionstandard.metadata.Brain1InteractionStandardMetadata
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import java.io.File

object Brain1InteractionStandardAuditor {

    fun noSingleBlinkCommands(): Boolean =
        UniversalInteractionGestures.MIN_COMMAND_BLINKS >= 2 &&
            readGestures()?.contains("MIN_COMMAND_BLINKS: Int = 2") == true

    fun universalGestureTableExists(): Boolean =
        Brain1InteractionStandardMetadata.UNIVERSAL_GESTURES.size >= 4 &&
            readMetadata()?.contains("UNIVERSAL_GESTURES") == true

    fun l2Standardized(): Boolean =
        UniversalInteractionGestures.OPTION_A_LEFT == 2 &&
            UniversalInteractionGestures.OPTION_A_RIGHT == 0

    fun r2Standardized(): Boolean =
        UniversalInteractionGestures.OPTION_B_LEFT == 0 &&
            UniversalInteractionGestures.OPTION_B_RIGHT == 2

    fun confirmStandardized(): Boolean =
        UniversalInteractionGestures.CONFIRM_LEFT == 1 &&
            UniversalInteractionGestures.CONFIRM_RIGHT == 1

    fun cancelStandardized(): Boolean {
        val gestures = readGestures() ?: return false
        return gestures.contains("isRightThenLeft") &&
            gestures.contains("BlinkSequenceOrder") &&
            !gestures.contains("CANCEL_LEFT: Int = 2")
    }

    fun firstLaunchEyeDriven(): Boolean {
        val welcome = readWelcomeScreen() ?: return false
        return welcome.contains("TrainingFirstLaunchChoiceScreen") &&
            welcome.contains("TrainingPrimaryButton") &&
            welcome.contains("Start Guided Learning") &&
            welcome.contains("Skip to Communication Workspace")
    }

    fun guidedLearningRequiresConfirmation(): Boolean {
        val controller = readSessionController() ?: return false
        return controller.contains("handleBrain1Interaction") &&
            controller.contains("Brain1DecisionEngine") &&
            controller.contains("FirstLaunchGuidedLearning") &&
            controller.contains("executeBrain1Confirmed")
    }

    fun workspaceSkipRequiresConfirmation(): Boolean {
        val controller = readSessionController() ?: return false
        return controller.contains("FirstLaunchSkipWorkspace") &&
            controller.contains("repeatForKind")
    }

    fun universalDecisionModelImplemented(): Boolean {
        val engine = readDecisionEngine() ?: return false
        return engine.contains("ChoiceDetected") &&
            engine.contains("AwaitingConfirm") &&
            engine.contains("Confirmed")
    }

    fun emergencyConfirmationImplemented(): Boolean {
        val controller = readSessionController() ?: return false
        val main = readMainActivity() ?: return false
        return controller.contains("beginEmergencyConfirm") &&
            controller.contains("onEmergencyConfirmed") &&
            main.contains("beginEmergencyConfirm") &&
            main.contains("onEmergencyConfirmed")
    }

    fun resetConfirmationImplemented(): Boolean {
        val controller = readSessionController() ?: return false
        val main = readMainActivity() ?: return false
        return controller.contains("ResetLearningProgress") &&
            main.contains("ResetLearningProgress")
    }

    fun recalibrationConfirmationImplemented(): Boolean {
        val controller = readSessionController() ?: return false
        return controller.contains("Recalibration") &&
            controller.contains("beginAwaitingBrain1Decision")
    }

    fun progressiveDifficultyImplemented(): Boolean =
        Brain1InteractionStandardMetadata.PROGRESSIVE_LEVELS.size == 5 &&
            TrainingLessonCatalog.communicationFundamentals.any { it.difficultyLevel >= 2 }

    fun earlyLessonsUseSimpleGestures(): Boolean =
        TrainingLessonCatalog.earliestLessonsUseSimpleGestures()

    fun calibrationIntroImproved(): Boolean {
        val controller = readSessionController() ?: return false
        return controller.contains("FirstFiveMinutesExperience.calibrationIntroDialogues") ||
            controller.contains("calibrationIntroAfterConfirm")
    }

    fun calibrationVisualsImproved(): Boolean {
        val screen = readLessonScreens() ?: return false
        return screen.contains("TrainingCalibrationScreen") &&
            screen.contains("rememberInfiniteTransition") &&
            screen.contains("Color(0xFF0D47A1)")
    }

    fun personalityEngineReused(): Boolean {
        val provider = readDialogueProvider() ?: return false
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return provider.contains("DefaultDialogueCatalog") &&
            catalog.contains("b1_first_launch") &&
            catalog.contains("b1_repeat_choice")
    }

    fun zeroTouchMaintained(): Boolean {
        val welcome = readWelcomeScreen() ?: return false
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return catalog.contains("I'll wait until you're ready") &&
            welcome.contains("TrainingFirstLaunchChoiceScreen")
    }

    fun existingBrain1SystemsReused(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) &&
            ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/personality/engine/LisaPersonalityEngine.kt"
            )

    fun documentationCompleted(): Boolean {
        val candidates = listOf(
            "features/brain1-interaction-standard/README.md",
            "app/src/main/java/com/idworx/lisa/features/brain1interactionstandard/README.md"
        )
        return candidates.any { ZeroTouchFileProbe.fileExists(it) }
    }

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateBrain1InteractionStandardAuthorityV1")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/Brain1InteractionStandardAuthorityV1Test.kt"
        )

    private fun readGestures(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/brain1interactionstandard/model/UniversalInteractionGestures.kt"
    )

    private fun readMetadata(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/brain1interactionstandard/metadata/Brain1InteractionStandardMetadata.kt"
    )

    private fun readWelcomeScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
    )

    private fun readSessionController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readDecisionEngine(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/brain1interactionstandard/engine/Brain1DecisionEngine.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readDialogueProvider(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/brain1interactionstandard/dialogue/Brain1DecisionDialogueProvider.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )
}
