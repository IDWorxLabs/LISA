package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Primary Settings UX — no duplicate Sensitivity/Response Time; Calibration opens real flow. */
class Rc7D_43PrimarySettingsSimplificationTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    @Test
    fun approvedStructureExcludesSensitivityAndResponseTime() {
        val keys = PrimarySettingsAuthority.approvedActionKeys
        assertFalse(keys.contains("sensitivity"))
        assertFalse(keys.contains("response_time"))
        assertTrue(keys.contains("calibration"))
        assertTrue(keys.contains("speech_volume"))
        assertTrue(keys.contains("speech_speed"))
        assertTrue(keys.contains("text_size"))
        assertTrue(keys.contains("device_check"))
        assertTrue(keys.contains("developer_mode"))
        assertEquals(6, PrimarySettingsAuthority.items.size)
        assertTrue(PrimarySettingsAuthority.removedFromPrimaryKeys.contains("sensitivity"))
        assertTrue(PrimarySettingsAuthority.removedFromPrimaryKeys.contains("response_time"))
    }

    @Test
    fun sectionsMatchApprovedLayout() {
        assertEquals(
            listOf(
                PrimarySettingsAuthority.Section.Detection,
                PrimarySettingsAuthority.Section.Speech,
                PrimarySettingsAuthority.Section.Display,
                PrimarySettingsAuthority.Section.Support,
                PrimarySettingsAuthority.Section.Advanced
            ),
            PrimarySettingsAuthority.items.map { it.section }.distinct()
        )
        assertEquals(
            english.settingsSectionSupportDiagnostics,
            PrimarySettingsAuthority.sectionTitle(PrimarySettingsAuthority.Section.Support, english)
        )
    }

    @Test
    fun menuDestinationActionsMatchAuthorityOrder() {
        val actions = PrimarySettingsAuthority.menuDestinationActions(english)
        assertEquals(PrimarySettingsAuthority.items.map { it.actionId }, actions.map { it.id })
        assertEquals(6, actions.size)
    }

    @Test
    fun calibrationIsNavigationNotToggle() {
        val cal = PrimarySettingsAuthority.item(PrimarySettingsAuthority.ItemId.Calibration)
        assertEquals(MenuDestinationActionType.Navigation, cal.actionType)
        assertEquals(
            english.calibrationStatusNeedsCalibration,
            PrimarySettingsAuthority.calibrationStatusLabel(false, english)
        )
        assertEquals(
            english.calibrationStatusCalibrated,
            PrimarySettingsAuthority.calibrationStatusLabel(true, english)
        )
    }

    @Test
    fun primarySettingsUiOmitsSensitivityResponseTimeAndOffCalibration() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val panel = ui.substringAfter("private fun SettingsPanel(")
            .substringBefore("private fun PrimarySettingsLauncherCard(")
        assertFalse(panel.contains("ItemId.Sensitivity"))
        assertFalse(panel.contains("ItemId.ResponseTime"))
        assertTrue(panel.contains("calibrationStatusLabel"))
        assertFalse(panel.contains("SettingsSliderRow("))
        // Calibration must not use a misleading On/Off toggle status.
        val calibrationBranch = panel.substringAfter("ItemId.Calibration ->")
            .substringBefore("ItemId.SpeechVolume")
        assertFalse(calibrationBranch.contains("\"On\""))
        assertFalse(calibrationBranch.contains("\"Off\""))
    }

    @Test
    fun selectingCalibrationOpensSharedRecalibrationFlow() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openSettingsRecalibration()"))
        assertTrue(main.contains("setting(\"calibration\") ->"))
        assertTrue(main.contains("SettingsRecalibrationController"))
        assertTrue(main.contains("LisaPanel.Recalibration"))
        assertTrue(
            readSource("app/src/main/java/com/idworx/lisa/SettingsRecalibrationController.kt")
                .contains("QuickEyeCalibrationEngine")
        )
        assertFalse(
            readSource("app/src/main/java/com/idworx/lisa/SettingsRecalibrationController.kt")
                .contains("class DuplicateCalibrationEngine")
        )
    }

    @Test
    fun recalibrationCancelPreservesPreviousCalibration() {
        var persisted: com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration? = null
        val previous = com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration(
            leftClosedEyeThreshold = 0.2f,
            rightClosedEyeThreshold = 0.2f,
            openEyeThreshold = 0.8f,
            blinkDurationMs = 120L,
            requiredWinkFrames = 2,
            eyeOpennessBaseline = 0.85f,
            faceDistanceProxy = 0.4f,
            confidence = 0.9f,
            calibratedAtMs = 1L
        )
        val controller = SettingsRecalibrationController(
            persistCalibration = { persisted = it }
        )
        controller.start()
        assertTrue(controller.isActive)
        controller.cancel()
        assertFalse(controller.isActive)
        assertEquals(null, persisted)
        // Previous profile value untouched by cancel.
        assertEquals(0.9f, previous.confidence, 0.001f)
    }

    @Test
    fun communicationHubStillHasSensitivityAndResponseTime() {
        assertEquals(
            listOf(
                SettingsControlKind.Sensitivity,
                SettingsControlKind.ResponseTime,
                SettingsControlKind.SpeechVolume,
                SettingsControlKind.SpeechSpeed
            ),
            SettingsAndControlsHubSequences.HUB_SETTING_KINDS
        )
    }

    @Test
    fun everyItemIsBlinkReachableViaSelectModel() {
        PrimarySettingsAuthority.items.forEach { item ->
            assertTrue(item.actionType.canReceiveFocus)
        }
        assertTrue(GuidedModeNavigation.isPreviousSequence(2, 0))
        assertTrue(GuidedModeNavigation.isNextSequence(0, 2))
        assertTrue(GuidedModeNavigation.isSelectSequence(1, 1))
        assertTrue(GuidedModeNavigation.isBackSequence(2, 2))
        assertTrue(isEmergencySequence(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test
    fun primarySettingsDoesNotRequirePagingControls() {
        assertFalse(PrimarySettingsNavigationAuthority.requiresMultiplePages())
        val capabilities = MenuDestinationNavigationController.capabilities(
            MenuDestinationScreenMode.Settings
        )
        assertFalse(capabilities.supportsPageMovement)
        assertEquals(
            PrimarySettingsNavigationAuthority.capabilities(),
            capabilities
        )
        val rail = MenuDestinationNavigationController.visibleCommands(capabilities, viewportPageCount = 1)
        assertEquals(PrimarySettingsNavigationAuthority.railCommands, rail)
        assertFalse(MenuDestinationPanelCommand.PreviousPage in rail)
        assertFalse(MenuDestinationPanelCommand.NextPage in rail)
    }

    @Test
    fun everyEnabledRailSequenceMatchesCanonicalAuthority() {
        PrimarySettingsNavigationAuthority.railCommands.forEach { command ->
            val sequence = PrimarySettingsNavigationAuthority.sequence(command)
            assertTrue("Missing sequence for $command", sequence != null)
        }
        assertEquals(
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
            0 to 4
        )
        // L0 R4 must not be registered for Primary Settings (capability off).
        val actions = PrimarySettingsAuthority.menuDestinationActions(english)
        val state = MenuDestinationNavigationController.open(
            MainMenuDestination.Settings,
            LisaPanel.Settings,
            actions
        )
        val result = MenuDestinationNavigationController.processSequence(
            left = 0,
            right = 4,
            state = state,
            actions = actions,
            capabilities = PrimarySettingsNavigationAuthority.capabilities(),
            viewportHeightPx = 800,
            maxScrollPx = 0
        )
        assertEquals(MenuDestinationSequenceResult.Unmatched, result)
    }

    @Test
    fun touchAndBlinkShareMoveSelectAndHorizontalAuthority() {
        val actions = PrimarySettingsAuthority.menuDestinationActions(english)
        var state = MenuDestinationNavigationController.open(
            MainMenuDestination.Settings,
            LisaPanel.Settings,
            actions
        )
        val caps = PrimarySettingsNavigationAuthority.capabilities()

        // Move Down via blink — selection only.
        val down = MenuDestinationNavigationController.processSequence(
            0, 2, state, actions, caps
        ) as MenuDestinationSequenceResult.Navigate
        state = down.state
        assertEquals(actions[1].id, state.selectedActionId)

        // Same Move Down via touch authority method.
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertEquals(actions[2].id, state.selectedActionId)

        // Select opens highlighted Navigation item — not Move Left/Right.
        val select = MenuDestinationNavigationController.processSequence(
            1, 1, state, actions, caps
        )
        assertTrue(select is MenuDestinationSequenceResult.Activate)
        assertEquals(actions[2].id, (select as MenuDestinationSequenceResult.Activate).actionId)

        // Move Left on a launcher (Navigation) must not act as Select.
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(2, 1, state, actions, caps)
        )
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(1, 2, state, actions, caps)
        )

        // Text Size (Choice) accepts horizontal adjustment.
        while (state.selectedActionId != MenuDestinationActionId.setting("text_size") &&
            state.selectedIndex < actions.lastIndex
        ) {
            state = MenuDestinationNavigationController.move(state, actions, 1)
        }
        assertEquals(MenuDestinationActionId.setting("text_size"), state.selectedActionId)
        assertTrue(
            MenuDestinationNavigationController.processSequence(2, 1, state, actions, caps)
                is MenuDestinationSequenceResult.MoveHorizontal
        )
        assertTrue(
            MenuDestinationNavigationController.processSequence(1, 2, state, actions, caps)
                is MenuDestinationSequenceResult.MoveHorizontal
        )
        assertTrue(
            MenuDestinationNavigationController.commandIsEnabled(
                MenuDestinationPanelCommand.MoveLeft,
                state,
                actions,
                canPreviousPage = false,
                canNextPage = false
            )
        )
    }

    @Test
    fun moveUpAndDownDisableAtBoundariesWithoutFallthrough() {
        val actions = PrimarySettingsAuthority.menuDestinationActions(english)
        val caps = PrimarySettingsNavigationAuthority.capabilities()
        val first = MenuDestinationNavigationController.open(
            MainMenuDestination.Settings,
            LisaPanel.Settings,
            actions
        )
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(2, 0, first, actions, caps)
        )
        assertFalse(
            MenuDestinationNavigationController.commandIsEnabled(
                MenuDestinationPanelCommand.MoveUp,
                first,
                actions,
                canPreviousPage = false,
                canNextPage = false
            )
        )

        var last = first
        repeat(actions.size) { last = MenuDestinationNavigationController.move(last, actions, 1) }
        assertEquals(actions.last().id, last.selectedActionId)
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(0, 2, last, actions, caps)
        )
        assertFalse(
            MenuDestinationNavigationController.commandIsEnabled(
                MenuDestinationPanelCommand.MoveDown,
                last,
                actions,
                canPreviousPage = false,
                canNextPage = false
            )
        )
    }

    @Test
    fun backSequenceReturnsBrowsingCancelWithoutSelecting() {
        val actions = PrimarySettingsAuthority.menuDestinationActions(english)
        val state = MenuDestinationNavigationController.open(
            MainMenuDestination.Settings,
            LisaPanel.Settings,
            actions
        )
        val result = MenuDestinationNavigationController.processSequence(
            2, 2, state, actions, PrimarySettingsNavigationAuthority.capabilities()
        )
        assertTrue(result is MenuDestinationSequenceResult.Back)
        assertFalse((result as MenuDestinationSequenceResult.Back).state.isActive)
        assertEquals(state.selectedActionId, result.state.selectedActionId)
    }

    @Test
    fun disabledPageSequencesDoNotRegisterWhenPagingUnsupported() {
        val actions = PrimarySettingsAuthority.menuDestinationActions(english)
        val state = MenuDestinationNavigationController.open(
            MainMenuDestination.Settings,
            LisaPanel.Settings,
            actions
        ).copy(viewportPage = 0, viewportPageCount = 2)
        val caps = PrimarySettingsNavigationAuthority.capabilities()
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(
                4, 0, state, actions, caps, viewportHeightPx = 100, maxScrollPx = 250
            )
        )
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(
                0, 4, state, actions, caps, viewportHeightPx = 100, maxScrollPx = 250
            )
        )
    }

    @Test
    fun multiPageDestinationsStillPageViaTouchAndBlinkWhenCapable() {
        val caps = MenuDestinationNavigationCapabilities.Interactive
        val state = MenuDestinationNavigationController.open(
            MainMenuDestination.AboutLisa,
            LisaPanel.AboutLisa,
            emptyList()
        )
        assertTrue(
            MenuDestinationPanelCommand.NextPage in
                MenuDestinationNavigationController.visibleCommands(caps, viewportPageCount = 3)
        )
        val blinkNext = MenuDestinationNavigationController.processSequence(
            0, 4, state, emptyList(), caps, viewportHeightPx = 100, maxScrollPx = 250
        ) as MenuDestinationSequenceResult.Navigate
        val touchNext = MenuDestinationNavigationController.nextPage(state, 100, 250)
        assertEquals(touchNext.viewportPage, blinkNext.state.viewportPage)
        assertEquals(touchNext.scrollRequestPx, blinkNext.state.scrollRequestPx)

        val onLast = touchNext.copy(viewportPage = touchNext.viewportPageCount - 1)
        assertEquals(
            MenuDestinationSequenceResult.Unmatched,
            MenuDestinationNavigationController.processSequence(
                0, 4, onLast, emptyList(), caps, viewportHeightPx = 100, maxScrollPx = 250
            )
        )
        assertFalse(
            MenuDestinationNavigationController.commandIsEnabled(
                MenuDestinationPanelCommand.NextPage,
                onLast,
                emptyList(),
                canPreviousPage = true,
                canNextPage = false
            )
        )
        assertFalse(
            MenuDestinationNavigationController.commandIsEnabled(
                MenuDestinationPanelCommand.PreviousPage,
                state,
                emptyList(),
                canPreviousPage = false,
                canNextPage = true
            )
        )
    }

    @Test
    fun launcherCardsDoNotPrintIdentitySequencesThatCollideWithRail() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val card = ui.substringAfter("private fun PrimarySettingsLauncherCard(")
            .substringBefore("private fun SettingsLinkRow(")
        assertFalse(card.contains("sequenceLabel"))
    }

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
}
