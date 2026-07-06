package com.idworx.lisa.features.experiencepolish.emotionalpresence.audit

import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceEngine
import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceExperience
import com.idworx.lisa.features.experiencepolish.emotionalpresence.metadata.EmotionalPresenceMetadata
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object EmotionalPresenceAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-emotional-presence/README.md")

    fun presenceEngineExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/emotionalpresence/EmotionalPresenceEngine.kt"
        )

    fun presenceDialoguesInCatalog(): Boolean = EmotionalPresenceEngine.catalogHasPresenceDialogues()

    fun personalityEngineWired(): Boolean {
        val engine = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/DefaultLisaPersonalityEngine.kt"
        ) ?: return false
        return engine.contains("generatePresenceDialogue") &&
            engine.contains("EmotionalPresenceDialogueProvider")
    }

    fun rateLimitingDefined(): Boolean =
        EmotionalPresenceEngine.MAX_PRESENCE_PER_HOUR <= 10 &&
            EmotionalPresenceEngine.MIN_SPEAK_INTERVAL_MS >= 60_000L

    fun mainActivityWired(): Boolean {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: return false
        return main.contains("EmotionalPresenceEngine") &&
            main.contains("PresenceSessionTracker") &&
            main.contains("LongPauseEncouragement")
    }

    fun trainingSessionWired(): Boolean {
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: return false
        return controller.contains("EmotionalPresenceEngine") &&
            controller.contains("FatigueCheckIn")
    }

    fun caregiverReassuranceWired(): Boolean {
        val coach = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/patientcommunicationcoach/PatientCommunicationCoachEngine.kt"
        ) ?: return false
        return coach.contains("EmotionalPresenceEngine") &&
            coach.contains("caregiverReassurance")
    }

    fun noBrain2Dependency(): Boolean {
        val engine = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/emotionalpresence/EmotionalPresenceEngine.kt"
        ) ?: return false
        return !engine.contains("Brain2") && !engine.contains("cloud")
    }

    fun experienceSurfacesAllMoments(): Boolean =
        EmotionalPresenceExperience.sessionOpeningDialogues().isNotEmpty() &&
            EmotionalPresenceExperience.warmReturnDialogues().isNotEmpty() &&
            EmotionalPresenceExperience.longPauseDialogues().isNotEmpty() &&
            EmotionalPresenceExperience.caregiverReassuranceDialogues().isNotEmpty() &&
            EmotionalPresenceExperience.fatigueCheckInDialogues().isNotEmpty() &&
            EmotionalPresenceExperience.emotionalMilestoneDialogues().isNotEmpty()

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/EmotionalPresenceAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaEmotionalPresenceV1")
    }

    fun deviceTestingChecklistUpdated(): Boolean {
        val suite = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/suites/DeviceTestSuites.kt"
        ) ?: return false
        return suite.contains("EmotionalPresence") || suite.contains("EMOTIONAL_PRESENCE")
    }

    fun metadataDefinesPhilosophy(): Boolean =
        EmotionalPresenceMetadata.PRESENCE_PHILOSOPHY.contains("Personality Engine")
}
