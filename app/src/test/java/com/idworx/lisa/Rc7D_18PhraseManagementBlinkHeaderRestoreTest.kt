package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.18 — Phrase Management / Phrase Details must keep the shared Communication Workspace
 * blink-status header (WATCHING YOUR EYES, Left/Right, Sensitivity, Response time).
 */
class Rc7D_18PhraseManagementBlinkHeaderRestoreTest {

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

    private fun accessibilityUi(): String =
        readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")

    private fun sharedHeaderSlice(): String {
        val ui = accessibilityUi()
        val start = ui.indexOf("if (showSharedBlinkStatusHeader)")
        val end = ui.indexOf("showGuidedVocabularyOverlayAlongsideManagement", start)
        assertTrue("shared blink header host missing", start >= 0 && end > start)
        return ui.substring(start, end)
    }

    private fun phraseManagementHostSlice(): String {
        val ui = accessibilityUi()
        val start = ui.indexOf("if (phraseManagementActive)")
        val end = ui.indexOf("EyeControlledPhraseComposerOverlay(", start)
        assertTrue(start >= 0 && end > start)
        return ui.substring(start, end)
    }

    // --- SHELL POLICY ---

    @Test
    fun sharedBlinkHeaderShownForPhraseManagementAndDetails() {
        assertTrue(PhraseManagementController.showSharedBlinkStatusHeader(phraseComposerActive = false))
        assertTrue(PhraseManagementController.occupiesMainContentSlot(LisaPanel.VocabularyTraining))
        assertFalse(
            PhraseManagementController.showGuidedVocabularyOverlayAlongsideManagement(
                phraseComposerActive = false,
                phraseManagementActive = true
            )
        )
    }

    @Test
    fun composerStillHidesSharedBlinkHeader() {
        assertFalse(PhraseManagementController.showSharedBlinkStatusHeader(phraseComposerActive = true))
    }

    @Test
    fun phraseManagementDoesNotHideSharedBlinkHeaderAnymore() {
        // RC7D.17A incorrectly hid header with phraseManagementActive; RC7D.18 restores it.
        val ui = accessibilityUi()
        assertFalse(ui.contains("!phraseComposerActive && !phraseManagementActive) {\n        Column("))
        assertTrue(ui.contains("if (showSharedBlinkStatusHeader)"))
        assertTrue(ui.contains("EverydayCommunicationPanel("))
        assertTrue(ui.contains("CompactSensitivityControls("))
        assertTrue(ui.contains("SequenceProgressDots("))
    }

    // --- PHRASE MANAGEMENT HEADER ---

    @Test
    fun phraseManagementRuntimePathRendersWatchingYourEyesViaSharedHeader() {
        assertEquals("WATCHING YOUR EYES...", english.eyeTrackingStatusWatching)
        val header = sharedHeaderSlice()
        assertTrue(header.contains("EverydayCommunicationPanel("))
        assertTrue(header.contains("userDisplay = userDisplay"))
        // Phrase Management host sits after the shared header in the same Column shell.
        val ui = accessibilityUi()
        val headerIdx = ui.indexOf("if (showSharedBlinkStatusHeader)")
        val pmIdx = ui.indexOf("if (phraseManagementActive)")
        assertTrue(headerIdx in 0 until pmIdx)
        assertTrue(phraseManagementHostSlice().contains("VocabularyManagementPanel("))
    }

    @Test
    fun phraseManagementHeaderRendersLeftAndRightCounters() {
        assertTrue(english.leftDots(0).startsWith("Left:"))
        assertTrue(english.rightDots(0).startsWith("Right:"))
        val header = sharedHeaderSlice()
        assertTrue(header.contains("SequenceProgressDots("))
        assertTrue(header.contains("userDisplay.leftWinkDots"))
        assertTrue(header.contains("userDisplay.rightWinkDots"))
        assertTrue(header.contains("phraseManagementActive"))
    }

    @Test
    fun phraseManagementHeaderRendersSensitivityControlsAndValue() {
        assertTrue(english.sensitivityDecrease.isNotBlank())
        assertTrue(english.sensitivityIncrease.isNotBlank())
        val header = sharedHeaderSlice()
        assertTrue(header.contains("CompactSensitivityControls("))
        assertTrue(header.contains("onDecrease = onSensitivityDecrease"))
        assertTrue(header.contains("onIncrease = onSensitivityIncrease"))
        assertTrue(header.contains("sensitivityLevel = sensitivityLevel"))
        val controls = accessibilityUi().substring(
            accessibilityUi().indexOf("private fun CompactSensitivityControls")
        )
        assertTrue(controls.contains("uiStrings.sensitivityDecrease"))
        assertTrue(controls.contains("uiStrings.sensitivityIncrease"))
        assertTrue(controls.contains("uiStrings.sensitivity"))
        assertTrue(controls.contains("listeningStatusLine(sensitivityLevel, responseTimeSec)"))
    }

    @Test
    fun phraseManagementHeaderRendersResponseTimeControlsAndValue() {
        assertTrue(english.responseTimeDecrease.isNotBlank())
        assertTrue(english.responseTimeIncrease.isNotBlank())
        val header = sharedHeaderSlice()
        assertTrue(header.contains("onDecreaseResponseTime = onResponseTimeDecrease"))
        assertTrue(header.contains("onIncreaseResponseTime = onResponseTimeIncrease"))
        assertTrue(header.contains("responseTimeSec = responseTimeSec"))
        val controls = accessibilityUi().substring(
            accessibilityUi().indexOf("private fun CompactSensitivityControls")
        )
        assertTrue(controls.contains("uiStrings.responseTimeDecrease"))
        assertTrue(controls.contains("uiStrings.responseTimeIncrease"))
        assertTrue(controls.contains("uiStrings.responseTime"))
    }

    @Test
    fun singleSharedHeaderNoDuplicateInsidePhraseManagementUi() {
        val managementUi = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertFalse(managementUi.contains("EverydayCommunicationPanel("))
        assertFalse(managementUi.contains("CompactSensitivityControls("))
        assertFalse(managementUi.contains("eyeTrackingStatusWatching"))
        assertEquals(
            1,
            Regex("(?<!fun )EverydayCommunicationPanel\\(").findAll(accessibilityUi()).count()
        )
        assertEquals(
            1,
            Regex("(?<!fun )CompactSensitivityControls\\(").findAll(accessibilityUi()).count()
        )
    }

    @Test
    fun headerRemainsWithScrollBackEmergencyOnAllPages() {
        assertTrue(PhraseManagementController.showSharedBlinkStatusHeader(false))
        val commandsFirst = PhraseManagementController.listCommandEntries(
            PhraseManagementUiState(listPageIndex = 0),
            phraseCount = PhraseManagementController.PAGE_SIZE + 2,
            uiStrings = english
        )
        val lastPage = PhraseManagementController.pageCount(PhraseManagementController.PAGE_SIZE + 2) - 1
        val commandsLast = PhraseManagementController.listCommandEntries(
            PhraseManagementUiState(listPageIndex = lastPage),
            phraseCount = PhraseManagementController.PAGE_SIZE + 2,
            uiStrings = english
        )
        assertEquals(3, commandsFirst.size)
        assertEquals(3, commandsLast.size)
        val host = phraseManagementHostSlice()
        assertTrue(host.contains("onScrollUp"))
        assertTrue(host.contains("onScrollDown"))
        assertTrue(host.contains("onEmergency"))
        val listUi = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(listUi.contains("PhraseManagementCommandStrip("))
        assertTrue(listUi.contains("ComposerEmergencyCommandCard("))
    }

    // --- PHRASE DETAILS HEADER ---

    @Test
    fun phraseDetailsUsesSameSharedHeaderAndRemainsUsable() {
        assertTrue(PhraseManagementController.occupiesMainContentSlot(LisaPanel.VocabularyTraining))
        assertTrue(PhraseManagementController.showSharedBlinkStatusHeader(false))
        val managementUi = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(managementUi.contains("PhraseManagementScreen.Details"))
        assertTrue(managementUi.contains("PhraseDetailsPanel("))
        val details = PhraseManagementController.detailsActionEntries(english)
        assertEquals(3, details.size)
        assertEquals(PhraseManagementController.PhraseDetailsAction.Edit, details[0].action)
        assertEquals(PhraseManagementController.PhraseDetailsAction.Move, details[1].action)
        assertEquals(PhraseManagementController.PhraseDetailsAction.Delete, details[2].action)
        assertTrue(managementUi.contains("ComposerEmergencyCommandCard("))
        assertTrue(managementUi.contains("GuidedModeNavigation.BACK_LEFT"))
    }

    // --- FUNCTIONALITY / WIRING ---

    @Test
    fun sensitivityAndResponseTimeUseCanonicalHandlers() {
        val header = sharedHeaderSlice()
        assertTrue(header.contains("onSensitivityDecrease"))
        assertTrue(header.contains("onSensitivityIncrease"))
        assertTrue(header.contains("onResponseTimeDecrease"))
        assertTrue(header.contains("onResponseTimeIncrease"))
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("onSensitivityDecrease"))
        assertTrue(main.contains("onSensitivityIncrease"))
        assertTrue(main.contains("onResponseTimeDecrease"))
        assertTrue(main.contains("onResponseTimeIncrease"))
    }

    @Test
    fun blinkCountsDrivenBySharedUserDisplayNotLocalPlaceholders() {
        val header = sharedHeaderSlice()
        assertTrue(header.contains("userDisplay.leftWinkDots"))
        assertTrue(header.contains("userDisplay.rightWinkDots"))
        assertFalse(header.contains("leftWinkCount = 0"))
        assertFalse(header.contains("rightWinkCount = 0"))
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("leftWinkDots = uiDiagLeftCount.value"))
    }

    @Test
    fun phraseManagementAndDetailsGesturesStillRoute() {
        val ctx = LisaGestureContext(
            activePanel = LisaPanel.VocabularyTraining,
            guidedOverlayActive = false,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(LisaInteractionMode.PhraseManagement, ModeScopedGestureAuthority.activeMode(ctx))
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(
                ctx, GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(
                ctx, GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(
                ctx, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS
            )
        )
    }

    @Test
    fun scrollBackEmergencyStillWorkWithHeaderRestored() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("scrollPhraseManagementList"))
        assertTrue(main.contains("exitPhraseManagement("))
        assertTrue(main.contains("PhraseManagementExitDestination.CommunicationWorkspace"))
        assertEquals(
            "L6 R0",
            formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
    }

    @Test
    fun closeClearRemainInSharedBottomChrome() {
        val ui = accessibilityUi()
        val pmEnd = ui.indexOf("EyeControlledPhraseComposerOverlay(")
        val bottom = ui.substring(pmEnd)
        assertTrue(bottom.contains("uiStrings.close"))
        assertTrue(bottom.contains("uiStrings.reset"))
        assertTrue(bottom.contains("onMenuClick"))
        assertTrue(bottom.contains("onReset"))
    }

    @Test
    fun noOldClippedBottomHostForPhraseManagement() {
        assertEquals(1, Regex("VocabularyManagementPanel\\(").findAll(accessibilityUi()).count())
        assertTrue(accessibilityUi().contains("LisaPanel.VocabularyTraining -> Unit"))
        assertTrue(phraseManagementHostSlice().contains(".weight(1f)"))
    }

    @Test
    fun pageSizeRemainsThreeForSmallScreensWithHeader() {
        assertEquals(3, PhraseManagementController.PAGE_SIZE)
        // Compact budget now includes the restored blink header (~160dp).
        assertTrue(
            PhraseManagementController.compactLayoutFits(
                screenHeightDp = 720,
                headerDp = 160
            )
        )
    }

    @Test
    fun eyeKeyboardIntactAndNoSystemIme() {
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composerUi.contains("BottomAlignedEyeKeyboard"))
        assertFalse(composerUi.contains("OutlinedTextField"))
        assertFalse(
            readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
                .contains("LocalSoftwareKeyboardController")
        )
    }
}
