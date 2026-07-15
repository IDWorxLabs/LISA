package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.23 — Category Menu Label Cleanup and Emergency Sequence Visibility.
 *
 * Three focused corrections, all validated here:
 *   1. The duplicated Category Menu heading is removed — only the content "Choose a Category"
 *      heading remains; the header no longer repeats the near-identical "Choose Category" mode
 *      label. The Communication context label and the Category X / Y + Page X / Y indicators stay.
 *   2. Category 7's caregiver-facing label is renamed "Custom" -> "Customize Phrases" through the
 *      canonical string source only; the internal enum identity, ordering, shortcut (L3 R3) and
 *      persistence are unchanged.
 *   3. The Customize Phrases page (the phrase composer opened for the Custom category, RC7D.1) shows
 *      the canonical Emergency sequence "L6 R0" derived from EMERGENCY_LEFT_WINKS/RIGHT_WINKS.
 */
class Rc7D_23CategoryMenuLabelCleanupTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir")),
            java.io.File(System.getProperty("user.dir")).parentFile
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

    private fun extractBlock(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Expected $signature", start >= 0)
        val openBrace = source.indexOf('{', start)
        var depth = 0
        for (index in openBrace until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        error("Unterminated block: $signature")
    }

    private val guidedModeUi: String get() =
        readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
    private val overlayHeader: String get() =
        extractBlock(guidedModeUi, "private fun GuidedOverlayHeader(")
    private val emergencyCard: String get() =
        extractBlock(
            readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt"),
            "fun ComposerEmergencyCommandCard("
        )

    private fun composerKeyboardContext() = LisaGestureContext(
        activePanel = LisaPanel.PhraseEditor,
        guidedOverlayActive = false,
        guidedScreenMode = null,
        isAdjustingPreference = false,
        phraseComposerMode = PhraseComposerMode.Keyboard,
        emergencyModalActive = false
    )

    // ==================================================================================
    // A. DUPLICATE-HEADING REMOVAL
    // ==================================================================================

    @Test // A1 — Category Menu renders "Choose a Category"
    fun categoryMenuRendersChooseACategory() {
        assertEquals("Choose a Category", english.guidedCategoryMenuTitle)
        assertTrue(guidedModeUi.contains("text = uiStrings.guidedCategoryMenuTitle"))
    }

    @Test // A2 — Category Menu does not render the separate "Choose Category" heading
    fun categoryMenuDoesNotRenderSeparateChooseCategoryHeading() {
        // The header suppresses its mode label for the Category Menu (that was the duplicate).
        assertTrue(overlayHeader.contains("val showModeLabel"))
        assertTrue(overlayHeader.contains("screenMode != GuidedOverlayScreenMode.CategoryMenu"))
        assertTrue(overlayHeader.contains("if (showModeLabel) {"))
        // The only mode-label Text is the one guarded by showModeLabel.
        assertTrue(overlayHeader.contains("text = modeLabel"))
    }

    @Test // A3 — only one caregiver-facing category-choice heading remains
    fun onlyOneCategoryChoiceHeadingRemains() {
        // The two strings are distinct; the header's "Choose Category" mode label is hidden for the
        // Category Menu, leaving the content "Choose a Category" as the single heading.
        assertEquals("Choose Category", english.guidedCategoryMenuMode)
        assertEquals("Choose a Category", english.guidedCategoryMenuTitle)
        val label = overlayHeader.substringAfter("if (showModeLabel) {")
        // "Choose a Category" is not printed by the header at all (it lives in the content).
        assertFalse(overlayHeader.contains("guidedCategoryMenuTitle"))
        assertTrue(label.isNotBlank())
    }

    @Test // A4 — Communication context label remains
    fun communicationContextLabelRemains() {
        assertEquals("Communication", english.workspaceCommunicationTitle)
        assertTrue(overlayHeader.contains("uiStrings.workspaceCommunicationTitle"))
    }

    @Test // A5 — Category X / 8 remains
    fun categoryIndicatorRemains() {
        assertEquals("Category 1 / 8", english.guidedCategoryIndicator(1, 8))
        assertTrue(overlayHeader.contains("uiStrings.guidedCategoryIndicator"))
    }

    @Test // A6 — Page X / Y remains
    fun pageIndicatorRemains() {
        assertEquals("Page 1 / 2", english.guidedPageIndicator(1, 2))
        assertTrue(overlayHeader.contains("uiStrings.guidedPageIndicator"))
    }

    @Test // A7 — removal does not leave a deliberate duplicate spacer or placeholder
    fun removalLeavesNoPlaceholder() {
        // No else-branch inserts a spacer/placeholder where the hidden mode label used to be.
        assertFalse(overlayHeader.contains("else {"))
    }

    @Test // A8 — category list is rendered immediately below the retained heading
    fun categoryListRenderedImmediatelyBelowHeading() {
        val headingIndex = guidedModeUi.indexOf("text = uiStrings.guidedCategoryMenuTitle")
        val listIndex = guidedModeUi.indexOf("categoryMenuTitles.forEachIndexed")
        assertTrue(headingIndex >= 0)
        assertTrue(listIndex >= 0)
        assertTrue("Heading must precede the category list", headingIndex < listIndex)
    }

    // ==================================================================================
    // B. CUSTOMIZE PHRASES RENAME
    // ==================================================================================

    @Test // B1 — category 7 caregiver-facing label is exactly "Customize Phrases"
    fun category7LabelIsCustomizePhrases() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertEquals("Customize Phrases", titles[GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX])
        assertEquals(
            "Customize Phrases",
            english.guidedCategoryTitle(GuidedVocabularyCategory.Custom)
        )
    }

    @Test // B2 — "Custom" is no longer rendered as the standalone category label
    fun customNoLongerRenderedAsStandaloneLabel() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertFalse(titles.contains("Custom"))
        assertTrue(titles.contains("Customize Phrases"))
    }

    @Test // B3 — category 7 remains in the same position
    fun category7RemainsInSamePosition() {
        assertEquals(6, GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
        assertEquals(
            GuidedVocabularyCategory.Custom,
            GuidedVocabularyCategory.ordered[GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX]
        )
    }

    @Test // B4 — category 7 shortcut remains L3 R3
    fun category7ShortcutRemainsL3R3() {
        assertEquals(
            "L3 R3",
            GuidedCategoryShortcuts.sequenceLabelForCategory(
                GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX
            )
        )
    }

    @Test // B5 — internal category identity remains stable
    fun internalCategoryIdentityRemainsStable() {
        assertEquals("Custom", GuidedVocabularyCategory.Custom.name)
        assertEquals("Custom", CustomPhraseEngine.CaregiverPhraseCategory.Custom.name)
        assertEquals("Custom", CustomPhraseEngine.CaregiverPhraseCategory.Custom.storageKey())
    }

    @Test // B6 — navigation to the category still works (opens the composer, RC7D.1)
    fun navigationToCategoryStillWorks() {
        val page = GuidedVocabularyCatalog.categoryAt(
            GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX,
            PreferredLanguage.English,
            english
        )
        assertNotNull(page)
        assertEquals(GuidedVocabularyCategory.Custom, page!!.category)
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openComposeModeFromCustom"))
    }

    @Test // B7 — Phrase Management remains category 8
    fun phraseManagementRemainsCategory8() {
        assertEquals(7, GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX)
        assertEquals(
            GuidedVocabularyCategory.PhraseManagement,
            GuidedVocabularyCategory.ordered[GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX]
        )
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertEquals("Phrase Management", titles[GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX])
    }

    @Test // B8 — accessibility-facing label uses "Customize Phrases"
    fun accessibilityFacingLabelUsesCustomizePhrases() {
        assertEquals(
            "Customize Phrases",
            english.caregiverPhraseCategoryLabel(CustomPhraseEngine.CaregiverPhraseCategory.Custom)
        )
    }

    @Test // B9 — longer text is supported by the category card (wraps, not clipped)
    fun longerLabelSupportedByCategoryCard() {
        val row = extractBlock(guidedModeUi, "private fun GuidedCategoryMenuRow(")
        // The title Text takes flexible width and is never forced to a single clipped line.
        assertTrue(row.contains("text = title"))
        assertTrue(row.contains("Modifier.weight(1f)"))
        assertFalse(row.contains("maxLines = 1"))
    }

    @Test // B10 — no saved phrase or repository identifier is migrated unnecessarily
    fun noRepositoryIdentifierMigratedUnnecessarily() {
        // Display label changed, but the serialized/storage identity is untouched.
        assertEquals("Custom", CustomPhraseEngine.CaregiverPhraseCategory.Custom.storageKey())
        assertEquals(
            GuidedVocabularyCategory.Custom,
            CustomPhraseEngine.CaregiverPhraseCategory.Custom.toGuidedCategory()
        )
    }

    // ==================================================================================
    // C. EMERGENCY SEQUENCE VISIBILITY (Customize Phrases page = phrase composer)
    // ==================================================================================

    @Test // C1 — Emergency button displays "Emergency"
    fun emergencyButtonDisplaysEmergency() {
        assertEquals("Emergency", english.guidedEmergencyNavTitle)
        assertTrue(emergencyCard.contains("uiStrings.guidedEmergencyNavTitle"))
        assertTrue(emergencyCard.contains("text = title"))
    }

    @Test // C2 — it displays "L6 R0"
    fun emergencyButtonDisplaysL6R0() {
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        assertTrue(
            emergencyCard.contains("formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)")
        )
        assertTrue(emergencyCard.contains("text = sequenceLabel"))
    }

    @Test // C3 — sequence is derived from canonical emergency constants, not hardcoded
    fun emergencySequenceDerivedFromCanonicalConstants() {
        assertTrue(emergencyCard.contains("EMERGENCY_LEFT_WINKS"))
        assertTrue(emergencyCard.contains("EMERGENCY_RIGHT_WINKS"))
        assertFalse("Sequence must not be a hardcoded literal", emergencyCard.contains("\"L6 R0\""))
    }

    @Test // C4 — Emergency action remains L6 R0
    fun emergencyActionRemainsL6R0() {
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        assertTrue(isEmergencySequence(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test // C5 — touch Emergency routes to the existing emergency handler
    fun touchEmergencyRoutesToHandler() {
        // The card's click is wired straight to onEmergency, threaded from the composer overlay.
        assertTrue(emergencyCard.contains("onClick"))
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("onClick = onEmergency"))
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composer.contains("onEmergency = onEmergency"))
    }

    @Test // C6 — blink L6 R0 routes to the existing emergency handler from the composer
    fun blinkL6R0RoutesToHandler() {
        assertTrue(ModeScopedGestureAuthority.isGlobalGesture(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(
                composerKeyboardContext(),
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
    }

    @Test // C7 — emergency styling remains present
    fun emergencyStylingRemainsPresent() {
        assertTrue(emergencyCard.contains("LisaEmergencyRed"))
    }

    @Test // C8 — emergency button retains its full-width layout role
    fun emergencyButtonFullWidthRetained() {
        assertTrue(emergencyCard.contains(".fillMaxWidth()"))
    }

    @Test // C9 — sequence text is not blank or hidden
    fun sequenceTextNotBlankOrHidden() {
        assertTrue(formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS).isNotBlank())
        assertTrue(emergencyCard.contains("text = sequenceLabel"))
    }

    @Test // C10 — no alternate emergency sequence is introduced
    fun noAlternateEmergencySequenceIntroduced() {
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        for (right in 1..6) {
            assertFalse(emergencyCard.contains("\"L6 R$right\""))
        }
    }

    // ==================================================================================
    // D. REGRESSION
    // ==================================================================================

    @Test // D1 — all category navigation sequences remain unchanged
    fun categoryNavigationSequencesUnchanged() {
        assertEquals("L2 R0", formatWinkSequenceShort(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT))
        assertEquals("L0 R2", formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT))
        assertEquals(
            "L4 R0",
            formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )
        )
        assertEquals(
            "L0 R4",
            formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )
        )
        assertEquals("L1 R1", formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT))
        assertEquals("L2 R2", formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT))
        assertEquals("L3 R0", formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT))
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test // D2 — all nine navigation panel actions remain
    fun allNineNavigationPanelActionsRemain() {
        assertEquals(9, GuidedTouchNavigationSpec.panelGestures.size)
    }

    @Test // D3 — the Category Menu panel keeps its stable action kinds
    fun categoryMenuPanelActionKindsIntact() {
        val kinds = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        ).map { it.kind }
        assertEquals(
            listOf(
                GuidedPanelActionKind.ScrollUp,
                GuidedPanelActionKind.ScrollDown,
                GuidedPanelActionKind.PreviousCategoryPage,
                GuidedPanelActionKind.NextCategoryPage,
                GuidedPanelActionKind.Select,
                GuidedPanelActionKind.Back,
                GuidedPanelActionKind.Emergency
            ),
            kinds
        )
    }

    @Test // D4 — viewport page navigation remains viewport-driven (RC7D.22 preserved)
    fun viewportPageNavigationRemains() {
        assertFalse(CategoryViewportPaging.canGoToPreviousPage(0))
        assertTrue(CategoryViewportPaging.canGoToNextPage(0, 2))
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(0, 1000, 600))
        assertEquals(600, CategoryViewportPaging.pageAnchorOffsetPx(1, 1000, 600))
    }

    @Test // D5 — Customize Phrases does not replace Phrase Management
    fun customizePhrasesDoesNotReplacePhraseManagement() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertEquals("Customize Phrases", titles[GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX])
        assertEquals("Phrase Management", titles[GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX])
        assertTrue(
            CategoryAreaDestination.isManagementDestination(GuidedVocabularyCategory.PhraseManagement)
        )
        assertFalse(
            CategoryAreaDestination.isAssignableCommunicationCategory(
                GuidedVocabularyCategory.PhraseManagement
            )
        )
    }

    @Test // D6 — Open Selected Category still requires L1 R1
    fun openSelectedCategoryStillL1R1() {
        assertEquals(
            "L1 R1",
            formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)
        )
    }

    @Test // D7 — Emergency remains L6 R0
    fun emergencyRemainsL6R0() {
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
    }

    @Test // D8 — no Android system keyboard is introduced
    fun noAndroidSystemKeyboardIntroduced() {
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(composer.contains("import android.view.inputmethod"))
        assertFalse(composer.contains("SoftwareKeyboardController"))
        assertTrue(composer.contains("BottomAlignedEyeKeyboard"))
    }
}
