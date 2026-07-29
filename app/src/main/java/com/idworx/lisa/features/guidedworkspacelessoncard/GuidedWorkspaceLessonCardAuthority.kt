package com.idworx.lisa.features.guidedworkspacelessoncard

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceLessonCardDock
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec

/**
 * RC8.16 / RC8.18 / RC8.28 — Shared instructional-card system for Guided Learning lessons 16–23.
 *
 * Presentation rules:
 * - content-wrapped height (never internal scrolling)
 * - max height ≈ 45% of the workspace content region (RC8.22 two-method teaching)
 * - always show an executable "Sequence: …" label
 * - docked inside the workspace region below UniversalEyeTrackingHeader
 */
object GuidedWorkspaceLessonCardAuthority {

    const val SEQUENCE_PREFIX: String = "Sequence: "

    /**
     * RC8.18 / RC8.22 — fraction of the available workspace Box height. Cards wrap content up to
     * this ceiling; copy must be shortened rather than scrolled when it would exceed the max.
     */
    const val MaxHeightFraction: Float =
        GuidedLessonExecutionAuthority.LESSON_CARD_MAX_HEIGHT_FRACTION

    val MaxCardWidth: Dp = 236.dp

    /** Safe inset inside the workspace content Box (below header, above bottom chrome). */
    val WorkspaceCardHorizontalPadding: Dp = 10.dp
    val WorkspaceCardTopPadding: Dp = 8.dp
    val WorkspaceCardBottomPadding: Dp = 8.dp

    fun formatSequenceLabel(rawGestureLabel: String): String {
        val trimmed = rawGestureLabel.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith(SEQUENCE_PREFIX)) return trimmed
        // RC8.33 — dual labelled sequences (Decrease:/Increase:) keep authored lines.
        if (trimmed.contains('\n') || trimmed.contains("Decrease:", ignoreCase = true)) {
            return trimmed
        }
        return "$SEQUENCE_PREFIX$trimmed"
    }

    fun stripSequencePrefix(displayed: String): String =
        displayed.removePrefix(SEQUENCE_PREFIX).trim()

    /** Every real-workspace navigation lesson (lessons 16–23) uses this compact card. */
    fun usesSharedCompactCard(action: NavigationAction): Boolean = when (action) {
        NavigationAction.MoveToMedicalCategory,
        NavigationAction.SelectCategory,
        NavigationAction.SelectPhrase,
        NavigationAction.CloseMenu,
        NavigationAction.NextPage,
        NavigationAction.PreviousPage,
        NavigationAction.TriggerEmergency,
        NavigationAction.AdjustSensitivity,
        NavigationAction.ResetSequence,
        NavigationAction.OpenMenu,
        NavigationAction.MenuSelectVoice,
        NavigationAction.OpenVoice,
        NavigationAction.BackFromDestination,
        NavigationAction.MenuSelectSettings,
        NavigationAction.OpenSettings,
        NavigationAction.FinishGuidedLearning,
        NavigationAction.OpenCategories -> true
        else -> false
    }

    fun displayedSequenceFor(
        action: NavigationAction,
        highlightedPhraseGesture: String? = null
    ): String = formatSequenceLabel(
        GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(action, highlightedPhraseGesture)
    )

    fun hasExecutableSequence(
        action: NavigationAction,
        highlightedPhraseGesture: String? = null
    ): Boolean {
        val raw = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(action, highlightedPhraseGesture)
        return raw.isNotBlank() &&
            !raw.contains("Blink the highlighted", ignoreCase = true) &&
            Regex("""L-?\d+\s+R-?\d+""").containsMatchIn(raw)
    }

    fun dockKeepsTargetVisible(dock: GuidedWorkspaceLessonCardDock): Boolean =
        dock == GuidedWorkspaceLessonCardDock.TopStart ||
            dock == GuidedWorkspaceLessonCardDock.TopEnd ||
            dock == GuidedWorkspaceLessonCardDock.BottomStart ||
            dock == GuidedWorkspaceLessonCardDock.BottomEnd

    /**
     * RC8.33 — named protected targets a Lesson 23 card must not cover, by highlight.
     * Constraint-based: placement uses [GuidedWorkspaceTrainingSpec.cardDockFor] zones.
     */
    fun protectedTargetsForHighlight(
        highlight: GuidedWorkspaceHighlightTarget?
    ): List<String> = when (highlight) {
        GuidedWorkspaceHighlightTarget.CategoryNextPage ->
            listOf("NextPage", "PageIndicator")
        GuidedWorkspaceHighlightTarget.CategoryRow ->
            listOf("SettingsAndControls", "SettingsSequenceL5R5")
        GuidedWorkspaceHighlightTarget.SettingsHubSensitivity ->
            listOf("SensitivityCard", "SensitivitySequenceL2R0")
        GuidedWorkspaceHighlightTarget.IncreaseOrDecreaseValue,
        GuidedWorkspaceHighlightTarget.IncreaseValue,
        GuidedWorkspaceHighlightTarget.DecreaseValue ->
            listOf("Decrease", "Increase", "CurrentSensitivityValue")
        GuidedWorkspaceHighlightTarget.Back ->
            listOf("MainBack", "RailBack")
        else -> emptyList()
    }

    fun dockAvoidsProtectedTargets(
        highlight: GuidedWorkspaceHighlightTarget?,
        dock: GuidedWorkspaceLessonCardDock = GuidedWorkspaceTrainingSpec.cardDockFor(highlight)
    ): Boolean {
        if (protectedTargetsForHighlight(highlight).isEmpty()) return true
        return when (highlight) {
            GuidedWorkspaceHighlightTarget.CategoryNextPage,
            GuidedWorkspaceHighlightTarget.CategoryPreviousPage,
            GuidedWorkspaceHighlightTarget.Back,
            GuidedWorkspaceHighlightTarget.IncreaseOrDecreaseValue,
            GuidedWorkspaceHighlightTarget.IncreaseValue,
            GuidedWorkspaceHighlightTarget.DecreaseValue,
            GuidedWorkspaceHighlightTarget.CategoryRow ->
                dock == GuidedWorkspaceLessonCardDock.TopStart ||
                    dock == GuidedWorkspaceLessonCardDock.TopEnd
            GuidedWorkspaceHighlightTarget.SettingsHubSensitivity ->
                dock == GuidedWorkspaceLessonCardDock.BottomEnd ||
                    dock == GuidedWorkspaceLessonCardDock.BottomStart ||
                    dock == GuidedWorkspaceLessonCardDock.TopEnd
            else -> dockKeepsTargetVisible(dock)
        }
    }
}
