package com.idworx.lisa

/**
 * Mode-Scoped Gesture Authority (MSGA) — constitutional interaction model for LISA.
 *
 * Global gestures are always active. Mode-scoped gestures are interpreted only by the
 * currently active interaction mode. Identical sequences may appear in multiple modes;
 * there is no cross-mode ambiguity because only one mode is active at a time.
 */
enum class LisaInteractionMode {
    CommunicationVocabulary,
    CommunicationCategoryMenu,
    CommunicationAdjustment,
    PhraseComposerKeyboard,
    PhraseComposerDestinationCategory,
    PhraseComposerSaveConfirmation,
    PhraseComposerDuplicateWarning,
    PhraseComposerCancelConfirm,
    PhraseComposerSuccess,
    PhraseManagement,
    SettingsPanel,
    Confirmation,
    EmergencyModal,
    None
}

/** Where [ModeScopedGestureAuthority] routes a finalized blink sequence. */
enum class GestureRoutingTarget {
    Emergency,
    FinishTraining,
    PhraseComposer,
    PhraseManagement,
    SettingsPanelBack,
    GuidedOverlay,
    SystemCommand,
    CommunicationPhrasePath
}

enum class GlobalGestureId {
    Emergency,
    Back,
    PreviousPage,
    NextPage,
    Categories,
    Select,
    FinishTraining,
    DecreaseValue,
    IncreaseValue
}

enum class ModeGestureTier {
    /** Highest priority within a mode — command-panel / menu actions. */
    Command,
    /** Primary content — phrases, letters, categories. */
    Content
}

data class ModeGestureBinding(
    val mode: LisaInteractionMode,
    val left: Int,
    val right: Int,
    val label: String,
    val tier: ModeGestureTier
)

data class LisaGestureContext(
    val activePanel: LisaPanel,
    val guidedOverlayActive: Boolean,
    val guidedScreenMode: GuidedOverlayScreenMode?,
    val isAdjustingPreference: Boolean,
    val phraseComposerMode: PhraseComposerMode?,
    val confirmationActive: Boolean = false,
    val emergencyModalActive: Boolean = false
)

object ModeScopedGestureAuthority {

    val globalGestures: Map<Pair<Int, Int>, GlobalGestureId> = mapOf(
        EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS to GlobalGestureId.Emergency,
        GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT to GlobalGestureId.Back,
        GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT to GlobalGestureId.PreviousPage,
        GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT to GlobalGestureId.NextPage,
        GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT to GlobalGestureId.Categories,
        GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT to GlobalGestureId.Select,
        GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT to GlobalGestureId.FinishTraining,
        GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT to GlobalGestureId.DecreaseValue,
        GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT to GlobalGestureId.IncreaseValue
    )

    /** Keyboard composer command namespace (RC7D navigation panel). */
    val phraseComposerCommandSequences: Map<PhraseComposerActionId, Pair<Int, Int>> = mapOf(
        PhraseComposerActionId.MoveUp to (
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT
            ),
        PhraseComposerActionId.MoveDown to (
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
            ),
        PhraseComposerActionId.MoveLeft to catalogSlot(0),
        PhraseComposerActionId.MoveRight to catalogSlot(1),
        PhraseComposerActionId.SelectKey to (
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
            ),
        PhraseComposerActionId.Backspace to catalogSlot(2),
        PhraseComposerActionId.Preview to catalogSlot(3),
        PhraseComposerActionId.Save to catalogSlot(4),
        PhraseComposerActionId.ToggleKeyboardLayout to composerToggleSequence(),
        PhraseComposerActionId.Back to (
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT
            )
    )

    fun activeMode(context: LisaGestureContext): LisaInteractionMode = when {
        context.emergencyModalActive -> LisaInteractionMode.EmergencyModal
        context.activePanel == LisaPanel.PhraseEditor -> when (context.phraseComposerMode) {
            PhraseComposerMode.Keyboard -> LisaInteractionMode.PhraseComposerKeyboard
            PhraseComposerMode.DestinationCategorySelection -> LisaInteractionMode.PhraseComposerDestinationCategory
            PhraseComposerMode.SaveConfirmation -> LisaInteractionMode.PhraseComposerSaveConfirmation
            PhraseComposerMode.DuplicateWarning -> LisaInteractionMode.PhraseComposerDuplicateWarning
            PhraseComposerMode.CancelConfirm -> LisaInteractionMode.PhraseComposerCancelConfirm
            PhraseComposerMode.ConfirmDelete -> LisaInteractionMode.PhraseComposerSaveConfirmation
            PhraseComposerMode.Success -> LisaInteractionMode.PhraseComposerSuccess
            null -> LisaInteractionMode.PhraseComposerDestinationCategory
        }
        context.activePanel == LisaPanel.VocabularyTraining -> LisaInteractionMode.PhraseManagement
        context.activePanel != LisaPanel.None -> LisaInteractionMode.SettingsPanel
        context.confirmationActive -> LisaInteractionMode.Confirmation
        context.guidedOverlayActive -> when {
            context.isAdjustingPreference -> LisaInteractionMode.CommunicationAdjustment
            context.guidedScreenMode == GuidedOverlayScreenMode.CategoryMenu ->
                LisaInteractionMode.CommunicationCategoryMenu
            else -> LisaInteractionMode.CommunicationVocabulary
        }
        else -> LisaInteractionMode.None
    }

    fun isGlobalGesture(left: Int, right: Int): Boolean = (left to right) in globalGestures

    fun globalGestureId(left: Int, right: Int): GlobalGestureId? = globalGestures[left to right]

    /**
     * Determines which handler owns a finalized sequence.
     * Global gestures that apply universally are identified first; then the active mode owns interpretation.
     */
    fun routingTarget(context: LisaGestureContext, left: Int, right: Int): GestureRoutingTarget {
        if (isEmergencySequence(left, right)) return GestureRoutingTarget.Emergency
        if (GuidedModeNavigation.isFinishTrainingSequence(left, right)) return GestureRoutingTarget.FinishTraining
        if (context.emergencyModalActive) return GestureRoutingTarget.Emergency

        when (activeMode(context)) {
            LisaInteractionMode.EmergencyModal -> return GestureRoutingTarget.Emergency
            LisaInteractionMode.PhraseComposerKeyboard,
            LisaInteractionMode.PhraseComposerDestinationCategory,
            LisaInteractionMode.PhraseComposerSaveConfirmation,
            LisaInteractionMode.PhraseComposerDuplicateWarning,
            LisaInteractionMode.PhraseComposerCancelConfirm,
            LisaInteractionMode.PhraseComposerSuccess -> return GestureRoutingTarget.PhraseComposer

            LisaInteractionMode.PhraseManagement -> return GestureRoutingTarget.PhraseManagement

            LisaInteractionMode.SettingsPanel -> {
                if (GuidedModeNavigation.isBackSequence(left, right)) {
                    return GestureRoutingTarget.SettingsPanelBack
                }
            }

            LisaInteractionMode.CommunicationVocabulary,
            LisaInteractionMode.CommunicationCategoryMenu,
            LisaInteractionMode.CommunicationAdjustment -> {
                if (context.guidedOverlayActive) return GestureRoutingTarget.GuidedOverlay
            }

            LisaInteractionMode.Confirmation,
            LisaInteractionMode.None -> Unit
        }

        if (context.guidedOverlayActive) return GestureRoutingTarget.GuidedOverlay
        if (LisaSystemLanguage.resolveGlobalCommand(left, right) != null) {
            return GestureRoutingTarget.SystemCommand
        }
        return GestureRoutingTarget.CommunicationPhrasePath
    }

    /** Bindings owned by Communication vocabulary mode (visible phrase slots). */
    fun communicationVocabularyBindings(): List<ModeGestureBinding> =
        (0 until GuidedPageSequences.slots.size).map { index ->
            val (left, right) = GuidedPageSequences.slotAt(index)
            ModeGestureBinding(
                mode = LisaInteractionMode.CommunicationVocabulary,
                left = left,
                right = right,
                label = "phrase_slot_$index",
                tier = ModeGestureTier.Content
            )
        }

    /**
     * Bindings owned by Category Menu mode: whole-page jump commands (RC7D.20) plus direct
     * category shortcuts. Page jumps sit in the Command tier so they never collide with the
     * Content-tier category shortcuts within this one mode.
     */
    fun communicationCategoryMenuBindings(): List<ModeGestureBinding> =
        listOf(
            ModeGestureBinding(
                mode = LisaInteractionMode.CommunicationCategoryMenu,
                left = GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                right = GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
                label = "previous_category_page",
                tier = ModeGestureTier.Command
            ),
            ModeGestureBinding(
                mode = LisaInteractionMode.CommunicationCategoryMenu,
                left = GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                right = GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
                label = "next_category_page",
                tier = ModeGestureTier.Command
            )
        ) +
            GuidedCategoryShortcuts.allGestures().mapIndexed { index, (left, right) ->
                ModeGestureBinding(
                    mode = LisaInteractionMode.CommunicationCategoryMenu,
                    left = left,
                    right = right,
                    label = "category_shortcut_$index",
                    tier = ModeGestureTier.Content
                )
            }

    /** Keyboard composer command-panel bindings (command tier only — cursor selects keys). */
    fun phraseComposerKeyboardCommandBindings(): List<ModeGestureBinding> =
        phraseComposerCommandSequences.map { (actionId, sequence) ->
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerKeyboard,
                left = sequence.first,
                right = sequence.second,
                label = actionId.name,
                tier = ModeGestureTier.Command
            )
        }

    /** Save-confirmation confirm/back bindings (RC7D.9). */
    fun phraseComposerSaveConfirmationBindings(): List<ModeGestureBinding> =
        listOf(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerSaveConfirmation,
                left = GuidedModeNavigation.SELECT_LEFT,
                right = GuidedModeNavigation.SELECT_RIGHT,
                label = "confirm_save",
                tier = ModeGestureTier.Command
            ),
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerSaveConfirmation,
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = "back",
                tier = ModeGestureTier.Command
            )
        )

    /** Duplicate-warning open-category / continue-editing bindings (RC7D.9). */
    fun phraseComposerDuplicateWarningBindings(): List<ModeGestureBinding> =
        listOf(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerDuplicateWarning,
                left = GuidedModeNavigation.SELECT_LEFT,
                right = GuidedModeNavigation.SELECT_RIGHT,
                label = "open_duplicate_category",
                tier = ModeGestureTier.Command
            ),
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerDuplicateWarning,
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = "continue_editing",
                tier = ModeGestureTier.Command
            )
        )

    /** Destination-category selection bindings. */
    fun phraseComposerDestinationBindings(): List<ModeGestureBinding> =
        CustomPhraseEngine.selectableCategories.mapIndexed { index, category ->
            val (left, right) = GuidedPageSequences.slotAt(index)
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerDestinationCategory,
                left = left,
                right = right,
                label = category.name,
                tier = ModeGestureTier.Content
            )
        }

    /** Success-screen follow-up bindings. */
    fun phraseComposerSuccessBindings(): List<ModeGestureBinding> =
        listOf(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerSuccess,
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = "create_another",
                tier = ModeGestureTier.Content
            ),
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerSuccess,
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = "return_to_communication",
                tier = ModeGestureTier.Content
            )
        )

    /** Cancel-confirm yes/no bindings. */
    fun phraseComposerCancelConfirmBindings(): List<ModeGestureBinding> =
        listOf(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerCancelConfirm,
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = "confirm_cancel",
                tier = ModeGestureTier.Command
            ),
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseComposerCancelConfirm,
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = "keep_composing",
                tier = ModeGestureTier.Command
            )
        )

    /** RC7D.15 — Phrase Management owns Back + optional Scroll + visible phrase slots. */
    fun phraseManagementBindings(): List<ModeGestureBinding> = buildList {
        add(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseManagement,
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = "back",
                tier = ModeGestureTier.Command
            )
        )
        add(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseManagement,
                left = GuidedModeNavigation.PREVIOUS_LEFT,
                right = GuidedModeNavigation.PREVIOUS_RIGHT,
                label = "scroll_up",
                tier = ModeGestureTier.Command
            )
        )
        add(
            ModeGestureBinding(
                mode = LisaInteractionMode.PhraseManagement,
                left = GuidedModeNavigation.NEXT_LEFT,
                right = GuidedModeNavigation.NEXT_RIGHT,
                label = "scroll_down",
                tier = ModeGestureTier.Command
            )
        )
        for (index in 0 until PhraseManagementController.PAGE_SIZE) {
            val (left, right) = GuidedPageSequences.slotAt(index)
            add(
                ModeGestureBinding(
                    mode = LisaInteractionMode.PhraseManagement,
                    left = left,
                    right = right,
                    label = "phrase_slot_$index",
                    tier = ModeGestureTier.Content
                )
            )
        }
    }

    /** Settings panels inherit global Back only — no mode-local phrase slots. */
    fun settingsPanelBindings(): List<ModeGestureBinding> = emptyList()

    fun namespaceFor(mode: LisaInteractionMode): List<ModeGestureBinding> = when (mode) {
        LisaInteractionMode.CommunicationVocabulary -> communicationVocabularyBindings()
        LisaInteractionMode.CommunicationCategoryMenu -> communicationCategoryMenuBindings()
        LisaInteractionMode.CommunicationAdjustment -> emptyList()
        LisaInteractionMode.PhraseComposerKeyboard -> phraseComposerKeyboardCommandBindings()
        LisaInteractionMode.PhraseComposerDestinationCategory -> phraseComposerDestinationBindings()
        LisaInteractionMode.PhraseComposerSaveConfirmation -> phraseComposerSaveConfirmationBindings()
        LisaInteractionMode.PhraseComposerDuplicateWarning -> phraseComposerDuplicateWarningBindings()
        LisaInteractionMode.PhraseComposerCancelConfirm -> phraseComposerCancelConfirmBindings()
        LisaInteractionMode.PhraseComposerSuccess -> phraseComposerSuccessBindings()
        LisaInteractionMode.PhraseManagement -> phraseManagementBindings()
        LisaInteractionMode.SettingsPanel -> settingsPanelBindings()
        LisaInteractionMode.Confirmation -> emptyList()
        LisaInteractionMode.EmergencyModal -> emptyList()
        LisaInteractionMode.None -> emptyList()
    }

    fun allRegisteredModes(): List<LisaInteractionMode> = listOf(
        LisaInteractionMode.CommunicationVocabulary,
        LisaInteractionMode.CommunicationCategoryMenu,
        LisaInteractionMode.CommunicationAdjustment,
        LisaInteractionMode.PhraseComposerKeyboard,
        LisaInteractionMode.PhraseComposerDestinationCategory,
        LisaInteractionMode.PhraseComposerSaveConfirmation,
        LisaInteractionMode.PhraseComposerDuplicateWarning,
        LisaInteractionMode.PhraseComposerCancelConfirm,
        LisaInteractionMode.PhraseComposerSuccess,
        LisaInteractionMode.PhraseManagement,
        LisaInteractionMode.SettingsPanel,
        LisaInteractionMode.Confirmation
    )

    private fun catalogSlot(index: Int): Pair<Int, Int> = GuidedPageSequences.slotAt(index)

    /** Shortest composer-only sequence not used by other keyboard commands (RC7D.2). */
    private fun composerToggleSequence(): Pair<Int, Int> = GuidedPageSequences.slotAt(7)
}

/**
 * Self-audit for Mode-Scoped Gesture Authority.
 */
object ModeScopedGestureAuthorityAudit {

    data class Finding(val message: String)

    fun auditAll(): List<Finding> = buildList {
        addAll(auditEveryModeOwnsNamespace())
        addAll(auditKeyboardNavigationSequences())
        addAll(auditGlobalGesturesAlwaysActive())
        addAll(auditNoSameTierAmbiguityWithinMode())
        addAll(auditCrossModeOverlapIsModeScoped())
        addAll(auditKeyboardCommandsIgnoredOutsideComposer())
    }

    fun passes(): Boolean = auditAll().isEmpty()

    private fun auditEveryModeOwnsNamespace(): List<Finding> {
        val modesWithoutLocalNamespace = setOf(
            LisaInteractionMode.CommunicationAdjustment,
            LisaInteractionMode.SettingsPanel,
            LisaInteractionMode.Confirmation
        )
        val missing = ModeScopedGestureAuthority.allRegisteredModes().filter { mode ->
            mode !in modesWithoutLocalNamespace &&
                ModeScopedGestureAuthority.namespaceFor(mode).isEmpty()
        }
        return if (missing.isEmpty()) emptyList() else {
            listOf(Finding("Modes without registered namespace: $missing"))
        }
    }

    internal fun auditKeyboardNavigationSequences(): List<Finding> {
        val expected = mapOf(
            PhraseComposerActionId.MoveUp to (2 to 0),
            PhraseComposerActionId.MoveDown to (0 to 2),
            PhraseComposerActionId.MoveLeft to (2 to 1),
            PhraseComposerActionId.MoveRight to (1 to 2),
            PhraseComposerActionId.SelectKey to (1 to 1),
            PhraseComposerActionId.Backspace to (3 to 1),
            PhraseComposerActionId.Preview to (1 to 3),
            PhraseComposerActionId.Save to (3 to 2),
            PhraseComposerActionId.Back to (2 to 2)
        )
        val findings = expected.mapNotNull { (actionId, sequence) ->
            val assigned = ModeScopedGestureAuthority.phraseComposerCommandSequences[actionId]
            if (assigned != sequence) {
                Finding(
                    "$actionId expected ${formatWinkSequenceShort(sequence.first, sequence.second)} " +
                        "but has ${assigned?.let { formatWinkSequenceShort(it.first, it.second) }}"
                )
            } else {
                null
            }
        }
        return findings + if (ModeScopedGestureAuthority.globalGestures[
                EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
            ] != GlobalGestureId.Emergency
        ) {
            listOf(Finding("Emergency must remain L6 R0"))
        } else {
            emptyList()
        }
    }

    private fun auditGlobalGesturesAlwaysActive(): List<Finding> {
        val required = listOf(
            GlobalGestureId.Emergency,
            GlobalGestureId.Back,
            GlobalGestureId.PreviousPage,
            GlobalGestureId.NextPage
        )
        val present = ModeScopedGestureAuthority.globalGestures.values.toSet()
        val missing = required.filter { it !in present }
        return if (missing.isEmpty()) emptyList() else {
            listOf(Finding("Missing global gestures: $missing"))
        }
    }

    private fun auditNoSameTierAmbiguityWithinMode(): List<Finding> {
        val findings = mutableListOf<Finding>()
        ModeScopedGestureAuthority.allRegisteredModes().forEach { mode ->
            val bindings = ModeScopedGestureAuthority.namespaceFor(mode)
            val byTier = bindings.groupBy { it.tier }
            byTier.forEach { (tier, tierBindings) ->
                val dupes = tierBindings.groupBy { it.left to it.right }.filter { it.value.size > 1 }
                if (dupes.isNotEmpty()) {
                    findings.add(Finding("Ambiguous $tier gestures in $mode: ${dupes.keys}"))
                }
            }
        }
        return findings
    }

    /**
     * Sequences may repeat across modes (MSGA principle). This audit verifies that overlap
     * exists only across different modes, never as same-tier ambiguity within one mode.
     */
    private fun auditCrossModeOverlapIsModeScoped(): List<Finding> {
        val comm = ModeScopedGestureAuthority.communicationVocabularyBindings()
            .map { it.left to it.right }
            .toSet()
        val composerCommands = ModeScopedGestureAuthority.phraseComposerKeyboardCommandBindings()
            .map { it.left to it.right }
            .toSet()
        val overlap = comm.intersect(composerCommands)
        return if (overlap.isEmpty()) {
            listOf(Finding("Expected cross-mode slot reuse between Communication and Composer"))
        } else {
            emptyList()
        }
    }

    private fun auditKeyboardCommandsIgnoredOutsideComposer(): List<Finding> {
        val composerContext = LisaGestureContext(
            activePanel = LisaPanel.None,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        val moveLeftSeq = ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.MoveLeft)
        val target = ModeScopedGestureAuthority.routingTarget(
            composerContext,
            moveLeftSeq.first,
            moveLeftSeq.second
        )
        return if (target == GestureRoutingTarget.PhraseComposer) {
            listOf(Finding("Communication context must not route to PhraseComposer"))
        } else {
            emptyList()
        }
    }
}
