package com.idworx.lisa.features.experiencepolish.caregiverconfidence.audit

import com.idworx.lisa.features.experiencepolish.caregiverconfidence.CaregiverConfidenceEngine
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.CaregiverConfidenceExperience
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.metadata.CaregiverConfidenceMetadata
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object CaregiverConfidenceAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-caregiver-confidence/README.md")

    fun confidenceEngineExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/caregiverconfidence/CaregiverConfidenceEngine.kt"
        )

    fun dialoguesInCatalog(): Boolean = CaregiverConfidenceEngine.catalogHasAllMoments()

    fun personalityEngineWired(): Boolean {
        val engine = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/DefaultLisaPersonalityEngine.kt"
        ) ?: return false
        return engine.contains("generateCaregiverSupportDialogue") &&
            engine.contains("CaregiverSupportDialogueProvider")
    }

    fun setupScreenWired(): Boolean {
        val screens = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingSetupScreen.kt"
        ) ?: return false
        return screens.contains("TrainingSetupScreen") &&
            screens.contains("SetupDetectionStatusRow")
    }

    fun calibrationScreenWired(): Boolean {
        val screens = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt"
        ) ?: return false
        return screens.contains("TrainingCalibrationScreen") &&
            screens.contains("Look at the blue dot")
    }

    /**
     * The Guided Communication screen no longer surfaces a caregiver tracking-recovery card —
     * face-loss recovery is handled purely through [com.idworx.lisa.LisaCommunicationState]
     * (e.g. WaitingForFace) so MainActivity must not re-introduce the retired
     * `refreshCaregiverSupport`/`CaregiverConfidenceEngine.communicationSupport` wiring.
     */
    fun mainActivityFreeOfRetiredTrackingRecoveryWiring(): Boolean {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: return false
        return !main.contains("refreshCaregiverSupport") &&
            !main.contains("CaregiverConfidenceEngine")
    }

    fun calibrationAdapterUsesEngine(): Boolean {
        val adapter = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/calibrationreliability/integration/CalibrationPersonalityAdapter.kt"
        ) ?: return false
        return adapter.contains("CaregiverConfidenceEngine.recommendationHint")
    }

    /**
     * The Guided Communication screen carries no Caregiver card at all anymore — the earlier
     * contextual strip (surfaced via [com.idworx.lisa.MainActivity]'s tracking/troubleshooting
     * state on the Accessibility panel) was removed outright to give the Communication Workspace
     * the full screen. Neither the strip composable, its engine call, nor its UI state may exist.
     */
    fun communicationScreenFreeOfCaregiverPanel(): Boolean {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: return false
        val accessibilityUi = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
        ) ?: return false
        return !main.contains("CaregiverConfidenceEngine.communicationSupport") &&
            !main.contains("uiCaregiverSupport") &&
            !accessibilityUi.contains("CaregiverSupportStrip") &&
            !ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/experiencepolish/caregiverconfidence/ui/CaregiverSupportStrip.kt"
            )
    }

    fun noBrain2Dependency(): Boolean {
        val engine = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/caregiverconfidence/CaregiverConfidenceEngine.kt"
        ) ?: return false
        return !engine.contains("Brain2") && !engine.contains("cloud")
    }

    fun experienceSurfacesAllMoments(): Boolean =
        CaregiverConfidenceExperience.phonePositionDialogues().isNotEmpty() &&
            CaregiverConfidenceExperience.trackingRecoveryDialogues().isNotEmpty() &&
            CaregiverConfidenceExperience.whatToDoNowDialogues().isNotEmpty()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/CaregiverConfidenceAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaCaregiverConfidenceV1")
    }

    fun deviceTestingChecklistUpdated(): Boolean {
        val suite = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/suites/DeviceTestSuites.kt"
        ) ?: return false
        return suite.contains("CaregiverConfidence") || suite.contains("CAREGIVER_CONFIDENCE")
    }
}
