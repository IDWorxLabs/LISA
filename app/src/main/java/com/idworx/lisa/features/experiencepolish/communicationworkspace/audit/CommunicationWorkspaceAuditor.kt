package com.idworx.lisa.features.experiencepolish.communicationworkspace.audit

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.experiencepolish.communicationworkspace.CommunicationWorkspaceExperience
import com.idworx.lisa.features.experiencepolish.communicationworkspace.WorkspaceGestureLayers
import com.idworx.lisa.features.experiencepolish.communicationworkspace.metadata.CommunicationWorkspaceMetadata
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object CommunicationWorkspaceAuditor {

    fun phaseDocumented(): Boolean =
        ZeroTouchFileProbe.fileExists("features/experience-polish-phase-b/README.md")

    fun workspaceEntryAfterTraining(): Boolean {
        val main = readMainActivity() ?: return false
        val store = readReleaseStore() ?: return false
        return main.contains("maybePlayWorkspaceEntryIntro") &&
            main.contains("CommunicationWorkspaceEntryHandler") &&
            store.contains("workspace_entry_intro")
    }

    fun navigationClarityImproved(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        return ui.contains("workspaceContextHint") && ui.contains("CaregiverHelpStrip")
    }

    fun categoryMenuGuidanceExists(): Boolean =
        CommunicationWorkspaceExperience.categoryMenuDialogues().isNotEmpty()

    fun phraseSelectionGuidanceExists(): Boolean =
        CommunicationWorkspaceExperience.phraseSelectionDialogues().isNotEmpty()

    fun backBehaviorDocumented(): Boolean =
        CommunicationWorkspaceExperience.backBehaviorDialogues().isNotEmpty() &&
            GuidedModeNavigation.BACK_LEFT == 2 && GuidedModeNavigation.BACK_RIGHT == 2

    fun gestureLayersSeparated(): Boolean {
        val layers = readWorkspaceGestureLayers() ?: return false
        return layers.contains("WorkspaceGestureLayers") &&
            layers.contains("Brain1DecisionGesture") &&
            layers.contains("WorkspaceNavigationGesture")
    }

    fun fatigueReductionPresent(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        return ui.contains("workspacePatienceHint") &&
            CommunicationWorkspaceExperience.fatiguePatienceDialogues().isNotEmpty()
    }

    fun emergencyAccessDocumented(): Boolean =
        CommunicationWorkspaceExperience.emergencyAccessDialogues().any {
            it.contains("confirm", ignoreCase = true)
        }

    fun caregiverHelpVisible(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        val strings = readUiStrings() ?: return false
        return ui.contains("CaregiverHelpStrip") &&
            strings.contains("workspaceCaregiverHelpLegend")
    }

    fun personalityEngineDialogues(): Boolean =
        CommunicationWorkspaceExperience.entryDialogues().size >= 3

    fun zeroTouchPreserved(): Boolean {
        val visibility = readGuidedMode() ?: return false
        return visibility.contains("GuidedVocabularyOverlayVisibility") &&
            visibility.contains("onboardingCompleted")
    }

    fun deviceTestingChecklistUpdated(): Boolean {
        val suite = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/suites/DeviceTestSuites.kt"
        ) ?: return false
        return suite.contains("PhaseBCommunicationWorkspace") || suite.contains("PHASE_B")
    }

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaExperiencePhaseBCommunicationWorkspaceV1")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/CommunicationWorkspaceAuthorityV1Test.kt"
        )

    fun metadataDefinesGestures(): Boolean =
        CommunicationWorkspaceMetadata.WORKSPACE_NAVIGATION_GESTURES.size >= 5

    fun workspaceOverlayUsesGuidedNavigation(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        return ui.contains("GuidedVocabularyOverlay") && ui.contains("onGuidedBack")
    }

    fun noBrain2Dependency(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return true
        return !gradle.contains("Brain2")
    }

    private fun readMainActivity(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")

    private fun readReleaseStore(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaReleaseStore.kt")

    private fun readGuidedModeUi(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")

    private fun readGuidedMode(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaGuidedMode.kt")

    private fun readUiStrings(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaUiStrings.kt")

    private fun readAccessibilityUi(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")

    private fun readWorkspaceGestureLayers(): String? =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/communicationworkspace/WorkspaceGestureLayers.kt"
        )
}
