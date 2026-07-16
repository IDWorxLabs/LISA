package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedVocabularyPagerTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = GuidedCatalogContext(
        responseTimeSec = SequenceProcessingDelay.DEFAULT_SECONDS,
        sensitivityLevel = 5
    )
    private val pages = GuidedVocabularyCatalog.buildPages(
        PreferredLanguage.English,
        uiStrings,
        catalogContext
    )

    private fun preferencesPhrases(): List<String> =
        pages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries.map { it.phrase }

    private fun vocabularyState(categoryIndex: Int = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = categoryIndex,
            draftResponseTimeSec = SequenceProcessingDelay.DEFAULT_SECONDS,
            draftSensitivityLevel = 5
        )

    private fun categoryMenuState(selection: Int = 0): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryIndex = 0,
            categoryMenuSelection = selection
        )

    private fun adjustResponseTimeState(draftSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS): GuidedNavigationState =
        vocabularyState().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
            draftResponseTimeSec = draftSec
        )

    private fun adjustSensitivityState(draftLevel: Int = 5): GuidedNavigationState =
        vocabularyState().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity,
            draftSensitivityLevel = draftLevel
        )

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        context: GuidedCatalogContext = catalogContext
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = context
        )

    @Test
    fun preferences_noLongListOfEveryResponseTimeOption() {
        assertTrue(GuidedVocabularyCatalogValidation.preferencesShowsCompactControlsOnly())
        assertFalse(preferencesPhrases().any { it.startsWith("Set response time to 1") })
        assertFalse(preferencesPhrases().any { it.startsWith("Set sensitivity to 1") })
        assertEquals(4, preferencesPhrases().size)
    }

    @Test
    fun preferences_showsAdjustResponseTime() {
        assertTrue(GuidedVocabularyCatalogValidation.preferencesHasAdjustResponseTime())
        assertTrue(preferencesPhrases().contains("Adjust response time"))
    }

    @Test
    fun preferences_showsAdjustSensitivity() {
        assertTrue(GuidedVocabularyCatalogValidation.preferencesHasAdjustSensitivity())
        assertTrue(preferencesPhrases().contains("Adjust sensitivity"))
    }

    @Test
    fun selectingAdjustResponseTime_opensAdjustmentMode() {
        val adjustEntry = pages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries
            .first { it.phrase == "Adjust response time" }
        val result = process(adjustEntry.left, adjustEntry.right, vocabularyState()) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, result.newState.preferencesAdjustMode)
        assertEquals(SequenceProcessingDelay.DEFAULT_SECONDS, result.newState.draftResponseTimeSec)
    }

    @Test
    fun responseTimeHeader_updatesImmediatelyWhileAdjusting() {
        val state = adjustResponseTimeState(draftSec = 4)
        assertEquals(4, state.displayResponseTimeSec(3))
        val decreased = PreferenceAdjustmentController.decreaseDraft(state)
        assertEquals(3, decreased.displayResponseTimeSec(3))
    }

    @Test
    fun sensitivityHeader_updatesImmediatelyWhileAdjusting() {
        val state = adjustSensitivityState(draftLevel = 5)
        assertEquals(6, state.copy(draftSensitivityLevel = 6).displaySensitivityLevel(5))
        val increased = PreferenceAdjustmentController.increaseDraft(state)
        assertEquals(6, increased.displaySensitivityLevel(5))
    }

    @Test
    fun cancel_restoresSavedHeaderValues() {
        val cancelled = PreferenceAdjustmentController.cancelAdjustment(adjustResponseTimeState(draftSec = 6))
        assertEquals(3, cancelled.displayResponseTimeSec(3))
        // RC7D.27 — Cancel / Back returns to Adjust Settings.
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    @Test
    fun save_persistsDraftValue() {
        val confirming = process(1, 1, adjustResponseTimeState(draftSec = 5)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveResponseTime, confirming.newState.preferencesAdjustMode)
        val result = process(1, 1, confirming.newState) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, result.responseTimeSec)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, result.newState.preferencesAdjustMode)
    }

    @Test
    fun responseTime_L2R0_doesNotDecreaseDraftValue() {
        val result = process(2, 0, adjustResponseTimeState(draftSec = 4)) as GuidedSequenceResult.Navigate
        assertEquals(4, result.newState.draftResponseTimeSec)
        assertTrue(result.newState.adjustmentScrollStep >= 0)
    }

    @Test
    fun responseTime_L0R2_doesNotIncreaseDraftValue() {
        val result = process(0, 2, adjustResponseTimeState(draftSec = 3)) as GuidedSequenceResult.Navigate
        assertEquals(3, result.newState.draftResponseTimeSec)
        assertTrue(result.newState.adjustmentScrollStep > 0)
    }

    @Test
    fun responseTime_L3R1_decreasesDraftValue() {
        val result = process(3, 1, adjustResponseTimeState(draftSec = 4)) as GuidedSequenceResult.Navigate
        assertEquals(3, result.newState.draftResponseTimeSec)
    }

    @Test
    fun responseTime_L1R3_increasesDraftValue() {
        val result = process(1, 3, adjustResponseTimeState(draftSec = 3)) as GuidedSequenceResult.Navigate
        assertEquals(4, result.newState.draftResponseTimeSec)
    }

    @Test
    fun responseTime_L1R1_savesValue() {
        val confirming = process(1, 1, adjustResponseTimeState(draftSec = 5)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveResponseTime, confirming.newState.preferencesAdjustMode)
        val result = process(1, 1, confirming.newState) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, result.responseTimeSec)
    }

    @Test
    fun responseTime_L2R2_cancelsWithoutSaving() {
        val result = process(2, 2, adjustResponseTimeState(draftSec = 6)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, result.newState.preferencesAdjustMode)
    }

    @Test
    fun selectingAdjustSensitivity_opensAdjustmentMode() {
        val adjustEntry = pages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries
            .first { it.phrase == "Adjust sensitivity" }
        val result = process(adjustEntry.left, adjustEntry.right, vocabularyState()) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, result.newState.preferencesAdjustMode)
    }

    @Test
    fun sensitivity_L3R1_decreasesDraftValue() {
        val result = process(3, 1, adjustSensitivityState(draftLevel = 5)) as GuidedSequenceResult.Navigate
        assertEquals(4, result.newState.draftSensitivityLevel)
    }

    @Test
    fun sensitivity_L1R3_increasesDraftValue() {
        val result = process(1, 3, adjustSensitivityState(draftLevel = 5)) as GuidedSequenceResult.Navigate
        assertEquals(6, result.newState.draftSensitivityLevel)
    }

    @Test
    fun sensitivity_L1R1_savesValue() {
        val confirming = process(1, 1, adjustSensitivityState(draftLevel = 7)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSensitivity, confirming.newState.preferencesAdjustMode)
        val result = process(1, 1, confirming.newState) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(7, result.sensitivityLevel)
    }

    @Test
    fun sensitivity_L2R2_cancelsWithoutSaving() {
        val result = process(2, 2, adjustSensitivityState(draftLevel = 8)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, result.newState.preferencesAdjustMode)
    }

    @Test
    fun adjustmentMode_scrollGesturesRemainForOverflow() {
        val scrolled = process(0, 2, adjustResponseTimeState()) as GuidedSequenceResult.Navigate
        assertNotEquals(0, scrolled.newState.adjustmentScrollStep)
        val scrolledUp = process(2, 0, scrolled.newState) as GuidedSequenceResult.Navigate
        assertEquals(0, scrolledUp.newState.adjustmentScrollStep)
    }

    @Test
    fun categoriesGesture_opensCategoryMenuFromVocabularyMode() {
        val result = process(
            GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, vocabularyState(categoryIndex = 0)
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
    }

    @Test
    fun categoriesGesture_opensCategoryMenuFromPreferencesPage() {
        val result = process(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT,
            vocabularyState(categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
    }

    @Test
    fun categoriesGesture_opensCategoryMenuFromResponseTimeAdjustment() {
        val result = process(
            GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, adjustResponseTimeState()
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
        assertEquals(GuidedPreferencesAdjustMode.None, result.newState.preferencesAdjustMode)
    }

    @Test
    fun categoriesGesture_opensCategoryMenuFromSensitivityAdjustment() {
        val result = process(
            GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, adjustSensitivityState()
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
        assertEquals(GuidedPreferencesAdjustMode.None, result.newState.preferencesAdjustMode)
    }

    @Test
    fun everyGuidedMode_hasBackCategoriesAndEmergencyReachable() {
        assertTrue(GuidedNavigationGestureAudit.everyModeHasBackCategoriesAndEmergency())
    }

    @Test
    fun noDuplicateActiveSequencesWithinEachGuidedMode() {
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
    }

    @Test
    fun adjustmentDoesNotUseScrollGesturesForValueChanges() {
        assertTrue(GuidedNavigationGestureAudit.adjustmentDoesNotUseScrollGesturesForValues())
    }

    @Test
    fun globalPanel_hasSixEssentialActionsIncludingCategories() {
        val actions = GuidedNavigationPanelSpec.panelActions(
            uiStrings,
            GuidedNavigationPanelSpec.PanelContext.Vocabulary
        )
        assertEquals(6, actions.size)
        assertEquals(
            formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT),
            actions[3].sequenceLabel
        )
        assertEquals("Categories", actions[3].title)
        assertTrue(GuidedNavigationPanelSpec.allActionsLabeled(actions))
    }

    @Test
    fun allPanelGesturesAreTappableViaTouchSpec() {
        // 7 base panel gestures + 2 RC7D.20 Category Menu page jumps (L4 R0 / L0 R4).
        assertEquals(9, GuidedTouchNavigationSpec.panelGestures.size)
        assertTrue(
            GuidedTouchNavigationSpec.panelGestures.contains(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )
        )
        assertTrue(
            GuidedTouchNavigationSpec.panelGestures.contains(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )
        )
        GuidedTouchNavigationSpec.panelGestures.forEach { (left, right) ->
            assertTrue(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right))
        }
        GuidedTouchNavigationSpec.adjustmentPanelGestures.forEach { (left, right) ->
            assertTrue(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right))
        }
    }

    @Test
    fun emergencyStillWorksGloballyDuringAdjustment() {
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS, adjustResponseTimeState())
        )
    }

    @Test
    fun categoryRows_showDirectShortcutSequences() {
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots())
    }

    @Test
    fun categoryMenu_scrollHighlightSelectStillWorksAsFallback() {
        val movedDown = process(0, 2, categoryMenuState(selection = 0)) as GuidedSequenceResult.Navigate
        assertEquals(1, movedDown.newState.categoryMenuSelection)
        val opened = process(1, 1, categoryMenuState(selection = 3)) as GuidedSequenceResult.Navigate
        assertEquals(3, opened.newState.categoryIndex)
    }

    @Test
    fun categoryShortcuts_doNotConflictWithGlobalNavigation() {
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutsDoNotConflictWithGlobalNavigation())
    }

    @Test
    fun draftValues_clampWithinRange() {
        val low = PreferenceAdjustmentController.decreaseDraft(
            adjustResponseTimeState(draftSec = SequenceProcessingDelay.MIN_SECONDS)
        )
        assertEquals(SequenceProcessingDelay.MIN_SECONDS, low.draftResponseTimeSec)
        val high = PreferenceAdjustmentController.increaseDraft(
            adjustResponseTimeState(draftSec = SequenceProcessingDelay.MAX_SECONDS)
        )
        assertEquals(SequenceProcessingDelay.MAX_SECONDS, high.draftResponseTimeSec)
    }

    @Test
    fun preferencesShowsCurrentValues() {
        assertTrue(preferencesPhrases().contains("Current response time: ${SequenceProcessingDelay.DEFAULT_SECONDS} seconds"))
        assertTrue(preferencesPhrases().contains("Current sensitivity: 5"))
    }

    @Test
    fun previousPhrasePage_movesFromPage2To1_onStandardCategory() {
        val basicNeedsPage = pages[GuidedVocabularyCategory.BasicNeeds.ordinal]
        assertTrue("fixture needs >6 entries to have 2 phrase pages", basicNeedsPage.entries.size > 6)
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal)
            .copy(phrasePageIndex = 1)
        val result = process(
            GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state
        ) as GuidedSequenceResult.Navigate
        assertEquals(0, result.newState.phrasePageIndex)
    }

    @Test
    fun nextPhrasePage_movesFromPage1To2_onStandardCategory() {
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal)
        val result = process(
            GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state
        ) as GuidedSequenceResult.Navigate
        assertEquals(1, result.newState.phrasePageIndex)
    }

    @Test
    fun previousPhrasePage_onFirstPage_isUnmatched_notASilentNoOp() {
        // No previous page is visible/available on page 1, so the gesture must not execute at
        // all (Unmatched) rather than silently "succeeding" with an unchanged state.
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal)
        val result = process(
            GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    @Test
    fun nextPhrasePage_onLastPage_isUnmatched_notASilentNoOp() {
        // No next page is visible/available on the last page, so the gesture must not execute at
        // all (Unmatched) rather than silently "succeeding" with an unchanged state.
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal)
            .copy(phrasePageIndex = 1)
        val result = process(
            GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state
        )
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    @Test
    fun previousPhrasePage_onSinglePageCategory_isUnmatched() {
        // Preferences fits on a single page, so Previous/Next never have a visible target.
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state)
        )
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state)
        )
    }

    @Test
    fun categoryMenu_previousAtFirstSelection_isUnmatched() {
        val state = categoryMenuState(selection = 0)
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state)
        )
    }

    @Test
    fun categoryMenu_nextAtLastSelection_isUnmatched() {
        val state = categoryMenuState(selection = GuidedVocabularyCategory.PAGE_COUNT - 1)
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state)
        )
    }

    @Test
    fun categoryMenu_previousAndNext_stillWorkWhenAvailable() {
        val movedUp = process(
            GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, categoryMenuState(selection = 1)
        ) as GuidedSequenceResult.Navigate
        assertEquals(0, movedUp.newState.categoryMenuSelection)
        val movedDown = process(
            GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, categoryMenuState(selection = 0)
        ) as GuidedSequenceResult.Navigate
        assertEquals(1, movedDown.newState.categoryMenuSelection)
    }

    @Test
    fun hiddenPhraseRowGesture_doesNotExecute_onVisibleFirstPage() {
        // A gesture belonging only to an entry on phrase page 2 must not resolve while page 1 is
        // showing, even though the gesture code itself is a valid vocabulary gesture in general.
        val basicNeedsPage = pages[GuidedVocabularyCategory.BasicNeeds.ordinal]
        assertTrue("fixture needs >6 entries to have 2 phrase pages", basicNeedsPage.entries.size > 6)
        val hiddenEntry = basicNeedsPage.entries[GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP]
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal)
            .copy(phrasePageIndex = 0)
        val result = process(hiddenEntry.left, hiddenEntry.right, state)
        assertEquals(GuidedSequenceResult.Unmatched, result)
    }

    @Test
    fun visiblePhraseRowGesture_stillExecutes_onItsOwnPage() {
        val basicNeedsPage = pages[GuidedVocabularyCategory.BasicNeeds.ordinal]
        val visibleEntry = basicNeedsPage.entries[GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP]
        val phrasePageIndex = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP / GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
        val state = vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal)
            .copy(phrasePageIndex = phrasePageIndex)
        val result = process(visibleEntry.left, visibleEntry.right, state)
        assertTrue(result is GuidedSequenceResult.Speak || result is GuidedSequenceResult.SystemAction)
    }

    @Test
    fun emergencyCheck_precedesAllVisibilityGating_inMainActivity() {
        // Emergency is deliberately dispatched before any Guided Communication visibility gating
        // (phrase page, category menu, overlay-active) in MainActivity.finalizeSequence, so it is
        // never blocked by hidden/off-screen navigation state — the one gesture that's always
        // "visible" regardless of screen. This is a real runtime-ordering guarantee, not just a
        // Navigation Panel label, so it's asserted against the actual source rather than the
        // (necessarily Unmatched-at-this-layer) GuidedNavigationController result below.
        val main = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main != null)
        val finalizeSequenceBody = main!!.substringAfter("private fun finalizeSequence()")
        val emergencyIndex = finalizeSequenceBody.indexOf("isEmergencySequence(capturedLeft, capturedRight)")
        val overlayGateIndex = finalizeSequenceBody.indexOf("GestureRoutingTarget.GuidedOverlay")
        assertTrue("expected isEmergencySequence check in finalizeSequence", emergencyIndex >= 0)
        assertTrue("expected guided overlay routing in finalizeSequence", overlayGateIndex >= 0)
        assertTrue("emergency must be checked before guided overlay routing", emergencyIndex < overlayGateIndex)
    }

    @Test
    fun emergencyShapedGesture_safelyUnmatchedAtControllerLevel_neverMisfiresAsVocabulary() {
        // By the time a gesture reaches GuidedNavigationController it has already failed the
        // earlier, unconditional isEmergencySequence check in MainActivity — so here it must be a
        // safe Unmatched, never accidentally resolved as some unrelated visible phrase/category.
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS,
                vocabularyState(categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal).copy(phrasePageIndex = 1)
            )
        )
        assertEquals(
            GuidedSequenceResult.Unmatched,
            process(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS, categoryMenuState(selection = 2))
        )
    }

    @Test
    fun scrollHint_null_whenOnlyOnePhrasePage() {
        assertEquals(null, uiStrings.guidedPhrasePageScrollHint(0, 1))
    }

    @Test
    fun scrollHint_directsDown_onFirstOfMultiplePages() {
        assertEquals("Scroll down for more phrases", uiStrings.guidedPhrasePageScrollHint(0, 3))
    }

    @Test
    fun scrollHint_directsUpOrDown_onMiddlePage() {
        assertEquals("Scroll up or down for more phrases", uiStrings.guidedPhrasePageScrollHint(1, 3))
    }

    @Test
    fun scrollHint_directsUp_onLastPage() {
        assertEquals("Scroll up for previous phrases", uiStrings.guidedPhrasePageScrollHint(2, 3))
    }

    @Test
    fun scrollHint_directsUp_onLastOfTwoPages() {
        assertEquals("Scroll up for previous phrases", uiStrings.guidedPhrasePageScrollHint(1, 2))
    }

    @Test
    fun emergencyNavHint_isSingleSided_andMatchesRuntimeGesture() {
        val hint = uiStrings.guidedEmergencyNavHint
        assertEquals("$EMERGENCY_LEFT_WINKS Left Winks", hint)
        assertFalse(hint.contains("+"))
    }

    @Test
    fun categoriesNavHint_isSingleSided_andMatchesRuntimeGesture() {
        val hint = uiStrings.guidedCategoriesNavHint
        assertEquals("${GuidedModeNavigation.CATEGORIES_LEFT} Left Winks", hint)
        assertFalse(hint.contains("+"))
    }

    @Test
    fun emergencyAwaitingConfirmMessage_mentionsConfirmAndCancelGestures() {
        val message = uiStrings.guidedEmergencyAwaitingConfirmMessage
        assertTrue(message.contains("left then right", ignoreCase = true))
        assertTrue(message.contains("right then left", ignoreCase = true))
    }
}
