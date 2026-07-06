package com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.audit

import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachEngine
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachExperience
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.metadata.PatientCommunicationCoachMetadata
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningService
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object PatientCommunicationCoachAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-patient-coach/README.md")

    fun coachEngineExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/coach/CommunicationCoachEngine.kt"
        )

    fun coachDialoguesPresent(): Boolean =
        PatientCommunicationCoachExperience.phraseIntroDialogues().isNotEmpty() &&
            PatientCommunicationCoachExperience.repeatPhraseDialogues().isNotEmpty() &&
            PatientCommunicationCoachExperience.patienceDialogues().isNotEmpty()

    fun gradualGestureProgression(): Boolean =
        com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine.catalogHasNoOverwhelmingGestureJumps()

    fun dailyPhrasesPrioritized(): Boolean =
        com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine.dailyEssentialsFrontLoaded()

    fun adaptiveOfferEnriched(): Boolean {
        val source = readAdaptiveLearningService() ?: return false
        return source.contains("showRepeatPhrase") && source.contains("caregiverCoachHint")
    }

    fun trainingSessionWired(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        return controller.contains("coachBeginLesson") &&
            controller.contains("PatientCommunicationCoachEngine")
    }

    fun caregiverProgressVisible(): Boolean {
        val screens = readTrainingLessonScreens() ?: return false
        return screens.contains("CaregiverCoachProgressStrip")
    }

    fun coachUiStateInTraining(): Boolean {
        val state = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/state/GuidedTrainingUiState.kt"
        ) ?: return false
        return state.contains("coachUiState") && state.contains("coachPacingBlocked")
    }

    fun personalityEngineDialogues(): Boolean =
        PatientCommunicationCoachExperience.difficultyBridgeDialogues().isNotEmpty()

    fun noBrain2Dependency(): Boolean {
        val engine = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/patientcommunicationcoach/PatientCommunicationCoachEngine.kt"
        ) ?: return false
        return !engine.contains("Brain2") && !engine.contains("cloud")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/PatientCommunicationCoachAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaPatientCommunicationCoachV1")
    }

    fun deviceTestingChecklistUpdated(): Boolean {
        val suite = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/suites/DeviceTestSuites.kt"
        ) ?: return false
        return suite.contains("PatientCommunicationCoach") || suite.contains("PATIENT_COACH")
    }

    fun metadataDefinesPhilosophy(): Boolean =
        PatientCommunicationCoachMetadata.COACHING_PHILOSOPHY.contains("patient")

    private fun readAdaptiveLearningService(): String? =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/AdaptiveLearningService.kt"
        )

    private fun readTrainingSessionController(): String? =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        )

    private fun readTrainingLessonScreens(): String? =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        )
}
