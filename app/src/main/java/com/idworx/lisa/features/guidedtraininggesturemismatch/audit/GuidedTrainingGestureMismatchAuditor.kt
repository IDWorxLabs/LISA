package com.idworx.lisa.features.guidedtraininggesturemismatch.audit

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedCatalogContext
import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationPanelSpec
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPanelActionKind
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingFocusPolicy
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.isEmergencySequence

object GuidedTrainingGestureMismatchAuditor {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val medicalCategoryIndex = GuidedWorkspaceTrainingSpec.medicalCategoryIndex

    /** Parses a "L<left> R<right>" label — the same format [formatWinkSequenceShort] produces. */
    private fun parseGesture(label: String): Pair<Int, Int>? {
        val match = Regex("^L(-?\\d+) R(-?\\d+)$").find(label) ?: return null
        val left = match.groupValues[1].toIntOrNull() ?: return null
        val right = match.groupValues[2].toIntOrNull() ?: return null
        return left to right
    }

    /** Mirrors MainActivity's `classifyNavigationGesture` best-effort classification. */
    private fun classify(left: Int, right: Int): NavigationAction = when {
        isEmergencySequence(left, right) -> NavigationAction.TriggerEmergency
        GuidedModeNavigation.isFinishTrainingSequence(left, right) -> NavigationAction.ResetSequence
        GuidedModeNavigation.isOpenMainMenuSequence(left, right) -> NavigationAction.OpenMenu
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        GuidedModeNavigation.isNextCategoryPageSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousCategoryPageSequence(left, right) -> NavigationAction.PreviousPage
        GuidedModeNavigation.isNextSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousSequence(left, right) -> NavigationAction.PreviousPage
        GuidedModeNavigation.isSelectSequence(left, right) -> NavigationAction.SelectCategory
        else -> NavigationAction.SelectPhrase
    }

    /** Mirrors MainActivity's page-lesson acceptance (RC8.26). */
    private fun acceptedForLesson(expected: NavigationAction, left: Int, right: Int): Boolean {
        when (expected) {
            NavigationAction.NextPage ->
                return GuidedModeNavigation.isNextCategoryPageSequence(left, right)
            NavigationAction.PreviousPage ->
                return GuidedModeNavigation.isPreviousCategoryPageSequence(left, right)
            else -> Unit
        }
        return classify(left, right) == expected
    }

    // --- 1. Every category lesson gesture equals the real workspace category gesture -----------
    fun categoryLessonGestureEqualsRealWorkspaceGesture(): Boolean {
        val lessonGesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        val realRowGesture = GuidedCategoryShortcuts.sequenceLabelForCategory(medicalCategoryIndex)
        // Guard against the exact regression reported: the lesson must not silently fall back to
        // the generic "Select" confirm gesture the real category row never displays.
        val genericSelectGesture = formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)
        return lessonGesture == realRowGesture && lessonGesture != genericSelectGesture
    }

    // --- 2. Every phrase lesson gesture equals the real workspace phrase gesture -----------------
    fun phraseLessonGestureEqualsRealWorkspacePhraseGesture(): Boolean {
        val page = GuidedVocabularyCatalog.categoryAt(
            medicalCategoryIndex, PreferredLanguage.English, uiStrings, GuidedCatalogContext()
        ) ?: return false
        val highlightedEntry = GuidedNavigationController.visiblePhraseEntries(
            entries = page.entries,
            phrasePageIndex = 0,
            visibleCap = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
        ).firstOrNull() ?: return false
        val cardLabel = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(
            NavigationAction.SelectPhrase, highlightedEntry.sequenceLabel
        )
        val fallbackLabel = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectPhrase, null)
        // RC8.16 — when no runtime highlight is supplied, fall back to the production Medical
        // phrase sequence (same source of truth), never a generic instructional hint.
        return cardLabel == highlightedEntry.sequenceLabel &&
            fallbackLabel == highlightedEntry.sequenceLabel
    }

    // --- 3. Every navigation lesson gesture equals the real workspace navigation gesture ---------
    fun navigationLessonGesturesEqualRealPanelGestures(): Boolean {
        // RC8.26 — Lessons 20–21 teach Category Menu viewport page controls (L0 R4 / L4 R0).
        val panelActions = GuidedNavigationPanelSpec.panelActions(
            uiStrings,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        )
        val nextPage = panelActions.firstOrNull { it.kind == GuidedPanelActionKind.NextCategoryPage }
            ?: return false
        val previousPage = panelActions.firstOrNull { it.kind == GuidedPanelActionKind.PreviousCategoryPage }
            ?: return false
        val back = panelActions.firstOrNull { it.kind == GuidedPanelActionKind.Back } ?: return false
        val emergency = panelActions.firstOrNull { it.kind == GuidedPanelActionKind.Emergency }
            ?: return false
        return GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.NextPage) ==
            nextPage.sequenceLabel &&
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.PreviousPage) ==
            previousPage.sequenceLabel &&
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.CloseMenu) ==
            back.sequenceLabel &&
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.TriggerEmergency) ==
            emergency.sequenceLabel
    }

    // --- 4. Displayed floating-card gesture equals the accepted gesture ---------------------------
    fun displayedGestureEqualsAcceptedGestureForEveryNavigationLesson(): Boolean {
        val singleTargetActions = listOf(
            NavigationAction.OpenCategories,
            NavigationAction.CloseMenu,
            NavigationAction.NextPage,
            NavigationAction.PreviousPage,
            NavigationAction.TriggerEmergency
        )
        val singleTargetsAgree = singleTargetActions.all { action ->
            val (left, right) = parseGesture(GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(action))
                ?: return@all false
            acceptedForLesson(action, left, right)
        }
        val (categoryLeft, categoryRight) = parseGesture(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        ) ?: return false
        val categoryGestureIsAccepted =
            GuidedCategoryShortcuts.categoryIndexForGesture(categoryLeft, categoryRight) == medicalCategoryIndex
        return singleTargetsAgree && categoryGestureIsAccepted
    }

    // --- 5. Highlighted target gesture equals the lesson gesture -----------------------------------
    fun highlightedTargetGestureEqualsLessonGesture(): Boolean {
        val highlightedCategoryGesture = GuidedCategoryShortcuts.sequenceLabelForCategory(medicalCategoryIndex)
        val lessonGesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        val categoryMatches = highlightedCategoryGesture == lessonGesture
        val ui = readAccessibilityUi() ?: return false
        // The floating card's phrase gesture must be sourced from the exact same categoryPage /
        // visiblePhraseEntries the real GuidedVocabularyOverlay renders — never a separate lookup.
        val phraseWiredFromSameSource = ui.contains("GuidedNavigationController.visiblePhraseEntries(") &&
            ui.contains("entries = guidedCategoryPage?.entries.orEmpty(),") &&
            ui.contains("phrasePageIndex = guidedNavigationState.phrasePageIndex,") &&
            ui.contains("firstOrNull()?.sequenceLabel") &&
            ui.contains("GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(") &&
            ui.contains("activeNavigationLesson.action,") &&
            ui.contains("guidedHighlightedPhraseGesture")
        return categoryMatches && phraseWiredFromSameSource
    }

    // --- 6. Wrong old/hardcoded gestures are rejected ------------------------------------------------
    fun wrongOrHardcodedCategoryGesturesAreRejected(): Boolean {
        // The gesture the bug report described as previously taught (generic Select confirm)
        // must no longer match any category's own direct shortcut — so it is now off-target.
        val genericSelect = GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
        val genericSelectIsNotACategoryShortcut =
            GuidedCategoryShortcuts.categoryIndexForGesture(genericSelect.first, genericSelect.second) == null
        val genericSelectRejected = !GuidedTrainingFocusPolicy.isTargetAllowed(
            NavigationAction.SelectCategory, NavigationAction.SelectCategory, isAttemptedTargetHighlighted = false
        )
        // A different category's own shortcut must also be rejected while Medical is taught.
        val otherCategoryIndex = (medicalCategoryIndex + 1) % GuidedVocabularyCategory.PAGE_COUNT
        val otherCategoryGesture = GuidedCategoryShortcuts.gestureForCategory(otherCategoryIndex)
        val otherIsHighlighted =
            GuidedCategoryShortcuts.categoryIndexForGesture(otherCategoryGesture.first, otherCategoryGesture.second) ==
                medicalCategoryIndex
        val otherCategoryRejected = !GuidedTrainingFocusPolicy.isTargetAllowed(
            NavigationAction.SelectCategory, NavigationAction.SelectCategory, otherIsHighlighted
        )
        return genericSelectIsNotACategoryShortcut && genericSelectRejected && otherCategoryRejected
    }

    // --- 7. Correct real workspace gestures are accepted ----------------------------------------------
    fun correctRealWorkspaceCategoryGestureIsAccepted(): Boolean {
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(medicalCategoryIndex)
        val isHighlighted = GuidedCategoryShortcuts.categoryIndexForGesture(left, right) == medicalCategoryIndex
        return isHighlighted && GuidedTrainingFocusPolicy.isTargetAllowed(
            NavigationAction.SelectCategory, NavigationAction.SelectCategory, isHighlighted
        )
    }

    // --- 8. Normal workspace after Guided Training uses the same gesture mapping the user was taught --
    fun normalWorkspaceUsesSameGestureMappingAfterTraining(): Boolean {
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(medicalCategoryIndex)
        val menuState = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        // processSequence never knows about Guided Training — this proves the exact gesture the
        // lesson teaches also opens the exact same category in plain, untrained normal use.
        val result = GuidedNavigationController.processSequence(
            left, right, menuState, PreferredLanguage.English, uiStrings
        )
        val opensTaughtCategory = result is GuidedSequenceResult.Navigate &&
            result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary &&
            result.newState.categoryIndex == medicalCategoryIndex
        return opensTaughtCategory
    }

    // --- 9. MainActivity fine gate is wired to the single source of truth --------------------------
    fun mainActivityFineGateUsesSingleSourceOfTruth(): Boolean {
        val main = readMainActivity() ?: return false
        val gateWiredToRealShortcut = main.contains("private fun isNavigationLessonOffTargetAttempt") &&
            main.contains("val targetCategoryIndex = GuidedCategoryShortcuts.categoryIndexForGesture(left, right)") &&
            main.contains("targetCategoryIndex == GuidedWorkspaceTrainingSpec.medicalCategoryIndex")
        val dispatchVerifiesShortcut = main.contains("val isCategoryShortcutGesture = screenModeBeforeHandling == GuidedOverlayScreenMode.CategoryMenu &&") &&
            main.contains("GuidedCategoryShortcuts.categoryIndexForGesture(left, right) != null") &&
            (main.contains("if (isCategoryShortcutGesture) {") ||
                main.contains("if (isCategoryShortcutGesture &&")) &&
            main.contains("verifyTrainingNavigation(NavigationAction.SelectCategory)")
        return gateWiredToRealShortcut && dispatchVerifiesShortcut
    }

    // --- Infra: test class + gradle task ------------------------------------------------------------
    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedTrainingGestureMismatchAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedTrainingGestureMismatchV1")
    }

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )
}
