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
    private val conversationCategoryIndex = GuidedWorkspaceTrainingSpec.conversationCategoryIndex

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
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        GuidedModeNavigation.isNextSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousSequence(left, right) -> NavigationAction.PreviousPage
        GuidedModeNavigation.isSelectSequence(left, right) -> NavigationAction.SelectCategory
        else -> NavigationAction.SelectPhrase
    }

    // --- 1. Every category lesson gesture equals the real workspace category gesture -----------
    fun categoryLessonGestureEqualsRealWorkspaceGesture(): Boolean {
        val lessonGesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        val realRowGesture = GuidedCategoryShortcuts.sequenceLabelForCategory(conversationCategoryIndex)
        // Guard against the exact regression reported: the lesson must not silently fall back to
        // the generic "Select" confirm gesture the real category row never displays.
        val genericSelectGesture = formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)
        return lessonGesture == realRowGesture && lessonGesture != genericSelectGesture
    }

    // --- 2. Every phrase lesson gesture equals the real workspace phrase gesture -----------------
    fun phraseLessonGestureEqualsRealWorkspacePhraseGesture(): Boolean {
        val page = GuidedVocabularyCatalog.categoryAt(
            conversationCategoryIndex, PreferredLanguage.English, uiStrings, GuidedCatalogContext()
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
        // When a real highlighted entry is supplied the card must show its exact code; only
        // absent a runtime entry does it fall back to the generic instructional hint.
        return cardLabel == highlightedEntry.sequenceLabel && fallbackLabel != highlightedEntry.sequenceLabel
    }

    // --- 3. Every navigation lesson gesture equals the real workspace navigation gesture ---------
    fun navigationLessonGesturesEqualRealPanelGestures(): Boolean {
        val panelActions = GuidedNavigationPanelSpec.panelActions(uiStrings, GuidedNavigationPanelSpec.PanelContext.Vocabulary)
        // Index order mirrors GuidedNavigationPanelSpec.panelActions: Previous, Select, Back, Categories, Emergency, Next.
        val expectedIndexByAction = mapOf(
            NavigationAction.PreviousPage to 0,
            NavigationAction.CloseMenu to 2,
            NavigationAction.OpenCategories to 3,
            NavigationAction.TriggerEmergency to 4,
            NavigationAction.NextPage to 5
        )
        return expectedIndexByAction.all { (action, index) ->
            val panelGesture = panelActions[index].sequenceLabel
            val lessonGesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(action)
            panelGesture == lessonGesture
        }
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
            val (left, right) = parseGesture(GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(action)) ?: return@all false
            classify(left, right) == action
        }
        val (categoryLeft, categoryRight) = parseGesture(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        ) ?: return false
        val categoryGestureIsAccepted =
            GuidedCategoryShortcuts.categoryIndexForGesture(categoryLeft, categoryRight) == conversationCategoryIndex
        return singleTargetsAgree && categoryGestureIsAccepted
    }

    // --- 5. Highlighted target gesture equals the lesson gesture -----------------------------------
    fun highlightedTargetGestureEqualsLessonGesture(): Boolean {
        val highlightedCategoryGesture = GuidedCategoryShortcuts.sequenceLabelForCategory(conversationCategoryIndex)
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
        // A different category's own shortcut must also be rejected while Conversation is taught.
        val otherCategoryIndex = (conversationCategoryIndex + 1) % GuidedVocabularyCategory.PAGE_COUNT
        val otherCategoryGesture = GuidedCategoryShortcuts.gestureForCategory(otherCategoryIndex)
        val otherIsHighlighted =
            GuidedCategoryShortcuts.categoryIndexForGesture(otherCategoryGesture.first, otherCategoryGesture.second) ==
                conversationCategoryIndex
        val otherCategoryRejected = !GuidedTrainingFocusPolicy.isTargetAllowed(
            NavigationAction.SelectCategory, NavigationAction.SelectCategory, otherIsHighlighted
        )
        return genericSelectIsNotACategoryShortcut && genericSelectRejected && otherCategoryRejected
    }

    // --- 7. Correct real workspace gestures are accepted ----------------------------------------------
    fun correctRealWorkspaceCategoryGestureIsAccepted(): Boolean {
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(conversationCategoryIndex)
        val isHighlighted = GuidedCategoryShortcuts.categoryIndexForGesture(left, right) == conversationCategoryIndex
        return isHighlighted && GuidedTrainingFocusPolicy.isTargetAllowed(
            NavigationAction.SelectCategory, NavigationAction.SelectCategory, isHighlighted
        )
    }

    // --- 8. Normal workspace after Guided Training uses the same gesture mapping the user was taught --
    fun normalWorkspaceUsesSameGestureMappingAfterTraining(): Boolean {
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(conversationCategoryIndex)
        val menuState = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        // processSequence never knows about Guided Training — this proves the exact gesture the
        // lesson teaches also opens the exact same category in plain, untrained normal use.
        val result = GuidedNavigationController.processSequence(
            left, right, menuState, PreferredLanguage.English, uiStrings
        )
        val opensTaughtCategory = result is GuidedSequenceResult.Navigate &&
            result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary &&
            result.newState.categoryIndex == conversationCategoryIndex
        return opensTaughtCategory
    }

    // --- 9. MainActivity fine gate is wired to the single source of truth --------------------------
    fun mainActivityFineGateUsesSingleSourceOfTruth(): Boolean {
        val main = readMainActivity() ?: return false
        val gateWiredToRealShortcut = main.contains("private fun isNavigationLessonOffTargetAttempt") &&
            main.contains("val targetCategoryIndex = GuidedCategoryShortcuts.categoryIndexForGesture(left, right)") &&
            main.contains("targetCategoryIndex == GuidedWorkspaceTrainingSpec.conversationCategoryIndex")
        val dispatchVerifiesShortcut = main.contains("val isCategoryShortcutGesture = screenModeBeforeHandling == GuidedOverlayScreenMode.CategoryMenu &&") &&
            main.contains("GuidedCategoryShortcuts.categoryIndexForGesture(left, right) != null") &&
            main.contains("if (isCategoryShortcutGesture) {") &&
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
