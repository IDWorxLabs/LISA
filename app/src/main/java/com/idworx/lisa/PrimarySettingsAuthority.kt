package com.idworx.lisa

/**
 * Canonical primary Settings screen — caregiver/user controls opened from Main Menu.
 *
 * Detection/speech timing (Sensitivity, Response Time) live only in Communication
 * Settings & Controls — not duplicated here.
 *
 * After simplification (~6 items), content fits one viewport. Previous/Next Page are
 * intentionally omitted — Move Up/Down + Select cover the full list.
 *
 * Selection model (while Settings is open):
 * - Move Up / Down: L2 R0 / L0 R2
 * - Move Left / Right: L2 R1 / L1 R2 — only when the highlighted item is Choice (Text Size)
 * - Select: L1 R1 opens or toggles the highlighted item
 * - Back: L2 R2 · Emergency: L6 R0 · Menu: L4 R6
 */
object PrimarySettingsAuthority {

    enum class Section {
        Detection,
        Speech,
        Display,
        Support,
        Advanced
    }

    enum class ItemId {
        Calibration,
        SpeechVolume,
        SpeechSpeed,
        TextSize,
        DeviceCheck,
        DeveloperMode
    }

    data class Item(
        val id: ItemId,
        val section: Section,
        val actionKey: String,
        val actionType: MenuDestinationActionType
    ) {
        val actionId: MenuDestinationActionId
            get() = MenuDestinationActionId.setting(actionKey)
    }

    val items: List<Item> = listOf(
        Item(ItemId.Calibration, Section.Detection, "calibration", MenuDestinationActionType.Navigation),
        Item(ItemId.SpeechVolume, Section.Speech, "speech_volume", MenuDestinationActionType.Navigation),
        Item(ItemId.SpeechSpeed, Section.Speech, "speech_speed", MenuDestinationActionType.Navigation),
        Item(ItemId.TextSize, Section.Display, "text_size", MenuDestinationActionType.Choice),
        Item(ItemId.DeviceCheck, Section.Support, "device_check", MenuDestinationActionType.Navigation),
        Item(ItemId.DeveloperMode, Section.Advanced, "developer_mode", MenuDestinationActionType.Toggle)
    )

    fun item(id: ItemId): Item = items.first { it.id == id }

    fun itemForAction(actionId: MenuDestinationActionId): Item? =
        items.firstOrNull { it.actionId == actionId }

    fun menuDestinationActions(uiStrings: LisaUiStrings): List<MenuDestinationAction> =
        items.map { item ->
            MenuDestinationAction(
                id = item.actionId,
                label = title(item.id, uiStrings),
                actionType = item.actionType
            )
        }

    fun title(id: ItemId, uiStrings: LisaUiStrings): String = when (id) {
        ItemId.Calibration -> uiStrings.calibrationTitle
        ItemId.SpeechVolume -> uiStrings.guidedSelectSpeechVolumeSetting
        ItemId.SpeechSpeed -> uiStrings.guidedSelectSpeechSpeedSetting
        ItemId.TextSize -> uiStrings.textSize
        ItemId.DeviceCheck -> uiStrings.runDeviceCheckTitle
        ItemId.DeveloperMode -> uiStrings.developerModeTitle
    }

    fun sectionTitle(section: Section, uiStrings: LisaUiStrings): String = when (section) {
        Section.Detection -> uiStrings.settingsSectionDetection
        Section.Speech -> uiStrings.settingsSectionSpeech
        Section.Display -> uiStrings.settingsSectionDisplay
        Section.Support -> uiStrings.settingsSectionSupportDiagnostics
        Section.Advanced -> uiStrings.settingsSectionAdvanced
    }

    fun calibrationStatusLabel(
        hasSavedCalibration: Boolean,
        uiStrings: LisaUiStrings
    ): String = if (hasSavedCalibration) {
        uiStrings.calibrationStatusCalibrated
    } else {
        uiStrings.calibrationStatusNeedsCalibration
    }

    val approvedActionKeys: Set<String> = items.map { it.actionKey }.toSet()

    val removedFromPrimaryKeys: Set<String> = setOf(
        "sensitivity",
        "response_time",
        "countdown",
        "emergency_volume",
        "replay_learning",
        "practice_communication",
        "practice_navigation",
        "reset_learning",
        "narration",
        "narration_speed",
        "narration_volume",
        "developer_tools",
        "profile_backup"
    )

    /** True when Left/Right adjust the highlighted setting inline (Choice cards only). */
    fun supportsInlineHorizontalAdjustment(actionId: MenuDestinationActionId?): Boolean {
        val item = itemForAction(actionId ?: return false) ?: return false
        return item.actionType == MenuDestinationActionType.Choice
    }
}

/**
 * Canonical Primary Settings right-rail — one action model for touch and blink.
 * Labels and sequences must come from the same [MenuDestinationNavigationController]
 * specification used for gesture routing.
 */
object PrimarySettingsNavigationAuthority {
    val railCommands: List<MenuDestinationPanelCommand> = listOf(
        MenuDestinationPanelCommand.MoveUp,
        MenuDestinationPanelCommand.MoveDown,
        MenuDestinationPanelCommand.MoveLeft,
        MenuDestinationPanelCommand.MoveRight,
        MenuDestinationPanelCommand.Select,
        MenuDestinationPanelCommand.Back,
        MenuDestinationPanelCommand.Emergency
    )

    fun capabilities(): MenuDestinationNavigationCapabilities =
        MenuDestinationNavigationCapabilities(
            supportsItemMovement = true,
            supportsPageMovement = false,
            supportsHorizontalMovement = true,
            supportsSelection = true
        )

    fun sequence(command: MenuDestinationPanelCommand): Pair<Int, Int>? = when (command) {
        MenuDestinationPanelCommand.MoveUp ->
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT
        MenuDestinationPanelCommand.MoveDown ->
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
        MenuDestinationPanelCommand.MoveLeft -> 2 to 1
        MenuDestinationPanelCommand.MoveRight -> 1 to 2
        MenuDestinationPanelCommand.Select ->
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
        MenuDestinationPanelCommand.Back ->
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT
        MenuDestinationPanelCommand.Emergency ->
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
        else -> null
    }

    fun requiresMultiplePages(): Boolean = false
}
