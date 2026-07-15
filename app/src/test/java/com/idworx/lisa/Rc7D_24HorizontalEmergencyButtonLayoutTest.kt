package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.24 — Horizontal Emergency Button Label Layout.
 *
 * The Customize Phrases composer Emergency button (`ComposerEmergencyCommandCard`) now lays its
 * content out on a single horizontal row — "Emergency" (left) 🚨 (centre) "L6 R0" (right) — instead
 * of the previous vertical stack. The change is scoped to that one composer composable; unrelated
 * Emergency buttons (e.g. the guided-mode panel button) are untouched. Sequence source, routing,
 * styling, sizing and accessibility semantics are preserved.
 */
class Rc7D_24HorizontalEmergencyButtonLayoutTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val workingDir = System.getProperty("user.dir") ?: "."
        val roots = listOfNotNull(
            java.io.File(workingDir),
            java.io.File(workingDir).parentFile
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

    private val composerGridSource: String get() =
        readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
    private val emergencyCard: String get() =
        extractBlock(composerGridSource, "fun ComposerEmergencyCommandCard(")
    private val guidedUiSource: String get() =
        readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")

    private val icon = "\uD83D\uDEA8" // 🚨

    private fun composerKeyboardContext() = LisaGestureContext(
        activePanel = LisaPanel.PhraseEditor,
        guidedOverlayActive = false,
        guidedScreenMode = null,
        isAdjustingPreference = false,
        phraseComposerMode = PhraseComposerMode.Keyboard,
        emergencyModalActive = false
    )

    @Test // 1 — Emergency label is present
    fun emergencyLabelPresent() {
        assertEquals("Emergency", english.guidedEmergencyNavTitle)
        assertTrue(emergencyCard.contains("uiStrings.guidedEmergencyNavTitle"))
        assertTrue(emergencyCard.contains("text = title"))
    }

    @Test // 2 — Emergency icon is present
    fun emergencyIconPresent() {
        assertTrue(emergencyCard.contains(icon))
    }

    @Test // 3 — L6 R0 sequence is present
    fun sequencePresent() {
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        assertTrue(emergencyCard.contains("text = sequenceLabel"))
        assertTrue(
            emergencyCard.contains("formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)")
        )
    }

    @Test // 4 — content order is label -> icon -> sequence
    fun contentOrderLabelIconSequence() {
        val labelIndex = emergencyCard.indexOf("text = title")
        val iconIndex = emergencyCard.indexOf(icon)
        val sequenceIndex = emergencyCard.indexOf("text = sequenceLabel")
        assertTrue(labelIndex >= 0)
        assertTrue(iconIndex >= 0)
        assertTrue(sequenceIndex >= 0)
        assertTrue("Label must precede icon", labelIndex < iconIndex)
        assertTrue("Icon must precede sequence", iconIndex < sequenceIndex)
    }

    @Test // 5 — layout uses a horizontal Row, not the previous vertical Column
    fun layoutUsesHorizontalRow() {
        assertTrue(emergencyCard.contains("Row("))
        assertTrue(emergencyCard.contains("verticalAlignment = Alignment.CenterVertically"))
        assertFalse(emergencyCard.contains("Column("))
        assertFalse(emergencyCard.contains("horizontalAlignment = Alignment.CenterHorizontally"))
        assertFalse(emergencyCard.contains("verticalArrangement = Arrangement.Center"))
    }

    @Test // 6 — left and right text areas use balanced weighted layout
    fun leftAndRightTextAreasBalanced() {
        val weightCount = Regex("Modifier\\.weight\\(1f\\)").findAll(emergencyCard).count()
        assertEquals("Two balanced weighted text areas", 2, weightCount)
        assertTrue(emergencyCard.contains("textAlign = TextAlign.End"))
        assertTrue(emergencyCard.contains("textAlign = TextAlign.Start"))
    }

    @Test // 7 — icon remains centrally positioned between the balanced text areas
    fun iconRemainsCentred() {
        val firstWeight = emergencyCard.indexOf("Modifier.weight(1f)")
        val lastWeight = emergencyCard.lastIndexOf("Modifier.weight(1f)")
        val iconIndex = emergencyCard.indexOf(icon)
        assertTrue(firstWeight in 0 until iconIndex)
        assertTrue(iconIndex < lastWeight)
        // Equal spacers on both sides of the icon keep it centred in the full-width button.
        val spacerCount = Regex("Spacer\\(Modifier\\.width").findAll(emergencyCard).count()
        assertEquals(2, spacerCount)
    }

    @Test // 8 — sequence derived from canonical constants
    fun sequenceDerivedFromCanonicalConstants() {
        assertTrue(emergencyCard.contains("EMERGENCY_LEFT_WINKS"))
        assertTrue(emergencyCard.contains("EMERGENCY_RIGHT_WINKS"))
        assertFalse("No hardcoded literal sequence", emergencyCard.contains("\"L6 R0\""))
    }

    @Test // 9 — no alternate emergency sequence is introduced
    fun noAlternateSequenceIntroduced() {
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        for (right in 1..6) {
            assertFalse(emergencyCard.contains("\"L6 R$right\""))
        }
    }

    @Test // 10 — button remains full width
    fun buttonRemainsFullWidth() {
        assertTrue(emergencyCard.contains(".fillMaxWidth()"))
        // Height role preserved (large touch target).
        assertTrue(emergencyCard.contains("defaultMinSize(minHeight = 72.dp)"))
    }

    @Test // 11 — red emergency styling remains
    fun redEmergencyStylingRemains() {
        assertTrue(emergencyCard.contains("LisaEmergencyRed"))
        assertTrue(emergencyCard.contains(".border("))
        assertTrue(emergencyCard.contains(".background("))
    }

    @Test // 12 — touch action still routes to the same emergency handler
    fun touchRoutesToEmergencyHandler() {
        assertTrue(emergencyCard.contains("onClick = onClick"))
        assertTrue(composerGridSource.contains("onClick = onEmergency"))
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composer.contains("onEmergency = onEmergency"))
    }

    @Test // 13 — blink L6 R0 still routes to Emergency from the composer
    fun blinkRoutesToEmergency() {
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

    @Test // 14 — accessibility label still includes Emergency and L6 R0
    fun accessibilityLabelIncludesEmergencyAndSequence() {
        assertTrue(emergencyCard.contains("contentDescription = \"\${title} \${sequenceLabel}\""))
        val description = "${english.guidedEmergencyNavTitle} " +
            formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        assertTrue(description.contains("Emergency"))
        assertTrue(description.contains("L6 R0"))
    }

    @Test // 15 — no unrelated Emergency button layout is changed
    fun unrelatedEmergencyButtonsUnchanged() {
        // The composer emergency card is used only by the composer, never the guided overlay.
        assertFalse(guidedUiSource.contains("ComposerEmergencyCommandCard"))
        // The guided-mode panel emergency button keeps its own (vertical) layout.
        val panelButton = extractBlock(guidedUiSource, "internal fun GuidedEmergencyNavButton(")
        assertTrue(panelButton.contains("Column("))
        assertTrue(panelButton.contains("horizontalAlignment = Alignment.CenterHorizontally"))
    }

    @Test // 16 — RC7D.23 label cleanup remains intact
    fun rc7d23LabelCleanupRemains() {
        assertEquals(
            "Customize Phrases",
            english.guidedCategoryTitle(GuidedVocabularyCategory.Custom)
        )
        val header = extractBlock(guidedUiSource, "private fun GuidedOverlayHeader(")
        assertTrue(header.contains("screenMode != GuidedOverlayScreenMode.CategoryMenu"))
        assertFalse(header.contains("guidedCategoryMenuTitle"))
    }

    @Test // 17 — category navigation and viewport paging remain unchanged
    fun categoryNavigationAndPagingUnchanged() {
        assertFalse(CategoryViewportPaging.canGoToPreviousPage(0))
        assertTrue(CategoryViewportPaging.canGoToNextPage(0, 2))
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(0, 1000, 600))
        assertEquals(600, CategoryViewportPaging.pageAnchorOffsetPx(1, 1000, 600))
        assertEquals(
            "L2 R0",
            formatWinkSequenceShort(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT)
        )
        assertEquals(
            "L0 R2",
            formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)
        )
    }

    @Test // 18 — no Android system keyboard is introduced
    fun noAndroidSystemKeyboardIntroduced() {
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(composer.contains("import android.view.inputmethod"))
        assertFalse(composer.contains("SoftwareKeyboardController"))
        assertTrue(composer.contains("BottomAlignedEyeKeyboard"))
    }
}
