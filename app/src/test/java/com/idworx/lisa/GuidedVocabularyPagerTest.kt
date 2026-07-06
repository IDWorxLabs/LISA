package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedVocabularyPagerTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
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
            draftResponseTimeSec = 3,
            draftSensitivityLevel = 5
        )

    private fun categoryMenuState(selection: Int = 0): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryIndex = 0,
            categoryMenuSelection = selection
        )

    private fun adjustResponseTimeState(draftSec: Int = 3): GuidedNavigationState =
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
        assertEquals(3, result.newState.draftResponseTimeSec)
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
        assertEquals(GuidedPreferencesAdjustMode.None, cancelled.preferencesAdjustMode)
    }

    @Test
    fun save_persistsDraftValue() {
        val result = process(1, 1, adjustResponseTimeState(draftSec = 5)) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, result.responseTimeSec)
        assertEquals(GuidedPreferencesAdjustMode.None, result.newState.preferencesAdjustMode)
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
        val result = process(1, 1, adjustResponseTimeState(draftSec = 5)) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, result.responseTimeSec)
    }

    @Test
    fun responseTime_L2R2_cancelsWithoutSaving() {
        val result = process(2, 2, adjustResponseTimeState(draftSec = 6)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.None, result.newState.preferencesAdjustMode)
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
        val result = process(1, 1, adjustSensitivityState(draftLevel = 7)) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(7, result.sensitivityLevel)
    }

    @Test
    fun sensitivity_L2R2_cancelsWithoutSaving() {
        val result = process(2, 2, adjustSensitivityState(draftLevel = 8)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.None, result.newState.preferencesAdjustMode)
    }

    @Test
    fun adjustmentMode_scrollGesturesRemainForOverflow() {
        val scrolled = process(0, 2, adjustResponseTimeState()) as GuidedSequenceResult.Navigate
        assertNotEquals(0, scrolled.newState.adjustmentScrollStep)
        val scrolledUp = process(2, 0, scrolled.newState) as GuidedSequenceResult.Navigate
        assertEquals(0, scrolledUp.newState.adjustmentScrollStep)
    }

    @Test
    fun L4R4_opensCategoryMenuFromVocabularyMode() {
        val result = process(4, 4, vocabularyState(categoryIndex = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
    }

    @Test
    fun L4R4_opensCategoryMenuFromPreferencesPage() {
        val result = process(4, 4, vocabularyState(categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
    }

    @Test
    fun L4R4_opensCategoryMenuFromResponseTimeAdjustment() {
        val result = process(4, 4, adjustResponseTimeState()) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
        assertEquals(GuidedPreferencesAdjustMode.None, result.newState.preferencesAdjustMode)
    }

    @Test
    fun L4R4_opensCategoryMenuFromSensitivityAdjustment() {
        val result = process(4, 4, adjustSensitivityState()) as GuidedSequenceResult.Navigate
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
        assertEquals("L4 R4", actions[3].sequenceLabel)
        assertEquals("Categories", actions[3].title)
        assertTrue(GuidedNavigationPanelSpec.allActionsLabeled(actions))
    }

    @Test
    fun allPanelGesturesAreTappableViaTouchSpec() {
        assertEquals(6, GuidedTouchNavigationSpec.panelGestures.size)
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
        val low = PreferenceAdjustmentController.decreaseDraft(adjustResponseTimeState(draftSec = 1))
        assertEquals(1, low.draftResponseTimeSec)
        val high = PreferenceAdjustmentController.increaseDraft(adjustResponseTimeState(draftSec = 6))
        assertEquals(6, high.draftResponseTimeSec)
    }

    @Test
    fun preferencesShowsCurrentValues() {
        assertTrue(preferencesPhrases().contains("Current response time: 3 seconds"))
        assertTrue(preferencesPhrases().contains("Current sensitivity: 5"))
    }
}
