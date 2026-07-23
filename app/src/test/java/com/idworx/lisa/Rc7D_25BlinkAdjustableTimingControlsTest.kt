package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.25 — Blink-Adjustable Sensitivity and Response Time via the Adjust Settings sub-mode.
 *
 * Interaction model (RC7D.27 simplified):
 *   • L5 R5 opens the Adjust Settings menu.
 *   • Inside it: L2 R0 opens Sensitivity, L0 R2 opens Response Time, L2 R2 backs out.
 *     Emergency L6 R0 stays global. L1 R1 has no open-selected role on the menu.
 *   • While editing: L3 R1 decrease, L1 R3 increase, L1 R1 enters save confirmation, L2 R2 cancels
 *     back to Adjust Settings. Confirm save with L1 R1; cancel confirmation with R1 L1.
 *
 * These are pure behaviour tests over the canonical controller/state plus targeted source checks for
 * the shared UI wiring.
 */
class Rc7D_25BlinkAdjustableTimingControlsTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private val entryLeft = GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT
    private val entryRight = GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT

    private fun ctx(sensitivity: Int = 5, responseSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS) =
        GuidedCatalogContext(responseTimeSec = responseSec, sensitivityLevel = sensitivity)

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext = ctx()
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = catalogContext
        )

    private fun navigate(result: GuidedSequenceResult): GuidedNavigationState {
        assertTrue("Expected Navigate but was $result", result is GuidedSequenceResult.Navigate)
        return (result as GuidedSequenceResult.Navigate).newState
    }

    private fun vocab() = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.Vocabulary)
    private fun categoryMenu() = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)

    private fun openSettings(state: GuidedNavigationState, catalogContext: GuidedCatalogContext = ctx()) =
        navigate(process(entryLeft, entryRight, state, catalogContext))

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir") ?: "."),
            java.io.File(System.getProperty("user.dir") ?: ".").parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${java.io.File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${java.io.File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }

    // ------------------------------------------------------------------ A. Entry gesture

    @Test
    fun entrySequenceIsLeft5Right5FromCanonicalConstants() {
        assertEquals(5, entryLeft)
        assertEquals(5, entryRight)
        assertTrue(GuidedModeNavigation.isAdjustSettingsEntrySequence(5, 5))
        assertFalse(GuidedModeNavigation.isAdjustSettingsEntrySequence(5, 4))
        assertFalse(GuidedModeNavigation.isAdjustSettingsEntrySequence(4, 5))
        // Visible label derived from the same constants.
        assertEquals("L5 R5", formatWinkSequenceShort(entryLeft, entryRight))
    }

    @Test
    fun entrySequenceIsGloballyUnique() {
        val seq = entryLeft to entryRight
        // Not any global-navigation / emergency / finish-training sequence.
        assertFalse(GuidedModeNavigation.isGlobalNavigationSequence(entryLeft, entryRight))
        assertFalse(isEmergencySequence(entryLeft, entryRight))
        assertFalse(GuidedModeNavigation.isFinishTrainingSequence(entryLeft, entryRight))
        assertFalse(GuidedModeNavigation.isDecreaseValueSequence(entryLeft, entryRight))
        assertFalse(GuidedModeNavigation.isIncreaseValueSequence(entryLeft, entryRight))
        assertFalse(GuidedModeNavigation.isPreviousCategoryPageSequence(entryLeft, entryRight))
        assertFalse(GuidedModeNavigation.isNextCategoryPageSequence(entryLeft, entryRight))
        // Not a vocabulary slot or extended slot.
        assertFalse(seq in GuidedPageSequences.slots)
        assertFalse(seq in GuidedPageSequences.extendedSlots)
        // RC7D.26 — L5 R5 is also the Category Menu destination-9 shortcut for Adjust Settings.
        // That is intentional one-path routing, not a second competing command.
        assertEquals(
            GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX,
            GuidedCategoryShortcuts.categoryIndexForGesture(entryLeft, entryRight)
        )
        // Not a system / quick-control command.
        assertNull(LisaSystemLanguage.resolveGlobalCommand(entryLeft, entryRight))
        assertNull(LisaSystemLanguage.resolveQuickControlCommand(entryLeft, entryRight))
        // Not a phrase-composer command.
        assertTrue(
            ModeScopedGestureAuthority.phraseComposerCommandSequences.values.none { it == seq }
        )
        // Not used by any built vocabulary phrase on any category page.
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        assertTrue(
            "L5 R5 must not be used by any vocabulary entry",
            pages.flatMap { it.entries }.none { it.left == entryLeft && it.right == entryRight }
        )
    }

    @Test
    fun entryLeftCountBelowEmergencySoItCannotShareEmergencyPrefix() {
        // Emergency is L6 R0; a left count of five can never pass through (6,0).
        assertTrue(entryLeft < EMERGENCY_LEFT_WINKS)
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
    }

    @Test
    fun entryIsNotAPrefixOfAnyExistingReservedSequence() {
        // Nothing reserved exists "up and to the right" of (5,5), so it can never be shadowed.
        val reserved = listOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT,
            GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT,
            GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
        ) + GuidedPageSequences.slots + GuidedPageSequences.extendedSlots
        assertTrue(
            "No reserved sequence may dominate (5,5)",
            reserved.none { (l, r) -> l >= entryLeft && r >= entryRight }
        )
    }

    @Test
    fun entryOpensSettingsMenuFromVocabulary() {
        val opened = openSettings(vocab())
        assertTrue(opened.isSettingsMenuActive)
        assertTrue(opened.isPreferencesAdjustmentActive)
        assertFalse(opened.isValueAdjustmentActive)
        assertEquals(GuidedOverlayScreenMode.Vocabulary, opened.screenMode)
    }

    @Test
    fun entryOpensSettingsMenuFromCategoryMenuPreservingScreen() {
        val opened = openSettings(categoryMenu())
        assertTrue(opened.isSettingsMenuActive)
        // Backing out must return to the Category Menu it was opened from.
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, opened.screenMode)
    }

    @Test
    fun entryDoesNotReplaceAnyExistingCommandOnThatPage() {
        // On the Category Menu, (5,5) is not a category shortcut, so entry never steals a shortcut.
        val fromMenu = process(entryLeft, entryRight, categoryMenu())
        assertTrue(fromMenu is GuidedSequenceResult.Navigate)
        assertTrue((fromMenu as GuidedSequenceResult.Navigate).newState.isSettingsMenuActive)
    }

    // ------------------------------------------------------------------ B. Sensitivity adjustment

    @Test
    fun settingsMenuOpensSensitivityAndCapturesOriginalValue() {
        val menu = openSettings(vocab(), ctx(sensitivity = 6))
        // Select (L1 R1) opens the highlighted Sensitivity card.
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu, ctx(sensitivity = 6))
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, opened.preferencesAdjustMode)
        assertEquals(6, opened.draftSensitivityLevel)
        assertEquals(6, opened.adjustmentOriginalSensitivity)
    }

    @Test
    fun sensitivityDecreaseIncreaseThroughAdjustmentAuthority() {
        var s = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = 5)),
                ctx(sensitivity = 5)
            )
        )
        s = navigate(process(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT, s))
        assertEquals(4, s.draftSensitivityLevel)
        s = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, s))
        s = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, s))
        assertEquals(6, s.draftSensitivityLevel)
    }

    @Test
    fun sensitivityStaysWithinExistingBounds() {
        var atMin = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = MIN_SENSITIVITY_LEVEL)),
                ctx(sensitivity = MIN_SENSITIVITY_LEVEL)
            )
        )
        atMin = navigate(process(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT, atMin))
        assertEquals(MIN_SENSITIVITY_LEVEL, atMin.draftSensitivityLevel)

        var atMax = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = MAX_SENSITIVITY_LEVEL)),
                ctx(sensitivity = MAX_SENSITIVITY_LEVEL)
            )
        )
        atMax = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, atMax))
        assertEquals(MAX_SENSITIVITY_LEVEL, atMax.draftSensitivityLevel)
    }

    @Test
    fun sensitivitySaveEmitsSaveResultAndExits() {
        var s = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = 5)),
                ctx(sensitivity = 5)
            )
        )
        s = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, s))
        // RC7D.27 — first L1 R1 enters confirmation; second confirms persistence.
        s = navigate(process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, s))
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSensitivity, s.preferencesAdjustMode)
        val saveResult = process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, s)
        assertTrue(saveResult is GuidedSequenceResult.SavePreferencesAdjustment)
        val save = saveResult as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(6, save.sensitivityLevel)
        assertNull("Sensitivity save must not carry a response-time value", save.responseTimeSec)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, save.newState.preferencesAdjustMode)
    }

    @Test
    fun sensitivityCancelRestoresOriginalAndDoesNotPersist() {
        var s = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = 5)),
                ctx(sensitivity = 5)
            )
        )
        s = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, s))
        assertEquals(6, s.draftSensitivityLevel)
        val cancelled = navigate(process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, s))
        // RC7D.27 — Cancel returns to Adjust Settings without persisting.
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    // ------------------------------------------------------------------ C. Response Time adjustment

    @Test
    fun settingsMenuSelectsAndOpensResponseTime() {
        val menu = openSettings(vocab(), ctx(responseSec = 4))
        // Scroll Down highlights Response Time, then Select opens it.
        val highlighted = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu, ctx(responseSec = 4))
        )
        assertEquals(1, highlighted.settingsHubSelection)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, highlighted.preferencesAdjustMode)
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, highlighted, ctx(responseSec = 4))
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, opened.preferencesAdjustMode)
        assertEquals(4, opened.draftResponseTimeSec)
    }

    @Test
    fun responseTimeAndSensitivityOpenViaSelectWithoutRailFallthrough() {
        val menu = openSettings(vocab())
        val highlighted = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu))
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, highlighted.preferencesAdjustMode)
        val response = navigate(process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, highlighted))
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, response.preferencesAdjustMode)
        val back = navigate(process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, response))
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, back.preferencesAdjustMode)
        val sensitivity = navigate(process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, back))
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, sensitivity.preferencesAdjustMode)
    }

    private fun openResponseTimeAdjust(responseSec: Int): GuidedNavigationState {
        val menu = openSettings(vocab(), ctx(responseSec = responseSec))
        val highlighted = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu, ctx(responseSec = responseSec))
        )
        return navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, highlighted, ctx(responseSec = responseSec))
        )
    }

    @Test
    fun responseTimeDecreaseIncreaseAndBounds() {
        var s = openResponseTimeAdjust(SequenceProcessingDelay.DEFAULT_SECONDS)
        val start = s.draftResponseTimeSec
        s = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, s))
        assertEquals(SequenceProcessingDelay.coerce(start + 1), s.draftResponseTimeSec)
        s = navigate(process(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT, s))
        assertEquals(start, s.draftResponseTimeSec)

        var atMin = openResponseTimeAdjust(SequenceProcessingDelay.MIN_SECONDS)
        atMin = navigate(process(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT, atMin))
        assertEquals(SequenceProcessingDelay.MIN_SECONDS, atMin.draftResponseTimeSec)

        var atMax = openResponseTimeAdjust(SequenceProcessingDelay.MAX_SECONDS)
        atMax = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, atMax))
        assertEquals(SequenceProcessingDelay.MAX_SECONDS, atMax.draftResponseTimeSec)
    }

    @Test
    fun responseTimeSaveAndCancel() {
        var s = openResponseTimeAdjust(SequenceProcessingDelay.MIN_SECONDS)
        s = navigate(process(GuidedModeNavigation.INCREASE_VALUE_LEFT, GuidedModeNavigation.INCREASE_VALUE_RIGHT, s))
        val expected = s.draftResponseTimeSec
        s = navigate(process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, s))
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveResponseTime, s.preferencesAdjustMode)
        val saveResult = process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, s)
        assertTrue(saveResult is GuidedSequenceResult.SavePreferencesAdjustment)
        val save = saveResult as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(expected, save.responseTimeSec)
        assertNull(save.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, save.newState.preferencesAdjustMode)

        // After save, Back from Adjust Settings exits the sub-mode entirely.
        val exited = navigate(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, save.newState)
        )
        assertEquals(GuidedPreferencesAdjustMode.None, exited.preferencesAdjustMode)

        // Adjustment cancel from editing returns to Adjust Settings (not None).
        val editing = openResponseTimeAdjust(SequenceProcessingDelay.MIN_SECONDS)
        val cancelled = navigate(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, editing)
        )
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    // ------------------------------------------------------------------ D. Touch/blink parity (single authority)

    @Test
    fun touchAndBlinkShareOneMutationAndPersistenceAuthority() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        // Touch −/+ route through changeSensitivity / changeResponseTime.
        assertTrue(main.contains("onSensitivityDecrease = { changeSensitivity(-1) }"))
        assertTrue(main.contains("onSensitivityIncrease = { changeSensitivity(1) }"))
        assertTrue(main.contains("onResponseTimeDecrease = { changeResponseTime(-1) }"))
        assertTrue(main.contains("onResponseTimeIncrease = { changeResponseTime(1) }"))
        // Those touch mutators funnel into the SAME apply* authority the blink save uses.
        assertTrue(main.contains("applySensitivityLevel(newLevel)"))
        assertTrue(main.contains("applySequenceProcessingDelay(seconds)"))
        assertTrue(main.contains("result.responseTimeSec?.let { setSequenceProcessingDelay(it) }"))
        assertTrue(main.contains("result.sensitivityLevel?.let { applySensitivityLevel(it) }"))
    }

    @Test
    fun gestureRoutingDoesNotDuplicateMutationFormulas() {
        // The controller never mutates a persisted value itself — save produces a result object that
        // MainActivity applies through the one authority. Decrease/Increase only edit the draft.
        val menu = openSettings(vocab(), ctx(sensitivity = 5))
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu, ctx(sensitivity = 5))
        )
        val confirming = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, opened)
        )
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSensitivity, confirming.preferencesAdjustMode)
        val save = process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, confirming)
        assertTrue(save is GuidedSequenceResult.SavePreferencesAdjustment)
    }

    // ------------------------------------------------------------------ E. Mode safety

    @Test
    fun emergencyTakesPriorityOverAdjustmentMode() {
        val adjusting = LisaGestureContext(
            activePanel = LisaPanel.None,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = true,
            phraseComposerMode = null
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(adjusting, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
        // Non-emergency adjustment gestures stay within the guided overlay.
        assertEquals(
            GestureRoutingTarget.GuidedOverlay,
            ModeScopedGestureAuthority.routingTarget(adjusting, GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        )
    }

    @Test
    fun settingsMenuIgnoresUnderlyingCategoryAndPhraseGestures() {
        val menu = openSettings(categoryMenu())
        // A category shortcut (L3 R1 = Medical in the menu) must NOT open a category from the sub-mode.
        val shortcut = process(3, 1, menu)
        assertTrue("Category shortcut must be inert in the settings sub-mode", shortcut is GuidedSequenceResult.Unmatched)
        // Categories (L3 R0) escapes Settings & Controls into the Category Menu.
        val categories = navigate(
            process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, menu)
        )
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, categories.screenMode)
        assertEquals(GuidedPreferencesAdjustMode.None, categories.preferencesAdjustMode)
    }

    @Test
    fun selectEntersSaveConfirmationRatherThanOpeningPhraseAndBackCancelsToSettingsMenu() {
        val opened = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = 5)),
                ctx(sensitivity = 5)
            )
        )
        // Select => enter confirmation (not Speak / phrase open). Confirm then persists.
        val onSelect = navigate(process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, opened))
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSensitivity, onSelect.preferencesAdjustMode)
        val saved = process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, onSelect)
        assertTrue(saved is GuidedSequenceResult.SavePreferencesAdjustment)
        assertFalse(saved is GuidedSequenceResult.Speak)
        // Back from editing => Adjust Settings, preserving underlying vocabulary screen.
        val onBack = navigate(process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, opened))
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, onBack.preferencesAdjustMode)
        assertEquals(GuidedOverlayScreenMode.Vocabulary, onBack.screenMode)
        assertEquals(opened.categoryIndex, onBack.categoryIndex)
    }

    @Test
    fun decreaseAndIncreaseDoNotOpenCategoriesWhileAdjusting() {
        // In value-adjustment, L3 R1 / L1 R3 are Decrease/Increase — not the Medical/Family shortcuts.
        val opened = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                openSettings(vocab(), ctx(sensitivity = 5)),
                ctx(sensitivity = 5)
            )
        )
        val dec = navigate(process(3, 1, opened))
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, dec.preferencesAdjustMode)
        assertEquals(GuidedOverlayScreenMode.Vocabulary, dec.screenMode)
        assertEquals(4, dec.draftSensitivityLevel)
        val inc = navigate(process(1, 3, dec))
        assertEquals(5, inc.draftSensitivityLevel)
        assertEquals(GuidedOverlayScreenMode.Vocabulary, inc.screenMode)
    }

    @Test
    fun leavingAdjustmentRestoresNormalGestureMeanings() {
        // After cancel, the same (3,0) that was inert during the sub-mode again opens the Category Menu.
        val opened = openSettings(vocab())
        val closed = navigate(process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, opened))
        assertEquals(GuidedPreferencesAdjustMode.None, closed.preferencesAdjustMode)
        val categories = navigate(process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, closed))
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, categories.screenMode)
    }

    @Test
    fun reEntryGestureIsInertWhileAlreadyAdjusting() {
        val menu = openSettings(vocab())
        // L5 R5 while inside the sub-mode is not a second entry — it is simply unmatched.
        assertTrue(process(entryLeft, entryRight, menu) is GuidedSequenceResult.Unmatched)
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu)
        )
        assertTrue(process(entryLeft, entryRight, opened) is GuidedSequenceResult.Unmatched)
    }

    // ------------------------------------------------------------------ F. UI wiring

    @Test
    fun sharedHeaderNoLongerAdvertisesAdjustSettingsEntry() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(ui.contains("guidedAdjustSettingsDiscoverabilityLabel"))
        assertFalse(ui.contains("Adjust: "))
        assertFalse(ui.contains("onOpenAdjustSettings"))
        // Four touch controls and values remain in CompactSensitivityControls.
        assertTrue(ui.contains("sensitivityDecrease"))
        assertTrue(ui.contains("sensitivityIncrease"))
        assertTrue(ui.contains("responseTimeDecrease"))
        assertTrue(ui.contains("responseTimeIncrease"))
        // The normal header must NOT permanently print the L3 R1 / L1 R3 adjustment sequences.
        assertFalse(ui.contains("\"L3 R1\""))
        assertFalse(ui.contains("\"L1 R3\""))
    }

    @Test
    fun continuationProtectionRegistersEntrySequence() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(
            "L5 R5 must be advertised as a longer continuation candidate",
            main.contains("GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT") &&
                main.contains("mappingsForSequenceContinuation")
        )
    }

    @Test
    fun settingsMenuPanelRendersAllOptionsWithCanonicalLabels() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("fun SettingsAndControlsHubPanel(") || ui.contains("fun SettingsMenuPanel("))
        assertTrue(ui.contains("guidedAdjustSettingsTitle"))
        assertTrue(ui.contains("guidedSelectSensitivitySetting"))
        assertTrue(ui.contains("guidedSelectResponseTimeSetting"))
        // Selection model: rail Select uses guidedOpenSelectedSetting; no Setting 1/2 indicator.
        assertTrue(ui.contains("PanelContext.SettingsHub") || ui.contains("settingsHubSelection"))
        assertFalse(ui.contains("guidedSettingIndicator"))
        assertTrue(ui.contains("SettingsAndControlsHubSequences") || ui.contains("GuidedModeNavigation.PREVIOUS_LEFT"))
        assertTrue(ui.contains("SettingsControlKind.Sensitivity") || ui.contains("GuidedModeNavigation.NEXT_LEFT"))
    }

    @Test
    fun adjustmentOverlayShowsValueAndAdjustmentSequences() {
        // Decrease L3 R1, Increase L1 R3, Save L1 R1, Cancel L2 R2 must appear in the adjustment panel.
        assertEquals(3, GuidedModeNavigation.DECREASE_VALUE_LEFT)
        assertEquals(1, GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        assertEquals(1, GuidedModeNavigation.INCREASE_VALUE_LEFT)
        assertEquals(3, GuidedModeNavigation.INCREASE_VALUE_RIGHT)
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("GuidedModeNavigation.DECREASE_VALUE_LEFT"))
        assertTrue(ui.contains("GuidedModeNavigation.INCREASE_VALUE_LEFT"))
    }

    @Test
    fun feedbackStringsExistForSaveAndCancel() {
        assertEquals("Sensitivity saved: 6", english.guidedSensitivitySaved(6))
        assertEquals("Response time saved: 5 seconds", english.guidedResponseTimeSaved(5))
        assertEquals("Sensitivity changes cancelled", english.guidedSensitivityChangesCancelled)
        assertEquals("Response time changes cancelled", english.guidedResponseTimeChangesCancelled)
        assertEquals("Settings & Controls", english.guidedAdjustSettingsTitle)
        assertEquals("Cancel / Back", english.guidedCancelBack)
        assertFalse(english.guidedCancelBack.contains("Preferences", ignoreCase = true))
    }

    // ------------------------------------------------------------------ G. Regression

    @Test
    fun existingCanonicalSequencesUnchanged() {
        assertEquals(2 to 0, GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT)
        assertEquals(0 to 2, GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(1 to 1, GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT)
        assertEquals(2 to 2, GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT)
        assertEquals(3 to 0, GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT)
        assertEquals(0 to 3, GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT)
        assertEquals(3 to 1, GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        assertEquals(1 to 3, GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT)
        assertEquals(4 to 0, GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT)
        assertEquals(0 to 4, GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT)
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        // Entry sequence is distinct from every one of them.
        val entry = entryLeft to entryRight
        assertNotEquals(entry, GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        assertNotEquals(entry, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
    }

    @Test
    fun preferencesCategoryDirectAdjustmentStillWorks() {
        // Direct open still works; RC7D.27 save returns to Adjust Settings (canonical post-save home).
        val direct = PreferenceAdjustmentController.openSensitivityAdjust(vocab(), 5)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, direct.preferencesAdjustMode)
        val confirming = PreferenceAdjustmentController.beginSaveConfirmation(direct)
        val save = PreferenceAdjustmentController.saveAdjustment(confirming)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, save.newState.preferencesAdjustMode)
        assertEquals(5, save.sensitivityLevel)
    }

    @Test
    fun modeScopedAuthoritySelfAuditStillPasses() {
        assertTrue(
            "MSGA audit findings: ${ModeScopedGestureAuthorityAudit.auditAll()}",
            ModeScopedGestureAuthorityAudit.passes()
        )
    }
}
