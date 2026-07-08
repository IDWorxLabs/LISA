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

    /**
     * Navigation clarity now comes from the always-visible, icon-labelled Navigation Panel
     * itself — not from a competing instructional text block. The old "Blink a phrase to
     * speak…/Gesture guide…/Take your time…" strip and header context-hint were removed so the
     * Communication Workspace stays focused on Vocabulary, the current category, the phrase
     * list, and the Navigation Panel.
     */
    fun navigationClarityImproved(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        return ui.contains("GuidedModeNavigationPanel") &&
            !ui.contains("CaregiverHelpStrip") &&
            !ui.contains("contextHint")
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

    /**
     * Fatigue/patience reassurance is delivered through the Personality Engine's spoken dialogue
     * catalog (e.g. "There's no rush.") rather than a permanent on-screen text block, keeping the
     * workspace visually calm while still offering comfort at the moments it is needed.
     */
    fun fatigueReductionPresent(): Boolean =
        CommunicationWorkspaceExperience.fatiguePatienceDialogues().isNotEmpty()

    fun emergencyAccessDocumented(): Boolean =
        CommunicationWorkspaceExperience.emergencyAccessDialogues().any {
            it.contains("confirm", ignoreCase = true)
        }

    /**
     * The Caregiver card has been removed entirely from the Guided Communication screen — not
     * even a contextual strip remains — so the Communication Workspace can occupy the full
     * screen with no competing panel and no leftover gap where it used to sit.
     */
    fun freeOfCaregiverPanel(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        return !ui.contains("CaregiverSupportStrip") && !ui.contains("caregiverSupport")
    }

    /**
     * The header used to stack "Vocabulary" and the category name directly above the list's own
     * bold category title (with a redundant "Choose Category" row sitting above the phrases too)
     * — three duplicated labels competing with the single title the list actually needs. While
     * plainly browsing phrases the header now shows only the small "Communication Workspace"
     * label, and the category title plus a directional, page-aware scroll hint is the sole header
     * directly above the phrase list.
     */
    fun headerFreeOfDuplicateVocabularyLabels(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        return ui.contains("isPlainVocabularyBrowsing") &&
            !ui.contains("GuidedCategoryMenuAccessRow") &&
            ui.contains("uiStrings.guidedPhrasePageScrollHint")
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

    private fun readAccessibilityUi(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")

    private fun readWorkspaceGestureLayers(): String? =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/experiencepolish/communicationworkspace/WorkspaceGestureLayers.kt"
        )
}
