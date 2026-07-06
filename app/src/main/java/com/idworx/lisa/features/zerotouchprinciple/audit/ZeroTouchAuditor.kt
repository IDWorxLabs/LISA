package com.idworx.lisa.features.zerotouchprinciple.audit

import com.idworx.lisa.features.zerotouchprinciple.metadata.ZeroTouchPrincipleMetadata
import java.io.File

object ZeroTouchFileProbe {
    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun readProjectFile(relativePath: String): String? {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                val file = File(candidate, relativePath)
                if (file.exists()) return file.readText()
            }
            dir = dir?.parentFile
        }
        return null
    }

    fun fileExists(relativePath: String): Boolean = readProjectFile(relativePath) != null
}

object ZeroTouchAuditor {

    fun welcomeIsConversational(): Boolean {
        val welcome = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
        ) ?: return false
        return !welcome.contains("\"Begin Learning\"") &&
            !welcome.contains("\"Skip Tutorial\"") &&
            welcome.contains("Lisa is speaking")
    }

    fun firstConversationDialogueExists(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return ZeroTouchPrincipleMetadata.REQUIRED_FIRST_CONVERSATION_ELEMENTS.all {
            catalog.contains(it, ignoreCase = true)
        }
    }

    fun noTapContinueInGuidedLearning(): Boolean {
        val paths = listOf(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt",
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt",
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
        )
        return paths.mapNotNull { ZeroTouchFileProbe.readProjectFile(it) }.all { content ->
            !content.contains("TrainingPrimaryButton(\"Continue\"") &&
                !content.contains("TrainingSecondaryButton(\"Continue\"")
        }
    }

    fun noUserBlameInCatalog(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return ZeroTouchPrincipleMetadata.FORBIDDEN_USER_BLAME_PHRASES.none { phrase ->
            catalog.contains("\"$phrase", ignoreCase = true) ||
                catalog.contains("\"$phrase.", ignoreCase = true)
        }
    }

    fun narrationAutoAdvanceExists(): Boolean {
        val narration = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/audio/OnboardingNarrationController.kt"
        ) ?: return false
        return narration.contains("onSequenceComplete")
    }

    fun waitingDialogueExists(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return catalog.contains("I'll wait until you're ready", ignoreCase = true)
    }

    fun personalityDrivesOnboarding(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/LisaPersonalityEngine.kt"
        )

    fun companionMemoryPersonalizesReturning(): Boolean {
        val catalog = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/dialogue/DefaultDialogueCatalog.kt"
        ) ?: return false
        return catalog.contains("Welcome back.", ignoreCase = true) &&
            catalog.contains("continue where we left off", ignoreCase = true)
    }

    fun noBrain2Dependency(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return true
        return !gradle.contains("Brain2")
    }

    fun noLlmDependency(): Boolean {
        val engine = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/DefaultLisaPersonalityEngine.kt"
        ) ?: return true
        return listOf("OpenAI", "ChatGPT", "LLM").none { engine.contains(it) }
    }

    fun noCloudDependency(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return true
        return !gradle.contains("firebase", ignoreCase = true) &&
            !gradle.contains("retrofit", ignoreCase = true)
    }

    fun constitutionalRuleDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/zero-touch-principle/README.md") &&
            ZeroTouchPrincipleMetadata.CONSTITUTIONAL_PRIORITY == 1

    fun autoIntroductionOnWelcome(): Boolean {
        val welcome = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
        ) ?: return false
        return welcome.contains("LaunchedEffect") && welcome.contains("onNarrationStarted")
    }

    fun completionIsConversational(): Boolean {
        val welcome = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
        ) ?: return false
        return welcome.contains("TrainingCompletionScreen") &&
            !welcome.contains("Start Using Lisa")
    }

    fun cameraSetupIsConversational(): Boolean {
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return controller.contains("setupNarration") &&
            (controller.contains("FirstConversationExperience.gettingReadyDialogues") ||
                controller.contains("FirstFiveMinutesExperience.gettingReadyDialogues"))
    }

    fun calibrationIntegrationReused(): Boolean =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        )?.contains("CalibrationGuidedLearningAdapter") == true

    fun communicationLessonsAutoNarrate(): Boolean {
        val screens = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        ) ?: return false
        return screens.contains("LaunchedEffect(phrase") && screens.contains("onLessonNarration")
    }

    fun navigationLessonsEyeControlled(): Boolean {
        val screens = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        ) ?: return false
        return screens.contains("NavigationLessonScreen") && screens.contains("awaitingAction")
    }

    fun caregiverWorkflowMinimized(): Boolean = welcomeIsConversational() && completionIsConversational()

    fun existingSystemsReused(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/validation/GuidedTrainingAuthorityV1.kt"
        ) &&
            ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/personality/engine/LisaPersonalityEngine.kt"
            ) &&
            ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/companionmemory/engine/CompanionMemoryEngine.kt"
            )

    fun firstConversationExperienceModuleExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/zerotouchprinciple/experience/FirstConversationExperience.kt"
        )

    fun narrationSequenceWiredInMainActivity(): Boolean {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: return false
        return main.contains("onSequenceComplete") && main.contains("onNarrationSequenceComplete")
    }

    fun returningUserWired(): Boolean {
        val ui = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
        ) ?: return false
        return ui.contains("isReturningUser")
    }
}
