package com.idworx.lisa.features.experiencepolish.firstfiveminutes.audit

import com.idworx.lisa.features.brain1interactionstandard.model.BlinkSequenceOrder
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.experiencepolish.firstfiveminutes.FirstFiveMinutesExperience
import com.idworx.lisa.features.experiencepolish.firstfiveminutes.metadata.FirstFiveMinutesMetadata
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object FirstFiveMinutesAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-phase-a/README.md")

    fun firstLaunchEyeDriven(): Boolean {
        val screen = readWelcomeScreen() ?: return false
        return screen.contains("startGuidedLearning") &&
            screen.contains("TrainingPrimaryButton") &&
            !screen.contains("L2 R2 to choose again")
    }

    fun choiceConfirmationRequired(): Boolean {
        val controller = readSessionController() ?: return false
        return controller.contains("AwaitingConfirm") && controller.contains("executeBrain1Confirmed")
    }

    fun cancelIsR1L1Only(): Boolean {
        val gestures = readGestures() ?: return false
        return gestures.contains("isRightThenLeft") && !gestures.contains("CANCEL_LEFT: Int = 2")
    }

    fun noL2R2CancelInBrain1(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return !catalog.contains("L2 R2 to choose again") &&
            catalog.contains("R1 L1 to choose again")
    }

    fun universalGestureTableMatches(): Boolean =
        UniversalInteractionGestures.OPTION_A_LEFT == 2 &&
            UniversalInteractionGestures.OPTION_B_RIGHT == 2 &&
            BlinkSequenceOrder.isLeftThenRight(listOf(true, false))

    fun meetLisaNarrationRich(): Boolean =
        FirstFiveMinutesExperience.meetLisaDialogues().size >= 6

    fun gettingReadyNarrationExists(): Boolean =
        FirstFiveMinutesExperience.gettingReadyDialogues().isNotEmpty()

    fun calibrationIntroExists(): Boolean =
        FirstFiveMinutesExperience.calibrationIntroDialogues().size >= 4

    fun calibrationDotVisible(): Boolean {
        val screen = readLessonScreens() ?: return false
        return screen.contains("Color(0xFF0D47A1)") && screen.contains("rememberInfiniteTransition")
    }

    fun calibrationResultDialogueExists(): Boolean =
        FirstFiveMinutesExperience.calibrationExcellentDialogues().isNotEmpty() &&
            FirstFiveMinutesExperience.calibrationPoorDialogues().isNotEmpty()

    fun helloNotL1R6(): Boolean {
        val hello = TrainingLessonCatalog.communicationFundamentals.firstOrNull() ?: return false
        return hello.left == 2 && hello.right == 0
    }

    fun firstPhraseTwoBlink(): Boolean {
        val hello = TrainingLessonCatalog.communicationFundamentals.firstOrNull() ?: return false
        return UniversalInteractionGestures.difficultyLevel(hello.left, hello.right) == 1
    }

    fun earlyPhrasesProgressive(): Boolean =
        TrainingLessonCatalog.earliestLessonsUseSimpleGestures()

    fun gentleFailureDialogue(): Boolean =
        FirstFiveMinutesExperience.gentleMissedBlinkDialogues().any {
            it.contains("missed", ignoreCase = true)
        }

    fun noForbiddenPunitiveWording(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return FirstFiveMinutesMetadata.FORBIDDEN_PUNITIVE_WORDS.none { word ->
            catalog.contains("\"$word", ignoreCase = true)
        }
    }

    fun personalityEngineProvidesDialogue(): Boolean =
        FirstFiveMinutesExperience.meetLisaDialogues().isNotEmpty()

    fun zeroTouchPreserved(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return catalog.contains("I'll wait until you're ready")
    }

    fun deviceTestingChecklistUpdated(): Boolean {
        val suite = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/suites/DeviceTestSuites.kt"
        ) ?: return false
        return suite.contains("PHASE_A") || suite.contains("phase_a_first_five") ||
            suite.contains("PhaseAFirstFiveMinutes")
    }

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaExperiencePhaseAFirstFiveMinutesV1")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/FirstFiveMinutesAuthorityV1Test.kt"
        )

    private fun readWelcomeScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
    )

    private fun readSessionController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readGestures(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/brain1interactionstandard/model/UniversalInteractionGestures.kt"
    )

    private fun readLessonScreens(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
    )
}
