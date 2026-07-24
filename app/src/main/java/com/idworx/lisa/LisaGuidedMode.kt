package com.idworx.lisa

/**
 * Guided Navigation Architecture V1 — Communication Workspace layer.
 *
 * Workspace global gestures: L2 R0, L0 R2, L1 R1 (select), L2 R2 (back), L3 R0 (categories), L6 R0.
 * L0 R3 finishes Guided Training and returns to normal communication — no touch required.
 * Brain 1 decision gestures (L2/R2 choice, L1 R1 confirm, R1 L1 cancel) apply only outside
 * workspace vocabulary contexts — see [com.idworx.lisa.features.experiencepolish.communicationworkspace.WorkspaceGestureLayers].
 * Adjustment-only gestures: L3 R1 decrease, L1 R3 increase.
 * Vocabulary uses short reusable page-local slots from [GuidedPageSequences].
 */
object GuidedModeNavigation {
    const val PREVIOUS_LEFT = 2
    const val PREVIOUS_RIGHT = 0
    const val NEXT_LEFT = 0
    const val NEXT_RIGHT = 2
    const val SELECT_LEFT = 1
    const val SELECT_RIGHT = 1
    const val BACK_LEFT = 2
    const val BACK_RIGHT = 2

    /**
     * Choose Category — one of the most frequently used actions in the app, so it is kept as
     * short as possible. Previously L4 R4 (8 total winks); shortened to L3 R0 (3 total winks).
     * Free of every global-nav, emergency, and vocabulary-slot gesture — see
     * [GuidedPageSequences.forbiddenForVocabulary].
     */
    const val CATEGORIES_LEFT = 3
    const val CATEGORIES_RIGHT = 0

    /**
     * Finish Training — the gesture-based, touch-independent way to end Guided Training and
     * return to normal communication mode. Mirrors the real workspace Reset action; see
     * [com.idworx.lisa.features.onboardingguide.model.NavigationAction.ResetSequence] and
     * [performReset]-style handling in MainActivity's gesture dispatch.
     */
    const val FINISH_TRAINING_LEFT = 0
    const val FINISH_TRAINING_RIGHT = 3

    const val DECREASE_VALUE_LEFT = 3
    const val DECREASE_VALUE_RIGHT = 1
    const val INCREASE_VALUE_LEFT = 1
    const val INCREASE_VALUE_RIGHT = 3

    /**
     * RC7D.25 — Adjust Settings sub-mode entry gesture. This is the single globally-unique command
     * that opens the blink-accessible "Adjust Settings" sub-mode from the shared top control area.
     *
     * Why L5 R5 is safe:
     *  • Globally unique — no communication phrase, navigation, system, practice, category-shortcut
     *    or vocabulary slot uses (5,5); the low-left/low-right space is fully occupied but nothing
     *    exists at 5×5.
     *  • Emergency-safe — the left count (5) is below Emergency's six, so the sequence can never
     *    pass through or share Emergency's complete L6 R0 prefix, and Emergency keeps global priority
     *    (it is matched first, upstream of this controller).
     *  • Not a prefix of any existing gesture — nothing exists "up and to the right" of (5,5), so it
     *    can never be shadowed by a longer sequence.
     *  • Shorter existing sequences ARE prefixes of it, so it is registered for explicit continuation
     *    protection (see MainActivity.mappingsForSequenceContinuation): the idle finalize gate plus
     *    the continuation hint keep those shorter prefixes from firing while the user is still
     *    winking toward the full 5×5 entry.
     *  • Balanced both-eye use (five left + five right) with only five winks per eye — every count
     *    is within the ranges the detector already handles for existing gestures.
     */
    const val ADJUST_SETTINGS_ENTRY_LEFT = 5
    const val ADJUST_SETTINGS_ENTRY_RIGHT = 5

    fun isAdjustSettingsEntrySequence(left: Int, right: Int): Boolean =
        left == ADJUST_SETTINGS_ENTRY_LEFT && right == ADJUST_SETTINGS_ENTRY_RIGHT

    /**
     * RC7D.28 — Open Communication workspace Menu (LisaPanel.Menu). Chosen after a full registry
     * audit: every both-eye sequence with total wink count ≤ 8 is already reserved, and every
     * shorter free option either collides or crosses Emergency's L6 R0 corridor. L4 R6 is the
     * shortest practical globally-unique both-eye sequence that:
     *  • collides with no nav / slot / vocabulary / system / composer / practice gesture
     *  • keeps left count (4) below Emergency (6)
     *  • is not dominated by a longer reserved sequence
     *  • needs continuation protection because shorter L4 R* prefixes exist (same pattern as L5 R5)
     *
     * Constants also alias [OPEN_MAIN_MENU_LEFT_WINKS] / [OPEN_MAIN_MENU_RIGHT_WINKS].
     */
    const val OPEN_MAIN_MENU_LEFT = 4
    const val OPEN_MAIN_MENU_RIGHT = 6
    const val OPEN_MAIN_MENU_LEFT_WINKS = OPEN_MAIN_MENU_LEFT
    const val OPEN_MAIN_MENU_RIGHT_WINKS = OPEN_MAIN_MENU_RIGHT

    fun isOpenMainMenuSequence(left: Int, right: Int): Boolean =
        left == OPEN_MAIN_MENU_LEFT && right == OPEN_MAIN_MENU_RIGHT

    /**
     * Category-page jump shortcuts (RC7D.20) — active only in Category Menu Mode. These move the
     * highlighted category directly between whole category pages, unlike the item-by-item
     * [PREVIOUS_LEFT]/[NEXT_LEFT] scroll gestures which stay one category at a time. Deliberately
     * distinct from the constitutional global-nav and emergency sequences, and from every
     * category-shortcut/vocabulary slot in [GuidedPageSequences], so they never shadow or collide.
     */
    const val PREVIOUS_CATEGORY_PAGE_LEFT = 4
    const val PREVIOUS_CATEGORY_PAGE_RIGHT = 0
    const val NEXT_CATEGORY_PAGE_LEFT = 0
    const val NEXT_CATEGORY_PAGE_RIGHT = 4

    fun isPreviousSequence(left: Int, right: Int): Boolean =
        left == PREVIOUS_LEFT && right == PREVIOUS_RIGHT

    fun isNextSequence(left: Int, right: Int): Boolean =
        left == NEXT_LEFT && right == NEXT_RIGHT

    fun isSelectSequence(left: Int, right: Int): Boolean =
        left == SELECT_LEFT && right == SELECT_RIGHT

    /**
     * RC7D.27 — L1 R1 confirm (left-then-right order). Counts alone cannot distinguish this from
     * [isConfirmCancelSequence]; callers must pass [blinkOrder] when both are meaningful.
     */
    fun isConfirmSequence(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        left == SELECT_LEFT && right == SELECT_RIGHT &&
            com.idworx.lisa.features.brain1interactionstandard.model.BlinkSequenceOrder.isLeftThenRight(blinkOrder)

    /** RC7D.27 — R1 L1 cancel-confirmation (right-then-left order). */
    fun isConfirmCancelSequence(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        left == SELECT_LEFT && right == SELECT_RIGHT &&
            com.idworx.lisa.features.brain1interactionstandard.model.BlinkSequenceOrder.isRightThenLeft(blinkOrder)

    fun isBackSequence(left: Int, right: Int): Boolean =
        left == BACK_LEFT && right == BACK_RIGHT

    fun isCategoriesSequence(left: Int, right: Int): Boolean =
        left == CATEGORIES_LEFT && right == CATEGORIES_RIGHT

    fun isFinishTrainingSequence(left: Int, right: Int): Boolean =
        left == FINISH_TRAINING_LEFT && right == FINISH_TRAINING_RIGHT

    fun isDecreaseValueSequence(left: Int, right: Int): Boolean =
        left == DECREASE_VALUE_LEFT && right == DECREASE_VALUE_RIGHT

    fun isIncreaseValueSequence(left: Int, right: Int): Boolean =
        left == INCREASE_VALUE_LEFT && right == INCREASE_VALUE_RIGHT

    fun isPreviousCategoryPageSequence(left: Int, right: Int): Boolean =
        left == PREVIOUS_CATEGORY_PAGE_LEFT && right == PREVIOUS_CATEGORY_PAGE_RIGHT

    fun isNextCategoryPageSequence(left: Int, right: Int): Boolean =
        left == NEXT_CATEGORY_PAGE_LEFT && right == NEXT_CATEGORY_PAGE_RIGHT

    fun isGlobalNavigationSequence(left: Int, right: Int): Boolean =
        isPreviousSequence(left, right) ||
            isNextSequence(left, right) ||
            isSelectSequence(left, right) ||
            isBackSequence(left, right) ||
            isCategoriesSequence(left, right) ||
            isFinishTrainingSequence(left, right)

    /** @deprecated Use [isPreviousSequence] */
    fun isPreviousPageSequence(left: Int, right: Int): Boolean = isPreviousSequence(left, right)

    /** @deprecated Use [isNextSequence] */
    fun isNextPageSequence(left: Int, right: Int): Boolean = isNextSequence(left, right)

    /** @deprecated Use [isGlobalNavigationSequence] */
    fun isNavigationSequence(left: Int, right: Int): Boolean =
        isPreviousSequence(left, right) || isNextSequence(left, right)
}

/** Reusable per-page vocabulary sequences — same slots on every category page. */
object GuidedPageSequences {
    val slots: List<Pair<Int, Int>> = listOf(
        2 to 1,
        1 to 2,
        3 to 1,
        1 to 3,
        3 to 2,
        2 to 3,
        3 to 3,
        4 to 1,
        1 to 4,
        4 to 2
    )

    fun leftAt(slotIndex: Int): Int = slotAt(slotIndex).first
    fun rightAt(slotIndex: Int): Int = slotAt(slotIndex).second

    fun slotAt(slotIndex: Int): Pair<Int, Int> =
        if (slotIndex < slots.size) {
            slots[slotIndex]
        } else {
            extendedSlots[slotIndex - slots.size]
        }

    /** Extra short sequences for categories with more than [slots] entries (e.g. Preferences). */
    val extendedSlots: List<Pair<Int, Int>> = listOf(
        5 to 1,
        1 to 5,
        5 to 2,
        2 to 4,
        4 to 3,
        3 to 4,
        5 to 3,
        1 to 6,
        6 to 1,
        2 to 5
    )

    fun allSlotsForEntryCount(count: Int): List<Pair<Int, Int>> =
        (0 until count).map { slotAt(it) }

    val forbiddenForVocabulary: Set<Pair<Int, Int>> = setOf(
        1 to 0,
        0 to 1,
        GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
        GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
        GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
        GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
        GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
        GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT,
        GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT,
        GuidedModeNavigation.OPEN_MAIN_MENU_LEFT to GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT,
        EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
    )
}

/** Direct gesture shortcuts for the top-level categories in Category Menu mode (one per page). */
object GuidedCategoryShortcuts {
    const val SHORTCUT_COUNT: Int = GuidedVocabularyCategory.PAGE_COUNT

    fun gestureForCategory(categoryIndex: Int): Pair<Int, Int> {
        val index = categoryIndex.coerceIn(0, SHORTCUT_COUNT - 1)
        // RC7D.26 — Adjust Settings always uses the canonical L5 R5 entry sequence (not the
        // auto-assigned page slot), so the Category Menu card and the global blink shortcut stay one.
        if (index == GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX) {
            return GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT
        }
        return GuidedPageSequences.slotAt(index)
    }

    fun sequenceLabelForCategory(categoryIndex: Int): String {
        val (left, right) = gestureForCategory(categoryIndex)
        return formatWinkSequenceShort(left, right)
    }

    fun categoryIndexForGesture(left: Int, right: Int): Int? {
        for (index in 0 until SHORTCUT_COUNT) {
            val (slotLeft, slotRight) = gestureForCategory(index)
            if (slotLeft == left && slotRight == right) return index
        }
        return null
    }

    fun allGestures(): List<Pair<Int, Int>> =
        (0 until SHORTCUT_COUNT).map { gestureForCategory(it) }

    fun doNotConflictWithGlobalNavigation(): Boolean =
        allGestures().none { (left, right) ->
            GuidedModeNavigation.isGlobalNavigationSequence(left, right) ||
                isEmergencySequence(left, right)
        }
}

enum class GuidedOverlayScreenMode {
    Vocabulary,
    CategoryMenu
}

enum class GuidedPreferencesAdjustMode {
    None,
    /** Settings & Controls hub (formerly Adjust Settings menu). */
    SettingsMenu,
    ResponseTime,
    Sensitivity,
    SpeechVolume,
    SpeechSpeed,
    /** Listening pause/resume — state control, not a numeric adjustment. */
    Listening,
    /** RC7D.27 — save confirmation for the Sensitivity draft (not yet persisted). */
    ConfirmSaveSensitivity,
    /** RC7D.27 — save confirmation for the Response Time draft (not yet persisted). */
    ConfirmSaveResponseTime,
    ConfirmSaveSpeechVolume,
    ConfirmSaveSpeechSpeed
}

/**
 * RC7D.25 — which setting the Adjust Settings sub-mode currently highlights and will open. Selection
 * starts on [Sensitivity]; L2 R0 / L0 R2 move between the two without changing either value.
 */
enum class GuidedAdjustmentTarget {
    Sensitivity,
    ResponseTime
}

enum class GuidedVocabularyCategory {
    Conversation,
    BasicNeeds,
    Medical,
    Family,
    /**
     * Legacy enum member retained for storage/migration compatibility only.
     * Controls were migrated into Settings & Controls; excluded from [ordered].
     */
    BasicSystemControls,
    /**
     * RC8.5 — removed from the Communication category menu.
     * Sensitivity / Response Time live only under Settings & Controls.
     * Retained for migration and validation compatibility.
     */
    Preferences,
    Custom,
    /** Management destination — not a communication phrase category. */
    PhraseManagement,
    /**
     * RC7D.26 — navigation destination for Settings & Controls (formerly Adjust Settings).
     * Not a phrase category; never used for saved-phrase assignment or storage keys.
     */
    AdjustSettings;

    companion object {
        /**
         * Visible category-menu destinations. Basic System Controls and Preferences are omitted —
         * Settings & Controls is the single Communication-facing settings destination (RC8.5).
         */
        val ordered: List<GuidedVocabularyCategory> = entries.filter {
            it != BasicSystemControls && it != Preferences
        }
        const val PAGE_COUNT: Int = 7
        const val STANDARD_ENTRIES_PER_PAGE: Int = 10
        /** Legacy pre-RC8.5 index of Preferences (no longer present in [ordered]). */
        const val LEGACY_PREFERENCES_CATEGORY_INDEX: Int = 4
        @Deprecated(
            "RC8.5 — Preferences removed from Communication; use Settings & Controls",
            ReplaceWith("GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX")
        )
        const val PREFERENCES_CATEGORY_INDEX: Int = LEGACY_PREFERENCES_CATEGORY_INDEX
        const val CUSTOM_CATEGORY_INDEX: Int = 4
        const val PHRASE_MANAGEMENT_INDEX: Int = 5
        const val ADJUST_SETTINGS_INDEX: Int = 6

        /**
         * Remaps a pre-RC8.5 category index (Preferences at 4) into the current [ordered] list.
         * Preferences lands on Category Selection (caller should force CategoryMenu).
         */
        fun migrateCategoryIndexFromPreRc85(index: Int): Int = when (index) {
            in 0..3 -> index
            LEGACY_PREFERENCES_CATEGORY_INDEX -> 0
            5 -> CUSTOM_CATEGORY_INDEX
            6 -> PHRASE_MANAGEMENT_INDEX
            7 -> ADJUST_SETTINGS_INDEX
            else -> index.coerceIn(0, PAGE_COUNT - 1)
        }

        fun wasLegacyPreferencesIndex(index: Int): Boolean =
            index == LEGACY_PREFERENCES_CATEGORY_INDEX
    }
}

/**
 * Categories-area destinations: assignable communication categories vs management entry points.
 * Phrase Management and Adjust Settings must never be treated as phrase storage/assignment categories.
 */
sealed class CategoryAreaDestination {
    data class CommunicationCategory(val category: GuidedVocabularyCategory) : CategoryAreaDestination()
    data object CreateCustomPhrase : CategoryAreaDestination()
    data object PhraseManagement : CategoryAreaDestination()
    /** RC7D.26 — opens the canonical Adjust Settings sub-mode (same as L5 R5). */
    data object AdjustSettings : CategoryAreaDestination()

    companion object {
        fun forCategoryIndex(index: Int): CategoryAreaDestination =
            when (val category = GuidedVocabularyCategory.ordered.getOrNull(index)) {
                GuidedVocabularyCategory.PhraseManagement -> PhraseManagement
                GuidedVocabularyCategory.AdjustSettings -> AdjustSettings
                GuidedVocabularyCategory.Custom -> CreateCustomPhrase
                GuidedVocabularyCategory.Preferences -> AdjustSettings
                null -> CommunicationCategory(GuidedVocabularyCategory.Conversation)
                else -> CommunicationCategory(category)
            }

        fun isAssignableCommunicationCategory(category: GuidedVocabularyCategory): Boolean =
            category.toCaregiverCategory() != null &&
                category != GuidedVocabularyCategory.Custom &&
                category != GuidedVocabularyCategory.PhraseManagement &&
                category != GuidedVocabularyCategory.AdjustSettings &&
                category != GuidedVocabularyCategory.Preferences

        fun isManagementDestination(category: GuidedVocabularyCategory): Boolean =
            category == GuidedVocabularyCategory.PhraseManagement ||
                category == GuidedVocabularyCategory.AdjustSettings
    }
}

enum class GuidedEntryKind {
    SpeakPhrase,
    SystemAction
}

enum class GuidedOverlayAction {
    RepeatLastPhrase,
    DecreaseSensitivity,
    IncreaseSensitivity,
    SetSpeedFast,
    SetSpeedSlow,
    TogglePauseListening,
    OpenMenu,
    ResetSequence,
    ShowHelp,
    ShowCurrentResponseTime,
    ShowCurrentSensitivity,
    OpenAdjustResponseTime,
    OpenAdjustSensitivity,
    OpenPhraseComposer
}

/**
 * RC7D.22 — why the Category Menu navigation state last changed, so the single Compose scroll
 * coordinator can pick the right behaviour. PAGE_MOVEMENT scrolls straight to a viewport-page
 * anchor and deliberately skips the RC7D.21 selection-centring path; every other cause reveals /
 * centres the selected row. This replaces fragile "suppress one effect after another" delays with
 * explicit intent carried in the state itself.
 */
enum class CategoryNavigationCause {
    ITEM_MOVEMENT,
    PAGE_MOVEMENT,
    DIRECT_SHORTCUT,
    MENU_RESTORE,
    TOUCH_SELECTION
}

data class GuidedNavigationState(
    /** RC8.1 — Communication workspace lands on Category Selection, not an opened category. */
    val screenMode: GuidedOverlayScreenMode = GuidedOverlayScreenMode.CategoryMenu,
    val categoryIndex: Int = 0,
    val categoryMenuSelection: Int = 0,
    val phrasePageIndex: Int = 0,
    val preferencesAdjustMode: GuidedPreferencesAdjustMode = GuidedPreferencesAdjustMode.None,
    val draftResponseTimeSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val draftSensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    val draftSpeechVolumeLevel: Int = SpeechVolumeAuthority.DEFAULT_LEVEL,
    val draftSpeechSpeedLevel: Int = SpeechSpeedAuthority.DEFAULT_LEVEL,
    /** RC7D.27 — value captured when the current adjustment opened (for confirmation + cancel restore). */
    val adjustmentOriginalSensitivity: Int = DEFAULT_SENSITIVITY_LEVEL,
    val adjustmentOriginalResponseTimeSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val adjustmentOriginalSpeechVolumeLevel: Int = SpeechVolumeAuthority.DEFAULT_LEVEL,
    val adjustmentOriginalSpeechSpeedLevel: Int = SpeechSpeedAuthority.DEFAULT_LEVEL,
    val adjustmentScrollStep: Int = 0,
    /** Selection index within the Settings & Controls hub (0 = Sensitivity … 3 = Speech Speed). */
    val settingsHubSelection: Int = 0,
    // RC7D.22 — explicit category-list VIEWPORT page, independent of which category is selected.
    // The viewport page is what the caregiver perceives as "Page 1 / Page 2" (a scroll window),
    // never selectionIndex / pageSize. [categoryViewportPageCount] is measured by the Compose layer
    // from the real viewport + content height and pushed back into state so both the header and the
    // controller's page-nav gating read one canonical source of truth.
    val categoryViewportPage: Int = 0,
    val categoryViewportPageCount: Int = 1,
    val categoryNavigationCause: CategoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
) {
    fun normalized(): GuidedNavigationState {
        val maxIndex = GuidedVocabularyCategory.PAGE_COUNT - 1
        // RC8.5 — remap only out-of-range pre-RC8.5 indices (e.g. old Adjust Settings at 7).
        val remappedIndex = if (categoryIndex > maxIndex) {
            GuidedVocabularyCategory.migrateCategoryIndexFromPreRc85(categoryIndex)
        } else {
            categoryIndex.coerceIn(0, maxIndex)
        }
        val remappedMenuSelection = if (categoryMenuSelection > maxIndex) {
            GuidedVocabularyCategory.migrateCategoryIndexFromPreRc85(categoryMenuSelection)
        } else {
            categoryMenuSelection.coerceIn(0, maxIndex)
        }
        return copy(
            categoryIndex = remappedIndex,
            categoryMenuSelection = remappedMenuSelection,
            phrasePageIndex = phrasePageIndex.coerceAtLeast(0),
            draftResponseTimeSec = SequenceProcessingDelay.coerce(draftResponseTimeSec),
            draftSensitivityLevel = draftSensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL),
            draftSpeechVolumeLevel = SpeechVolumeAuthority.coerce(draftSpeechVolumeLevel),
            draftSpeechSpeedLevel = SpeechSpeedAuthority.coerce(draftSpeechSpeedLevel),
            adjustmentOriginalSensitivity = adjustmentOriginalSensitivity.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL),
            adjustmentOriginalResponseTimeSec = SequenceProcessingDelay.coerce(adjustmentOriginalResponseTimeSec),
            adjustmentOriginalSpeechVolumeLevel = SpeechVolumeAuthority.coerce(adjustmentOriginalSpeechVolumeLevel),
            adjustmentOriginalSpeechSpeedLevel = SpeechSpeedAuthority.coerce(adjustmentOriginalSpeechSpeedLevel),
            adjustmentScrollStep = adjustmentScrollStep.coerceAtLeast(0),
            settingsHubSelection = settingsHubSelection.coerceIn(
                0,
                (SettingsAndControlsHubSequences.HUB_SETTING_KINDS.size - 1).coerceAtLeast(0)
            ),
            categoryViewportPageCount = categoryViewportPageCount.coerceAtLeast(1),
            categoryViewportPage = categoryViewportPage.coerceIn(0, (categoryViewportPageCount.coerceAtLeast(1) - 1))
        )
    }

    val isPreferencesAdjustmentActive: Boolean
        get() = preferencesAdjustMode != GuidedPreferencesAdjustMode.None

    /** Settings & Controls hub (level 1) — no value changing yet. */
    val isSettingsMenuActive: Boolean
        get() = preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu

    /** Level 2: a specific numeric setting is open and its draft value is being adjusted. */
    val isValueAdjustmentActive: Boolean
        get() = preferencesAdjustMode == GuidedPreferencesAdjustMode.ResponseTime ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.SpeechVolume ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.SpeechSpeed

    val isListeningControlActive: Boolean
        get() = preferencesAdjustMode == GuidedPreferencesAdjustMode.Listening

    /** RC7D.27 — save confirmation is showing (draft not yet persisted). */
    val isSaveConfirmationActive: Boolean
        get() = preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveSensitivity ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveResponseTime ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed

    fun displayResponseTimeSec(savedSec: Int): Int = when (preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ResponseTime,
        GuidedPreferencesAdjustMode.ConfirmSaveResponseTime ->
            SequenceProcessingDelay.coerce(draftResponseTimeSec)
        else -> SequenceProcessingDelay.coerce(savedSec)
    }

    fun displaySensitivityLevel(savedLevel: Int): Int = when (preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.Sensitivity,
        GuidedPreferencesAdjustMode.ConfirmSaveSensitivity ->
            draftSensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        else -> savedLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
    }

    fun displaySpeechVolumeLevel(savedLevel: Int): Int = when (preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.SpeechVolume,
        GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume ->
            SpeechVolumeAuthority.coerce(draftSpeechVolumeLevel)
        else -> SpeechVolumeAuthority.coerce(savedLevel)
    }

    fun displaySpeechSpeedLevel(savedLevel: Int): Int = when (preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.SpeechSpeed,
        GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed ->
            SpeechSpeedAuthority.coerce(draftSpeechSpeedLevel)
        else -> SpeechSpeedAuthority.coerce(savedLevel)
    }
}

data class GuidedVocabularyEntry(
    val left: Int,
    val right: Int,
    val phrase: String,
    val englishSubtitle: String? = null,
    val kind: GuidedEntryKind = GuidedEntryKind.SpeakPhrase,
    val systemAction: SystemCommandAction? = null,
    val guidedAction: GuidedOverlayAction? = null,
    val preferenceValue: Int? = null,
    val slotIndex: Int = 0
) {
    val sequenceLabel: String get() = formatWinkSequenceShort(left, right)
}

data class GuidedCategoryPage(
    val category: GuidedVocabularyCategory,
    val title: String,
    val entries: List<GuidedVocabularyEntry>
)

/**
 * Stable identity of a guided navigation-panel action, so touch handler / enabled / highlight
 * wiring is keyed by meaning rather than a fragile list position — this lets the Category Menu
 * panel carry extra page-navigation buttons (RC7D.20) without disturbing the Vocabulary /
 * Adjustment panel ordering other auditors depend on.
 */
enum class GuidedPanelActionKind {
    ScrollUp,
    ScrollDown,
    PreviousCategoryPage,
    NextCategoryPage,
    Select,
    Back,
    Categories,
    Emergency
}

data class GuidedNavPanelAction(
    val symbol: String,
    val title: String,
    val gestureHint: String,
    val sequenceLabel: String,
    val kind: GuidedPanelActionKind = GuidedPanelActionKind.Select
)

object GuidedNavigationPanelSpec {

    enum class PanelContext {
        Vocabulary,
        CategoryMenu,
        Adjustment,
        /** Settings & Controls hub — Scroll Up/Down move selection; Select opens the highlighted setting. */
        SettingsHub
    }

    fun panelActions(uiStrings: LisaUiStrings, context: PanelContext): List<GuidedNavPanelAction> {
        val scrollUpTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedPreviousPhrasePage
            PanelContext.CategoryMenu -> uiStrings.guidedMoveUpCategory
            PanelContext.Adjustment -> uiStrings.guidedScrollUp
            PanelContext.SettingsHub -> uiStrings.guidedScrollUp
        }
        val selectTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedSelectEnter
            PanelContext.CategoryMenu -> uiStrings.guidedOpenSelectedCategory
            PanelContext.Adjustment -> uiStrings.guidedSaveSelectedValue
            PanelContext.SettingsHub -> uiStrings.guidedOpenSelectedSetting
        }
        val backTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedBack
            PanelContext.CategoryMenu -> uiStrings.guidedBackToPhrases
            PanelContext.Adjustment -> uiStrings.guidedCancelAdjustment
            PanelContext.SettingsHub -> uiStrings.guidedBack
        }
        val scrollDownTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedNextPhrasePage
            PanelContext.CategoryMenu -> uiStrings.guidedMoveDownCategory
            PanelContext.Adjustment -> uiStrings.guidedScrollDown
            PanelContext.SettingsHub -> uiStrings.guidedScrollDown
        }
        // Every sequenceLabel is derived directly from the same GuidedModeNavigation/emergency
        // constants the real gesture handlers check against — never a separately hardcoded copy —
        // so this panel can never drift out of sync with what a gesture actually does.
        val scrollUp = GuidedNavPanelAction(
            "↑↑", scrollUpTitle, uiStrings.guidedScrollUpHint,
            formatWinkSequenceShort(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT),
            GuidedPanelActionKind.ScrollUp
        )
        val scrollDown = GuidedNavPanelAction(
            "↓↓", scrollDownTitle, uiStrings.guidedScrollDownHint,
            formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT),
            GuidedPanelActionKind.ScrollDown
        )
        val select = GuidedNavPanelAction(
            "✅", selectTitle, uiStrings.guidedSelectEnterHint,
            formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT),
            GuidedPanelActionKind.Select
        )
        val back = GuidedNavPanelAction(
            "↩", backTitle, uiStrings.guidedBackHint,
            formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT),
            GuidedPanelActionKind.Back
        )
        val categories = GuidedNavPanelAction(
            "☰", uiStrings.guidedCategoriesNavTitle, uiStrings.guidedCategoriesNavHint,
            formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT),
            GuidedPanelActionKind.Categories
        )
        val emergency = GuidedNavPanelAction(
            "🚨", uiStrings.guidedEmergencyNavTitle, uiStrings.guidedEmergencyNavHint,
            formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            GuidedPanelActionKind.Emergency
        )
        // RC7D.20 — the Category Menu carries whole-page jump shortcuts alongside the item-by-item
        // Move Up / Move Down. Categories (☰) is dropped here (you are already in the menu) so the
        // narrow panel has room for both without hiding item navigation.
        if (context == PanelContext.CategoryMenu) {
            val previousPage = GuidedNavPanelAction(
                "⏮", uiStrings.guidedPreviousCategoryPage, uiStrings.guidedPreviousCategoryPageHint,
                formatWinkSequenceShort(
                    GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                    GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
                ),
                GuidedPanelActionKind.PreviousCategoryPage
            )
            val nextPage = GuidedNavPanelAction(
                "⏭", uiStrings.guidedNextCategoryPage, uiStrings.guidedNextCategoryPageHint,
                formatWinkSequenceShort(
                    GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                    GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
                ),
                GuidedPanelActionKind.NextCategoryPage
            )
            return listOf(scrollUp, scrollDown, previousPage, nextPage, select, back, emergency)
        }
        // Vocabulary / Adjustment order is unchanged (index 0 Previous … 5 Next) — other auditors
        // rely on this positional layout.
        return listOf(scrollUp, select, back, categories, emergency, scrollDown)
    }

    fun vocabularyModeActions(uiStrings: LisaUiStrings): List<GuidedNavPanelAction> =
        panelActions(uiStrings, PanelContext.Vocabulary)

    fun categoryMenuModeActions(uiStrings: LisaUiStrings): List<GuidedNavPanelAction> =
        panelActions(uiStrings, PanelContext.CategoryMenu)

    fun adjustmentModeActions(uiStrings: LisaUiStrings): List<GuidedNavPanelAction> =
        panelActions(uiStrings, PanelContext.Adjustment)

    fun allActionsLabeled(actions: List<GuidedNavPanelAction>): Boolean =
        actions.all { action ->
            action.title.isNotBlank() &&
                action.gestureHint.isNotBlank() &&
                action.sequenceLabel.isNotBlank() &&
                action.symbol.isNotBlank()
        }
}

/** Documents touch targets that must mirror eye-gesture handlers in the guided overlay UI. */
object GuidedTouchNavigationSpec {
    val panelGestures: List<Pair<Int, Int>> = listOf(
        GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
        GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
        GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
        GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
        EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
        GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
        // RC7D.20 — Category Menu whole-page jumps, touch-mirrored like every other panel button.
        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
        GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
        // Finish Training mirrors the bottom-bar Reset touch button — see performReset() in MainActivity.
        GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
    )

    val adjustmentPanelGestures: List<Pair<Int, Int>> = listOf(
        GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT,
        GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT
    ) + panelGestures

    fun touchMirrorsEyeGesture(left: Int, right: Int): Boolean =
        panelGestures.any { it.first == left && it.second == right } ||
            adjustmentPanelGestures.any { it.first == left && it.second == right } ||
            GuidedPageSequences.slots.any { it.first == left && it.second == right } ||
            GuidedPageSequences.extendedSlots.any { it.first == left && it.second == right }
}

data class GuidedCatalogContext(
    val responseTimeSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val sensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    val speechVolumeLevel: Int = SpeechVolumeAuthority.DEFAULT_LEVEL,
    val speechSpeedLevel: Int = SpeechSpeedAuthority.DEFAULT_LEVEL,
    val listeningPaused: Boolean = false,
    val caregiverCustomPhrases: List<CustomPhraseEngine.CaregiverCustomPhraseEntry> = emptyList()
)

sealed class GuidedSequenceResult {
    data class Navigate(val newState: GuidedNavigationState) : GuidedSequenceResult()
    data class Speak(val entry: GuidedVocabularyEntry) : GuidedSequenceResult()
    data class SystemAction(val entry: GuidedVocabularyEntry) : GuidedSequenceResult()
    /** Hub / Listening direct actions that are not vocabulary phrase entries. */
    data class SettingsControlAction(val kind: SettingsControlKind) : GuidedSequenceResult()
    data class SavePreferencesAdjustment(
        val newState: GuidedNavigationState,
        val responseTimeSec: Int? = null,
        val sensitivityLevel: Int? = null,
        val speechVolumeLevel: Int? = null,
        val speechSpeedLevel: Int? = null
    ) : GuidedSequenceResult()
    object Unmatched : GuidedSequenceResult()
}

object PreferenceAdjustmentBarSpec {
    fun responseTimeValues(): List<Int> = SequenceProcessingDelay.allowedSeconds.toList()

    fun sensitivityValues(): List<Int> = (MIN_SENSITIVITY_LEVEL..MAX_SENSITIVITY_LEVEL).toList()

    fun formatResponseTimeTick(seconds: Int): String = "${seconds}s"

    fun formatSensitivityTick(level: Int): String = level.toString()

    fun isHighlighted(tickValue: Int, draftValue: Int): Boolean = tickValue == draftValue
}

object PreferenceAdjustmentController {

    /**
     * RC8.5 — collapses a restored/stale Preferences destination into Category Selection
     * (or Settings & Controls when [preferSettings] is true).
     */
    fun recoverFromRemovedPreferences(
        state: GuidedNavigationState,
        preferSettings: Boolean = false
    ): GuidedNavigationState {
        val normalized = state.normalized()
        return if (preferSettings) {
            openSettingsMenu(normalized)
        } else {
            GuidedNavigationController.openCategoryMenu(
                normalized.copy(categoryIndex = 0, categoryMenuSelection = 0)
            )
        }
    }

    /**
     * Open the Settings & Controls hub. Does not touch stored values. The underlying screen
     * (Vocabulary / Category Menu) is preserved so backing out of the menu returns there.
     */
    fun openSettingsMenu(state: GuidedNavigationState): GuidedNavigationState =
        state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.SettingsMenu,
            adjustmentScrollStep = 0,
            settingsHubSelection = 0
        )

    fun openResponseTimeAdjust(state: GuidedNavigationState, currentSec: Int): GuidedNavigationState {
        val coerced = SequenceProcessingDelay.coerce(currentSec)
        return state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
            draftResponseTimeSec = coerced,
            adjustmentOriginalResponseTimeSec = coerced,
            adjustmentScrollStep = 0
        )
    }

    fun openSensitivityAdjust(state: GuidedNavigationState, currentLevel: Int): GuidedNavigationState {
        val coerced = currentLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        return state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity,
            draftSensitivityLevel = coerced,
            adjustmentOriginalSensitivity = coerced,
            adjustmentScrollStep = 0
        )
    }

    fun openSpeechVolumeAdjust(state: GuidedNavigationState, currentLevel: Int): GuidedNavigationState {
        val coerced = SpeechVolumeAuthority.coerce(currentLevel)
        return state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.SpeechVolume,
            draftSpeechVolumeLevel = coerced,
            adjustmentOriginalSpeechVolumeLevel = coerced,
            adjustmentScrollStep = 0
        )
    }

    fun openSpeechSpeedAdjust(state: GuidedNavigationState, currentLevel: Int): GuidedNavigationState {
        val coerced = SpeechSpeedAuthority.coerce(currentLevel)
        return state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.SpeechSpeed,
            draftSpeechSpeedLevel = coerced,
            adjustmentOriginalSpeechSpeedLevel = coerced,
            adjustmentScrollStep = 0
        )
    }

    fun moveHubSelectionUp(state: GuidedNavigationState): GuidedNavigationState {
        val max = SettingsAndControlsHubSequences.HUB_SETTING_KINDS.lastIndex
        return state.copy(
            settingsHubSelection = (state.settingsHubSelection - 1).coerceIn(0, max)
        )
    }

    fun moveHubSelectionDown(state: GuidedNavigationState): GuidedNavigationState {
        val max = SettingsAndControlsHubSequences.HUB_SETTING_KINDS.lastIndex
        return state.copy(
            settingsHubSelection = (state.settingsHubSelection + 1).coerceIn(0, max)
        )
    }

    fun openSelectedHubSetting(
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext
    ): GuidedNavigationState {
        val kind = SettingsAndControlsHubSequences.HUB_SETTING_KINDS
            .getOrElse(state.settingsHubSelection) { SettingsControlKind.Sensitivity }
        return openHubSetting(state, kind, catalogContext)
    }

    fun openHubSetting(
        state: GuidedNavigationState,
        kind: SettingsControlKind,
        catalogContext: GuidedCatalogContext
    ): GuidedNavigationState = when (kind) {
        SettingsControlKind.Sensitivity ->
            openSensitivityAdjust(state, catalogContext.sensitivityLevel)
        SettingsControlKind.ResponseTime ->
            openResponseTimeAdjust(state, catalogContext.responseTimeSec)
        SettingsControlKind.SpeechVolume ->
            openSpeechVolumeAdjust(state, catalogContext.speechVolumeLevel)
        SettingsControlKind.SpeechSpeed ->
            openSpeechSpeedAdjust(state, catalogContext.speechSpeedLevel)
        else -> state
    }

    fun openListeningControl(state: GuidedNavigationState): GuidedNavigationState =
        state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.Listening,
            adjustmentScrollStep = 0
        )

    fun decreaseDraft(state: GuidedNavigationState): GuidedNavigationState = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ResponseTime -> state.copy(
            draftResponseTimeSec = SequenceProcessingDelay.coerce(state.draftResponseTimeSec - 1)
        )
        GuidedPreferencesAdjustMode.Sensitivity -> state.copy(
            draftSensitivityLevel = (state.draftSensitivityLevel - 1)
                .coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        )
        GuidedPreferencesAdjustMode.SpeechVolume -> state.copy(
            draftSpeechVolumeLevel = SpeechVolumeAuthority.coerce(state.draftSpeechVolumeLevel - 1)
        )
        GuidedPreferencesAdjustMode.SpeechSpeed -> state.copy(
            draftSpeechSpeedLevel = SpeechSpeedAuthority.coerce(state.draftSpeechSpeedLevel - 1)
        )
        else -> state
    }

    fun increaseDraft(state: GuidedNavigationState): GuidedNavigationState = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ResponseTime -> state.copy(
            draftResponseTimeSec = SequenceProcessingDelay.coerce(state.draftResponseTimeSec + 1)
        )
        GuidedPreferencesAdjustMode.Sensitivity -> state.copy(
            draftSensitivityLevel = (state.draftSensitivityLevel + 1)
                .coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        )
        GuidedPreferencesAdjustMode.SpeechVolume -> state.copy(
            draftSpeechVolumeLevel = SpeechVolumeAuthority.coerce(state.draftSpeechVolumeLevel + 1)
        )
        GuidedPreferencesAdjustMode.SpeechSpeed -> state.copy(
            draftSpeechSpeedLevel = SpeechSpeedAuthority.coerce(state.draftSpeechSpeedLevel + 1)
        )
        else -> state
    }

    /** RC7D.27 — first Save (L1 R1) enters confirmation; draft is not persisted yet. */
    fun beginSaveConfirmation(state: GuidedNavigationState): GuidedNavigationState = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.Sensitivity -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ConfirmSaveSensitivity
        )
        GuidedPreferencesAdjustMode.ResponseTime -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ConfirmSaveResponseTime
        )
        GuidedPreferencesAdjustMode.SpeechVolume -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume
        )
        GuidedPreferencesAdjustMode.SpeechSpeed -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed
        )
        else -> state
    }

    /** RC7D.27 — R1 L1 leaves confirmation and returns to editing with the draft intact. */
    fun cancelSaveConfirmation(state: GuidedNavigationState): GuidedNavigationState = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ConfirmSaveSensitivity -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity
        )
        GuidedPreferencesAdjustMode.ConfirmSaveResponseTime -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime
        )
        GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.SpeechVolume
        )
        GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed -> state.copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.SpeechSpeed
        )
        else -> state
    }

    /**
     * Exit Settings & Controls entirely (Back from the hub).
     */
    fun exitSettingsMenu(state: GuidedNavigationState): GuidedNavigationState =
        state.copy(preferencesAdjustMode = GuidedPreferencesAdjustMode.None)

    /**
     * RC7D.27 — Cancel / Back from an individual adjustment (or from confirmation via L2 R2 is
     * blocked): discard the draft and return to the Settings & Controls hub.
     */
    fun cancelAdjustment(state: GuidedNavigationState): GuidedNavigationState =
        when (state.preferencesAdjustMode) {
            GuidedPreferencesAdjustMode.SettingsMenu -> exitSettingsMenu(state)
            GuidedPreferencesAdjustMode.None -> state
            else -> openSettingsMenu(state)
        }

    fun saveAdjustment(state: GuidedNavigationState): GuidedSequenceResult.SavePreferencesAdjustment =
        when (state.preferencesAdjustMode) {
            GuidedPreferencesAdjustMode.ResponseTime,
            GuidedPreferencesAdjustMode.ConfirmSaveResponseTime -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = openSettingsMenu(state),
                responseTimeSec = SequenceProcessingDelay.coerce(state.draftResponseTimeSec)
            )
            GuidedPreferencesAdjustMode.Sensitivity,
            GuidedPreferencesAdjustMode.ConfirmSaveSensitivity -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = openSettingsMenu(state),
                sensitivityLevel = state.draftSensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
            )
            GuidedPreferencesAdjustMode.SpeechVolume,
            GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = openSettingsMenu(state),
                speechVolumeLevel = SpeechVolumeAuthority.coerce(state.draftSpeechVolumeLevel)
            )
            GuidedPreferencesAdjustMode.SpeechSpeed,
            GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = openSettingsMenu(state),
                speechSpeedLevel = SpeechSpeedAuthority.coerce(state.draftSpeechSpeedLevel)
            )
            GuidedPreferencesAdjustMode.SettingsMenu,
            GuidedPreferencesAdjustMode.Listening,
            GuidedPreferencesAdjustMode.None ->
                GuidedSequenceResult.SavePreferencesAdjustment(newState = state)
        }
}

/**
 * RC7D.19 kept the Category Menu blink selection *visible*; RC7D.21 upgrades that to a single
 * canonical **viewport-centering** authority. Every selection change — Move Up / Move Down,
 * Previous Page / Next Page, touch, direct shortcut, and Category Menu restoration — feeds the same
 * pure calculation here, so positioning can never drift between input sources.
 *
 * The policy: place the selected category near the vertical centre of the visible category list
 * whenever there is enough scroll room, and clamp to the top / content-bottom otherwise so we never
 * open blank space above category 1 or below the final category. It is measurement-driven (item
 * top, item height, viewport height, max scroll) rather than fixed-index, and is safe when
 * measurements are momentarily unavailable (returns a harmless clamped value instead of crashing).
 */
object GuidedCategoryMenuScroll {
    /**
     * Approximate [GuidedCategoryMenuRow] pitch (padding + text + 8.dp spacing). Only a deterministic
     * fallback row model for [centeredScrollOffsetPxForIndex] where a live measurement is not
     * available (and for focused index-based tests); the live Compose path measures real pixels.
     */
    const val ROW_PITCH_PX: Int = 72

    /**
     * Minimum |target - current| gap worth animating. Prevents one-pixel correction loops / jitter
     * when a fresh measurement lands a hair off the current resting offset.
     */
    const val CENTERING_TOLERANCE_PX: Int = 4

    /**
     * Canonical RC7D.21 calculation. Returns the scroll offset that centres the selected category in
     * the viewport, clamped to `[0, maxScrollPx]`.
     *
     * Safe by construction: zero/negative viewport or max scroll yields 0 (nothing to scroll),
     * negative measurements are coerced, and the result is always within the valid scroll range —
     * never negative, never past the content bottom. Pure and deterministic: identical inputs always
     * produce an identical target, so it can be unit-tested without Compose.
     */
    fun centeredScrollOffsetPx(
        selectedItemTopPx: Int,
        selectedItemHeightPx: Int,
        viewportHeightPx: Int,
        maxScrollPx: Int
    ): Int {
        if (viewportHeightPx <= 0 || maxScrollPx <= 0) return 0
        val safeTop = selectedItemTopPx.coerceAtLeast(0)
        val safeHeight = selectedItemHeightPx.coerceAtLeast(0)
        val itemCentre = safeTop + safeHeight / 2
        val target = itemCentre - viewportHeightPx / 2
        return target.coerceIn(0, maxScrollPx)
    }

    /**
     * Index-based convenience over [centeredScrollOffsetPx] using the uniform [ROW_PITCH_PX] row
     * model. Used where only a selection index is known (deterministic fallback + focused tests).
     */
    fun centeredScrollOffsetPxForIndex(
        selectionIndex: Int,
        viewportHeightPx: Int,
        maxScrollPx: Int,
        rowPitchPx: Int = ROW_PITCH_PX
    ): Int {
        val pitch = rowPitchPx.coerceAtLeast(1)
        return centeredScrollOffsetPx(
            selectedItemTopPx = selectionIndex.coerceAtLeast(0) * pitch,
            selectedItemHeightPx = pitch,
            viewportHeightPx = viewportHeightPx,
            maxScrollPx = maxScrollPx
        )
    }

    /**
     * Integration gate: only animate when the freshly calculated [target] differs from the [current]
     * resting offset by more than [tolerancePx]. Keeps recomposition-driven re-measures from firing
     * redundant scroll animations.
     */
    fun shouldAnimateTo(
        current: Int,
        target: Int,
        tolerancePx: Int = CENTERING_TOLERANCE_PX
    ): Boolean = kotlin.math.abs(target - current) > tolerancePx

    fun canReachEveryCategoryByBlink(itemCount: Int = GuidedVocabularyCategory.PAGE_COUNT): Boolean =
        itemCount == GuidedVocabularyCategory.ordered.size &&
            itemCount == GuidedVocabularyCategory.PAGE_COUNT
}

/**
 * RC7D.22 — canonical VIEWPORT-page authority for the Category Menu.
 *
 * A "page" here is a scroll window over the category list, derived purely from measured layout
 * (viewport height, content height / maximum scroll offset) — never from `selectionIndex / 7`.
 * The visible top of the list (scroll offset 0) is page 1; scrolling to the bottom (maxScrollPx)
 * is the final page. A category that straddles a viewport boundary (e.g. a partly-visible
 * Category 7) can therefore legitimately belong to two adjacent pages, which is exactly why page
 * position and selected-category position are kept independent.
 *
 * Every function is pure, deterministic and defensive against zero/negative measurements so it can
 * be unit-tested without Compose and can never crash on a transient un-measured frame.
 */
object CategoryViewportPaging {
    /** Small tolerance so a resting offset a hair short of the bottom still counts as the last page. */
    const val ANCHOR_TOLERANCE_PX: Int = 4

    /**
     * Number of viewport-sized pages needed to cover the scrollable content. Content that fits
     * entirely inside the viewport (no scroll room) is a single page; any overflow up to one
     * viewport is two pages; further overflow adds a page per viewport height.
     */
    fun pageCount(viewportHeightPx: Int, maxScrollPx: Int): Int {
        if (viewportHeightPx <= 0 || maxScrollPx <= 0) return 1
        return 1 + ((maxScrollPx + viewportHeightPx - 1) / viewportHeightPx)
    }

    /** Convenience page count from a content height instead of a pre-computed maximum scroll. */
    fun pageCountForContent(viewportHeightPx: Int, contentHeightPx: Int): Int {
        val maxScroll = (contentHeightPx - viewportHeightPx).coerceAtLeast(0)
        return pageCount(viewportHeightPx, maxScroll)
    }

    /**
     * Scroll offset that anchors a given viewport page. Page 0 anchors at the top (0); the final
     * page always anchors at [maxScrollPx] (so the bottom of the list is fully revealed with no
     * blank space); interim pages step down one viewport height at a time. Always clamped into
     * `[0, maxScrollPx]`.
     */
    fun pageAnchorOffsetPx(pageIndex: Int, viewportHeightPx: Int, maxScrollPx: Int): Int {
        if (viewportHeightPx <= 0 || maxScrollPx <= 0) return 0
        val count = pageCount(viewportHeightPx, maxScrollPx)
        val page = clampPage(pageIndex, count)
        if (page >= count - 1) return maxScrollPx
        return (page.toLong() * viewportHeightPx).coerceIn(0L, maxScrollPx.toLong()).toInt()
    }

    /**
     * Which viewport page a raw scroll offset currently sits on. Anything at (or within tolerance
     * of) the bottom is the final page; otherwise the offset is bucketed by viewport height.
     */
    fun currentPageForScroll(scrollPx: Int, viewportHeightPx: Int, maxScrollPx: Int): Int {
        val count = pageCount(viewportHeightPx, maxScrollPx)
        if (count <= 1 || viewportHeightPx <= 0) return 0
        if (scrollPx >= maxScrollPx - ANCHOR_TOLERANCE_PX) return count - 1
        val page = scrollPx.coerceAtLeast(0) / viewportHeightPx
        return clampPage(page, count)
    }

    fun clampPage(pageIndex: Int, pageCount: Int): Int =
        pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

    fun canGoToNextPage(currentPage: Int, pageCount: Int): Boolean =
        currentPage < pageCount - 1

    fun canGoToPreviousPage(currentPage: Int): Boolean =
        currentPage > 0
}

object GuidedNavigationController {

    fun phrasePageCount(entryCount: Int, visibleCap: Int): Int {
        if (entryCount <= 0) return 1
        return (entryCount + visibleCap - 1) / visibleCap
    }

    fun visiblePhraseEntries(
        entries: List<GuidedVocabularyEntry>,
        phrasePageIndex: Int,
        visibleCap: Int
    ): List<GuidedVocabularyEntry> {
        if (entries.isEmpty()) return emptyList()
        val start = phrasePageIndex.coerceAtLeast(0) * visibleCap
        if (start >= entries.size) return emptyList()
        return entries.subList(start, minOf(start + visibleCap, entries.size))
    }

    fun openCategoryMenu(state: GuidedNavigationState): GuidedNavigationState =
        state.normalized().copy(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = state.categoryIndex,
            // RC7D.22 — restore deterministically from the top; the Compose layer re-measures the
            // real viewport, syncs the page count, and reveals the restored selection.
            categoryViewportPage = 0,
            categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
        )

    /**
     * RC8.1 — canonical top-level Communication landing: Category Selection with focus on a
     * category row, without opening that category's phrase page.
     */
    fun communicationWorkspaceRoot(
        state: GuidedNavigationState = GuidedNavigationState()
    ): GuidedNavigationState =
        openCategoryMenu(
            state.copy(
                preferencesAdjustMode = GuidedPreferencesAdjustMode.None,
                phrasePageIndex = 0
            )
        )

    fun closeCategoryMenu(state: GuidedNavigationState): GuidedNavigationState =
        state.normalized().copy(screenMode = GuidedOverlayScreenMode.Vocabulary)

    fun moveCategorySelectionUp(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            categoryMenuSelection = (normalized.categoryMenuSelection - 1)
                .coerceAtLeast(0),
            categoryNavigationCause = CategoryNavigationCause.ITEM_MOVEMENT
        )
    }

    fun moveCategorySelectionDown(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            categoryMenuSelection = (normalized.categoryMenuSelection + 1)
                .coerceAtMost(GuidedVocabularyCategory.PAGE_COUNT - 1),
            categoryNavigationCause = CategoryNavigationCause.ITEM_MOVEMENT
        )
    }

    /**
     * RC7D.22 — TRUE viewport page navigation. Advances the category-list viewport page directly
     * (never `selectionIndex / pageSize`); the Compose coordinator then scrolls straight to that
     * page's measured anchor — no stepping through categories, no dependence on selection centring.
     * A single decorative selection update (the final category, visible on the bottom page) gives a
     * usable highlight, but the PAGE_MOVEMENT cause guarantees the page anchor — not the selection —
     * is what drives the scroll. No-op / never wraps at the last page (gated by the caller).
     */
    fun nextCategoryPage(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        val lastPage = (normalized.categoryViewportPageCount - 1).coerceAtLeast(0)
        return normalized.copy(
            categoryViewportPage = (normalized.categoryViewportPage + 1).coerceAtMost(lastPage),
            categoryMenuSelection = (GuidedVocabularyCategory.PAGE_COUNT - 1).coerceAtLeast(0),
            categoryNavigationCause = CategoryNavigationCause.PAGE_MOVEMENT
        )
    }

    /**
     * RC7D.22 — TRUE viewport page navigation back to the preceding page. Sets the viewport page
     * directly so the coordinator scrolls straight to that page's anchor (page 1 → offset 0,
     * restoring the exact top view seen when the menu first opens). Selection is moved once to
     * Category 1 for a usable highlight; the PAGE_MOVEMENT cause keeps the page anchor authoritative
     * so centring can never drag the view back toward the lower page. No-op / never wraps at page 1.
     */
    fun previousCategoryPage(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            categoryViewportPage = (normalized.categoryViewportPage - 1).coerceAtLeast(0),
            categoryMenuSelection = 0,
            categoryNavigationCause = CategoryNavigationCause.PAGE_MOVEMENT
        )
    }

    fun openSelectedCategory(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = normalized.categoryMenuSelection,
            phrasePageIndex = 0
        )
    }

    fun openCategoryDirectly(state: GuidedNavigationState, categoryIndex: Int): GuidedNavigationState =
        openCategoryAtPage(state, categoryIndex, phrasePageIndex = 0)
            .copy(categoryNavigationCause = CategoryNavigationCause.DIRECT_SHORTCUT)

    /** RC7D.10 — open destination category on the page that contains [phrasePageIndex]. */
    fun openCategoryAtPage(
        state: GuidedNavigationState,
        categoryIndex: Int,
        phrasePageIndex: Int
    ): GuidedNavigationState =
        state.normalized().copy(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = categoryIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1),
            categoryMenuSelection = categoryIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1),
            phrasePageIndex = phrasePageIndex.coerceAtLeast(0),
            preferencesAdjustMode = GuidedPreferencesAdjustMode.None,
            adjustmentScrollStep = 0
        )

    fun openCategoryMenuEscapingAdjustment(state: GuidedNavigationState): GuidedNavigationState =
        openCategoryMenu(PreferenceAdjustmentController.exitSettingsMenu(state))

    fun scrollAdjustmentContentUp(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            adjustmentScrollStep = (normalized.adjustmentScrollStep - 1).coerceAtLeast(0)
        )
    }

    fun scrollAdjustmentContentDown(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(adjustmentScrollStep = normalized.adjustmentScrollStep + 1)
    }

    fun movePhrasePagePrevious(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            phrasePageIndex = (normalized.phrasePageIndex - 1).coerceAtLeast(0)
        )
    }

    fun movePhrasePageNext(
        state: GuidedNavigationState,
        entryCount: Int,
        visibleCap: Int
    ): GuidedNavigationState {
        val normalized = state.normalized()
        val maxPage = phrasePageCount(entryCount, visibleCap) - 1
        return normalized.copy(
            phrasePageIndex = (normalized.phrasePageIndex + 1).coerceAtMost(maxPage)
        )
    }

    fun processSequence(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(),
        /** RC7D.27 — order of blinks (true=left). Required to distinguish L1 R1 from R1 L1. */
        blinkOrder: List<Boolean> = emptyList()
    ): GuidedSequenceResult {
        val normalized = state.normalized()

        if (normalized.isPreferencesAdjustmentActive) {
            return processPreferencesAdjustmentGesture(left, right, normalized, catalogContext, blinkOrder)
        }

        // RC7D.25 — the single global entry into the Adjust Settings sub-mode. Checked before the
        // per-screen dispatch so it is reachable from both the Category Menu and any phrase category
        // page; it matches no category shortcut or vocabulary slot, so nothing is shadowed.
        if (GuidedModeNavigation.isAdjustSettingsEntrySequence(left, right)) {
            return GuidedSequenceResult.Navigate(PreferenceAdjustmentController.openSettingsMenu(normalized))
        }

        val currentPage = GuidedVocabularyCatalog.categoryAt(
            pageIndex = normalized.categoryIndex,
            language = language,
            uiStrings = uiStrings,
            catalogContext = catalogContext
        )

        return when (normalized.screenMode) {
            GuidedOverlayScreenMode.CategoryMenu -> processCategoryMenuGesture(
                left = left,
                right = right,
                state = normalized
            )
            GuidedOverlayScreenMode.Vocabulary -> processVocabularyGesture(
                left = left,
                right = right,
                state = normalized,
                pageIndex = normalized.categoryIndex,
                language = language,
                uiStrings = uiStrings,
                catalogContext = catalogContext,
                entryCount = currentPage?.entries?.size ?: 0,
                visibleEntryCap = visibleEntryCap
            )
        }
    }

    /**
     * Reserved global-navigation gestures that are actually actionable from [state] right now —
     * probes [processSequence] itself for each reserved code rather than duplicating
     * [processCategoryMenuGesture]/[processVocabularyGesture]'s boundary rules, so this can never
     * drift out of sync with what actually executes. Emergency and Finish Training are handled
     * upstream of this controller entirely, so callers add those separately.
     */
    fun visibleGlobalNavigationGestures(
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(),
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
    ): Set<Pair<Int, Int>> {
        val candidates = listOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT
        )
        return candidates.filterTo(mutableSetOf()) { (left, right) ->
            val result = processSequence(left, right, state, language, uiStrings, visibleEntryCap, catalogContext)
            result !is GuidedSequenceResult.Unmatched
        }
    }

    /**
     * Settings & Controls gesture routing:
     *   • SettingsMenu — hub cards open settings or fire control actions; L2 R2 exits.
     *   • Sensitivity / ResponseTime / SpeechVolume / SpeechSpeed — decrease / increase /
     *     enter save confirmation / cancel to hub.
     *   • Listening — toggle pause/resume; Back returns to hub.
     *   • ConfirmSave* — L1 R1 persists; R1 L1 returns to editing; other commands blocked.
     * Emergency (L6 R0) is matched upstream of this controller and therefore always takes priority.
     */
    private fun processPreferencesAdjustmentGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext,
        blinkOrder: List<Boolean>
    ): GuidedSequenceResult = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.SettingsMenu ->
            processSettingsMenuGesture(left, right, state, catalogContext)
        GuidedPreferencesAdjustMode.ConfirmSaveSensitivity,
        GuidedPreferencesAdjustMode.ConfirmSaveResponseTime,
        GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume,
        GuidedPreferencesAdjustMode.ConfirmSaveSpeechSpeed ->
            processSaveConfirmationGesture(left, right, state, blinkOrder)
        GuidedPreferencesAdjustMode.Sensitivity,
        GuidedPreferencesAdjustMode.ResponseTime,
        GuidedPreferencesAdjustMode.SpeechVolume,
        GuidedPreferencesAdjustMode.SpeechSpeed ->
            processValueAdjustmentGesture(left, right, state, blinkOrder)
        GuidedPreferencesAdjustMode.Listening ->
            processListeningControlGesture(left, right, state)
        GuidedPreferencesAdjustMode.None -> GuidedSequenceResult.Unmatched
    }

    /**
     * Settings & Controls hub:
     *   • L2 R0 / L0 R2 move the highlighted card (Scroll Up / Down) — never open a setting
     *   • L1 R1 opens the highlighted setting (Select)
     *   • L1 R2 / L3 R2 open Speech Volume / Speed directly (card shortcuts)
     *   • L2 R2 exits; L3 R0 opens Categories
     */
    private fun processSettingsMenuGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext
    ): GuidedSequenceResult {
        if (GuidedModeNavigation.isBackSequence(left, right)) {
            return GuidedSequenceResult.Navigate(PreferenceAdjustmentController.exitSettingsMenu(state))
        }
        if (GuidedModeNavigation.isCategoriesSequence(left, right)) {
            return GuidedSequenceResult.Navigate(openCategoryMenuEscapingAdjustment(state))
        }
        if (GuidedModeNavigation.isPreviousSequence(left, right)) {
            return if (state.settingsHubSelection > 0) {
                GuidedSequenceResult.Navigate(PreferenceAdjustmentController.moveHubSelectionUp(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        }
        if (GuidedModeNavigation.isNextSequence(left, right)) {
            val last = SettingsAndControlsHubSequences.HUB_SETTING_KINDS.lastIndex
            return if (state.settingsHubSelection < last) {
                GuidedSequenceResult.Navigate(PreferenceAdjustmentController.moveHubSelectionDown(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        }
        if (GuidedModeNavigation.isSelectSequence(left, right)) {
            return GuidedSequenceResult.Navigate(
                PreferenceAdjustmentController.openSelectedHubSetting(state, catalogContext)
            )
        }
        return when (SettingsAndControlsHubSequences.hubDirectOpenKindForGesture(left, right)) {
            SettingsControlKind.SpeechVolume -> GuidedSequenceResult.Navigate(
                PreferenceAdjustmentController.openHubSetting(
                    state,
                    SettingsControlKind.SpeechVolume,
                    catalogContext
                )
            )
            SettingsControlKind.SpeechSpeed -> GuidedSequenceResult.Navigate(
                PreferenceAdjustmentController.openHubSetting(
                    state,
                    SettingsControlKind.SpeechSpeed,
                    catalogContext
                )
            )
            else -> GuidedSequenceResult.Unmatched
        }
    }

    private fun processListeningControlGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isBackSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.cancelAdjustment(state))
        GuidedModeNavigation.isSelectSequence(left, right) ||
            (left == SettingsAndControlsHubSequences.LISTENING.first &&
                right == SettingsAndControlsHubSequences.LISTENING.second) ||
            (left == SettingsAndControlsHubSequences.LISTENING_ALT.first &&
                right == SettingsAndControlsHubSequences.LISTENING_ALT.second) ->
            GuidedSequenceResult.SettingsControlAction(SettingsControlKind.Listening)
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(openCategoryMenuEscapingAdjustment(state))
        else -> GuidedSequenceResult.Unmatched
    }

    /** Value-adjustment level — first L1 R1 enters save confirmation (does not persist). */
    private fun processValueAdjustmentGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        blinkOrder: List<Boolean>
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isPreviousSequence(left, right) ->
            GuidedSequenceResult.Navigate(scrollAdjustmentContentUp(state))
        GuidedModeNavigation.isNextSequence(left, right) ->
            GuidedSequenceResult.Navigate(scrollAdjustmentContentDown(state))
        GuidedModeNavigation.isDecreaseValueSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.decreaseDraft(state))
        GuidedModeNavigation.isIncreaseValueSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.increaseDraft(state))
        // Touch Save uses counts (1,1) without order; blink Save requires L-then-R when order is known.
        GuidedModeNavigation.isSelectSequence(left, right) &&
            (blinkOrder.isEmpty() || GuidedModeNavigation.isConfirmSequence(left, right, blinkOrder)) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.beginSaveConfirmation(state))
        GuidedModeNavigation.isBackSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.cancelAdjustment(state))
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(openCategoryMenuEscapingAdjustment(state))
        else -> GuidedSequenceResult.Unmatched
    }

    /**
     * RC7D.27 — confirmation: L1 R1 (or touch Confirm with empty order) persists;
     * R1 L1 cancels confirmation and returns to editing with the draft kept.
     */
    private fun processSaveConfirmationGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        blinkOrder: List<Boolean>
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isConfirmCancelSequence(left, right, blinkOrder) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.cancelSaveConfirmation(state))
        GuidedModeNavigation.isSelectSequence(left, right) &&
            (blinkOrder.isEmpty() || GuidedModeNavigation.isConfirmSequence(left, right, blinkOrder)) ->
            PreferenceAdjustmentController.saveAdjustment(state)
        else -> GuidedSequenceResult.Unmatched
    }

    /**
     * Same up/down-availability rule the Navigation Panel itself uses to enable/disable its
     * Previous/Next buttons (see `canGoPrevious`/`canGoNext` in [com.idworx.lisa.GuidedModeNavigationPanel]'s
     * caller) — a scroll-style gesture only ever executes when its target is currently reachable,
     * never a silent same-state no-op. General across every category: driven entirely by the
     * live selection/count, never a hardcoded index.
     */
    private fun processCategoryMenuGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isPreviousSequence(left, right) ->
            if (state.categoryMenuSelection > 0) {
                GuidedSequenceResult.Navigate(moveCategorySelectionUp(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        GuidedModeNavigation.isNextSequence(left, right) ->
            if (state.categoryMenuSelection < GuidedVocabularyCategory.PAGE_COUNT - 1) {
                GuidedSequenceResult.Navigate(moveCategorySelectionDown(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        GuidedModeNavigation.isPreviousCategoryPageSequence(left, right) ->
            // RC7D.22 — gated on the VIEWPORT page, not the selected category. Previous Page is a
            // safe no-op on page 1 and never wraps.
            if (CategoryViewportPaging.canGoToPreviousPage(state.categoryViewportPage)) {
                GuidedSequenceResult.Navigate(previousCategoryPage(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        GuidedModeNavigation.isNextCategoryPageSequence(left, right) ->
            if (CategoryViewportPaging.canGoToNextPage(state.categoryViewportPage, state.categoryViewportPageCount)) {
                GuidedSequenceResult.Navigate(nextCategoryPage(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        GuidedModeNavigation.isSelectSequence(left, right) ->
            GuidedSequenceResult.Navigate(openSelectedCategory(state))
        GuidedModeNavigation.isBackSequence(left, right) ->
            GuidedSequenceResult.Navigate(closeCategoryMenu(state))
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(state.normalized())
        else -> GuidedCategoryShortcuts.categoryIndexForGesture(left, right)?.let { categoryIndex ->
            GuidedSequenceResult.Navigate(openCategoryDirectly(state, categoryIndex))
        } ?: GuidedSequenceResult.Unmatched
    }

    private fun processVocabularyGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext,
        entryCount: Int,
        visibleEntryCap: Int
    ): GuidedSequenceResult = when {
        // Previous/Next Phrase Page only ever execute when that page action is actually visible
        // (matches the header's guidedPhrasePageScrollHint and the panel's canGoPrevious/canGoNext) —
        // a gesture whose target page doesn't exist is Unmatched, never a same-state no-op.
        GuidedModeNavigation.isPreviousSequence(left, right) ->
            if (state.phrasePageIndex > 0) {
                GuidedSequenceResult.Navigate(movePhrasePagePrevious(state))
            } else {
                GuidedSequenceResult.Unmatched
            }
        GuidedModeNavigation.isNextSequence(left, right) ->
            if (state.phrasePageIndex < phrasePageCount(entryCount, visibleEntryCap) - 1) {
                GuidedSequenceResult.Navigate(movePhrasePageNext(state, entryCount, visibleEntryCap))
            } else {
                GuidedSequenceResult.Unmatched
            }
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(openCategoryMenu(state))
        GuidedModeNavigation.isBackSequence(left, right) ->
            // RC8.1 — Back from an opened category returns to Category Selection.
            GuidedSequenceResult.Navigate(openCategoryMenu(state))
        else -> {
            val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
                left = left,
                right = right,
                pageIndex = pageIndex,
                phrasePageIndex = state.phrasePageIndex,
                language = language,
                uiStrings = uiStrings,
                catalogContext = catalogContext,
                visibleEntryCap = visibleEntryCap
            )
            when {
                match == null -> GuidedSequenceResult.Unmatched
                match.guidedAction == GuidedOverlayAction.OpenAdjustResponseTime ->
                    GuidedSequenceResult.Navigate(
                        PreferenceAdjustmentController.openResponseTimeAdjust(state, catalogContext.responseTimeSec)
                    )
                match.guidedAction == GuidedOverlayAction.OpenAdjustSensitivity ->
                    GuidedSequenceResult.Navigate(
                        PreferenceAdjustmentController.openSensitivityAdjust(state, catalogContext.sensitivityLevel)
                    )
                match.kind == GuidedEntryKind.SystemAction -> GuidedSequenceResult.SystemAction(match)
                else -> GuidedSequenceResult.Speak(match)
            }
        }
    }
}

object GuidedVocabularyCatalog {

    const val DEFAULT_VISIBLE_ENTRY_CAP: Int = 6

    private data class CatalogSpec(
        val category: GuidedVocabularyCategory,
        val entries: List<CatalogEntrySpec>
    )

    private sealed class CatalogEntrySpec {
        data class CoreVocabulary(val vocabularyId: String) : CatalogEntrySpec()
        data class Phrase(val phraseKey: String) : CatalogEntrySpec()
        data class System(
            val labelKey: String,
            val systemAction: SystemCommandAction? = null,
            val guidedAction: GuidedOverlayAction? = null
        ) : CatalogEntrySpec()

        data class Preference(
            val labelKey: String,
            val guidedAction: GuidedOverlayAction,
            val value: Int? = null
        ) : CatalogEntrySpec()
    }

    private val catalog: List<CatalogSpec> = listOf(
        CatalogSpec(
            category = GuidedVocabularyCategory.Conversation,
            entries = listOf(
                CatalogEntrySpec.CoreVocabulary("yes"),
                CatalogEntrySpec.CoreVocabulary("no"),
                CatalogEntrySpec.CoreVocabulary("thank_you"),
                CatalogEntrySpec.CoreVocabulary("i_love_you"),
                CatalogEntrySpec.CoreVocabulary("please"),
                CatalogEntrySpec.Phrase("stop"),
                CatalogEntrySpec.Phrase("i_am_okay"),
                CatalogEntrySpec.CoreVocabulary("i_need_help"),
                CatalogEntrySpec.Phrase("please_repeat_that"),
                CatalogEntrySpec.CoreVocabulary("i_dont_understand")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.BasicNeeds,
            entries = listOf(
                CatalogEntrySpec.Phrase("i_need_some_water"),
                CatalogEntrySpec.Phrase("i_am_hungry"),
                CatalogEntrySpec.Phrase("i_need_to_use_the_bathroom"),
                CatalogEntrySpec.Phrase("i_am_tired"),
                CatalogEntrySpec.Phrase("i_am_cold"),
                CatalogEntrySpec.Phrase("i_am_hot"),
                CatalogEntrySpec.Phrase("please_help_me_move"),
                CatalogEntrySpec.Phrase("please_reposition_me"),
                CatalogEntrySpec.Phrase("i_am_uncomfortable"),
                CatalogEntrySpec.Phrase("i_would_like_some_privacy")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.Medical,
            entries = listOf(
                CatalogEntrySpec.Phrase("i_am_in_pain"),
                CatalogEntrySpec.Phrase("please_call_the_nurse"),
                CatalogEntrySpec.Phrase("i_need_my_medication"),
                CatalogEntrySpec.Phrase("please_help_me_now"),
                CatalogEntrySpec.Phrase("i_am_having_difficulty_breathing"),
                CatalogEntrySpec.Phrase("i_feel_dizzy"),
                CatalogEntrySpec.Phrase("my_chest_hurts"),
                CatalogEntrySpec.Phrase("i_do_not_feel_well"),
                CatalogEntrySpec.Phrase("please_call_the_doctor"),
                CatalogEntrySpec.Phrase("please_check_my_body")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.Family,
            entries = listOf(
                CatalogEntrySpec.Phrase("i_want_to_see_my_mom"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_dad"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_wife"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_husband"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_child"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_friend"),
                CatalogEntrySpec.Phrase("call_my_family"),
                CatalogEntrySpec.Phrase("i_miss_you"),
                CatalogEntrySpec.Phrase("stay_with_me"),
                CatalogEntrySpec.Phrase("i_want_to_talk")
            )
        ),
        // Basic System Controls + Preferences migrated into Settings & Controls — no longer phrase categories.
        // Custom page: composer launcher only — no stored phrases (RC7D.1).
        CatalogSpec(
            category = GuidedVocabularyCategory.Custom,
            entries = emptyList()
        ),
        // Phrase Management: management destination — no spoken phrases (RC7D.14).
        CatalogSpec(
            category = GuidedVocabularyCategory.PhraseManagement,
            entries = emptyList()
        ),
        // Settings & Controls: navigation destination only — opens settings hub (RC7D.26+).
        CatalogSpec(
            category = GuidedVocabularyCategory.AdjustSettings,
            entries = emptyList()
        )
    )

    fun buildPages(
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): List<GuidedCategoryPage> =
        GuidedVocabularyCategory.ordered.mapNotNull { category ->
            val spec = catalog.firstOrNull { it.category == category } ?: return@mapNotNull null
            val builtInEntries = spec.entries.mapIndexedNotNull { index, entrySpec ->
                resolveEntry(
                    spec = entrySpec,
                    slotIndex = index,
                    language = language,
                    uiStrings = uiStrings,
                    catalogContext = catalogContext
                )
            }
            val customEntries = if (
                spec.category == GuidedVocabularyCategory.Custom ||
                spec.category == GuidedVocabularyCategory.PhraseManagement ||
                spec.category == GuidedVocabularyCategory.AdjustSettings
            ) {
                emptyList()
            } else {
                catalogContext.caregiverCustomPhrases
                    .filter { it.category.toGuidedCategory() == spec.category }
                    .mapIndexed { index, custom ->
                        GuidedVocabularyEntry(
                            left = custom.left,
                            right = custom.right,
                            phrase = custom.phrase,
                            englishSubtitle = null,
                            kind = GuidedEntryKind.SpeakPhrase,
                            slotIndex = builtInEntries.size + index
                        )
                    }
            }
            GuidedCategoryPage(
                category = spec.category,
                title = uiStrings.guidedCategoryTitle(spec.category),
                entries = builtInEntries + customEntries
            )
        }

    fun categoryAt(
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): GuidedCategoryPage? =
        buildPages(language, uiStrings, catalogContext)
            .getOrNull(pageIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1))

    fun categoryMenuTitles(uiStrings: LisaUiStrings): List<String> =
        GuidedVocabularyCategory.ordered.map { uiStrings.guidedCategoryTitle(it) }

    data class CatalogPhraseEntry(
        val phrase: String,
        val category: CustomPhraseEngine.CaregiverPhraseCategory
    )

    fun catalogPhraseEntries(
        language: PreferredLanguage,
        uiStrings: LisaUiStrings
    ): List<CatalogPhraseEntry> = catalog.flatMap { spec ->
        val caregiverCategory = spec.category.toCaregiverCategory() ?: return@flatMap emptyList()
        spec.entries.mapNotNull { entrySpec ->
            when (entrySpec) {
                is CatalogEntrySpec.Phrase -> CatalogPhraseEntry(
                    phrase = uiStrings.guidedPhrase(entrySpec.phraseKey),
                    category = caregiverCategory
                )
                else -> null
            }
        }
    }

    fun categoryForCoreVocabularyId(vocabularyId: String): GuidedVocabularyCategory? =
        catalog.firstOrNull { spec ->
            spec.entries.any { entry ->
                when (entry) {
                    is CatalogEntrySpec.CoreVocabulary -> entry.vocabularyId == vocabularyId
                    is CatalogEntrySpec.Phrase -> entry.phraseKey == vocabularyId
                    else -> false
                }
            }
        }?.category

    fun currentPageEntries(
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): List<GuidedVocabularyEntry> =
        categoryAt(pageIndex, language, uiStrings, catalogContext)?.entries.orEmpty()

    fun findMatchOnCurrentPage(
        left: Int,
        right: Int,
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): GuidedVocabularyEntry? =
        currentPageEntries(pageIndex, language, uiStrings, catalogContext)
            .firstOrNull { it.left == left && it.right == right }

    /** Resolves a phrase only when it appears on the currently visible phrase page. */
    fun findMatchOnVisiblePage(
        left: Int,
        right: Int,
        pageIndex: Int,
        phrasePageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(),
        visibleEntryCap: Int = DEFAULT_VISIBLE_ENTRY_CAP
    ): GuidedVocabularyEntry? =
        GuidedNavigationController.visiblePhraseEntries(
            entries = currentPageEntries(pageIndex, language, uiStrings, catalogContext),
            phrasePageIndex = phrasePageIndex,
            visibleCap = visibleEntryCap
        ).firstOrNull { it.left == left && it.right == right }

    fun visibleEntryCount(screenWidthDp: Int, screenHeightDp: Int): Int = when {
        screenWidthDp >= 600 && screenHeightDp >= 640 -> 10
        screenWidthDp >= 400 && screenHeightDp >= 560 -> 8
        else -> DEFAULT_VISIBLE_ENTRY_CAP
    }

    fun allVocabularyPhraseEntries(language: PreferredLanguage, uiStrings: LisaUiStrings): List<GuidedVocabularyEntry> =
        buildPages(language, uiStrings)
            .flatMap { it.entries }
            .filter { it.kind == GuidedEntryKind.SpeakPhrase }

    /** @deprecated Category paging uses Category Menu mode in V1. */
    fun previousPage(currentPage: Int): Int = (currentPage - 1).coerceAtLeast(0)

    /** @deprecated Category paging uses Category Menu mode in V1. */
    fun nextPage(currentPage: Int): Int =
        (currentPage + 1).coerceAtMost(GuidedVocabularyCategory.PAGE_COUNT - 1)

    private fun resolveEntry(
        spec: CatalogEntrySpec,
        slotIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext
    ): GuidedVocabularyEntry? {
        val left = GuidedPageSequences.leftAt(slotIndex)
        val right = GuidedPageSequences.rightAt(slotIndex)
        return when (spec) {
            is CatalogEntrySpec.CoreVocabulary -> {
                val mapping = defaultLanguageMappings().firstOrNull { it.vocabularyId == spec.vocabularyId }
                    ?: return null
                GuidedVocabularyEntry(
                    left = left,
                    right = right,
                    phrase = mapping.localizedPhrase(language),
                    englishSubtitle = if (language != PreferredLanguage.English && !mapping.isCustom) {
                        LisaCoreVocabulary.text(spec.vocabularyId, PreferredLanguage.English)
                    } else null,
                    kind = GuidedEntryKind.SpeakPhrase,
                    slotIndex = slotIndex
                )
            }
            is CatalogEntrySpec.Phrase -> GuidedVocabularyEntry(
                left = left,
                right = right,
                phrase = uiStrings.guidedPhrase(spec.phraseKey),
                englishSubtitle = uiStrings.guidedPhraseEnglish(spec.phraseKey),
                kind = GuidedEntryKind.SpeakPhrase,
                slotIndex = slotIndex
            )
            is CatalogEntrySpec.System -> GuidedVocabularyEntry(
                left = left,
                right = right,
                phrase = uiStrings.guidedSystemLabel(spec.labelKey),
                englishSubtitle = uiStrings.guidedSystemLabelEnglish(spec.labelKey),
                kind = GuidedEntryKind.SystemAction,
                systemAction = spec.systemAction,
                guidedAction = spec.guidedAction,
                slotIndex = slotIndex
            )
            is CatalogEntrySpec.Preference -> GuidedVocabularyEntry(
                left = left,
                right = right,
                phrase = uiStrings.guidedPreferenceLabel(
                    labelKey = spec.labelKey,
                    responseTimeSec = catalogContext.responseTimeSec,
                    sensitivityLevel = catalogContext.sensitivityLevel,
                    value = spec.value
                ),
                englishSubtitle = uiStrings.guidedPreferenceLabelEnglish(
                    labelKey = spec.labelKey,
                    responseTimeSec = catalogContext.responseTimeSec,
                    sensitivityLevel = catalogContext.sensitivityLevel,
                    value = spec.value
                ),
                kind = GuidedEntryKind.SystemAction,
                guidedAction = spec.guidedAction,
                preferenceValue = spec.value,
                slotIndex = slotIndex
            )
        }
    }
}

/** Validates that no two important actions share the same gesture within a guided mode. */
object GuidedNavigationGestureAudit {

    data class GestureBinding(val left: Int, val right: Int, val action: String)

    fun globalPanelBindings(): List<GestureBinding> = listOf(
        GestureBinding(2, 0, "ScrollUp"),
        GestureBinding(0, 2, "ScrollDown"),
        GestureBinding(1, 1, "SelectSave"),
        GestureBinding(2, 2, "BackCancel"),
        GestureBinding(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, "Categories"),
        GestureBinding(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS, "Emergency")
    )

    fun adjustmentOnlyBindings(): List<GestureBinding> = listOf(
        GestureBinding(3, 1, "DecreaseValue"),
        GestureBinding(1, 3, "IncreaseValue")
    )

    fun vocabularyModeBindings(pageIndex: Int): List<GestureBinding> {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val entries = GuidedVocabularyCatalog.currentPageEntries(
            pageIndex = pageIndex,
            language = PreferredLanguage.English,
            uiStrings = uiStrings
        )
        return globalPanelBindings() + entries.map { entry ->
            GestureBinding(entry.left, entry.right, "Phrase:${entry.phrase}")
        }
    }

    fun categoryMenuModeBindings(): List<GestureBinding> =
        globalPanelBindings() +
            listOf(
                GestureBinding(
                    GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                    GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
                    "PreviousCategoryPage"
                ),
                GestureBinding(
                    GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                    GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
                    "NextCategoryPage"
                )
            ) +
            GuidedCategoryShortcuts.allGestures().mapIndexed { index, gesture ->
                GestureBinding(gesture.first, gesture.second, "CategoryShortcut:$index")
            }

    fun preferencesPageBindings(): List<GestureBinding> =
        // RC8.5 — Preferences page removed; Settings hub uses shared adjustment bindings.
        responseTimeAdjustmentBindings()

    fun responseTimeAdjustmentBindings(): List<GestureBinding> =
        globalPanelBindings() + adjustmentOnlyBindings()

    fun sensitivityAdjustmentBindings(): List<GestureBinding> =
        globalPanelBindings() + adjustmentOnlyBindings()

    fun noDuplicateGestures(bindings: List<GestureBinding>): Boolean =
        bindings.groupBy { it.left to it.right }.all { it.value.size == 1 }

    fun auditAllModes(): Boolean =
        noDuplicateGestures(vocabularyModeBindings(0)) &&
            noDuplicateGestures(preferencesPageBindings()) &&
            noDuplicateGestures(categoryMenuModeBindings()) &&
            noDuplicateGestures(responseTimeAdjustmentBindings()) &&
            noDuplicateGestures(sensitivityAdjustmentBindings()) &&
            globalPanelBindings().let { globals ->
                noDuplicateGestures(globals) &&
                    globals.size == 6
            }

    fun adjustmentDoesNotUseScrollGesturesForValues(): Boolean {
        val adjustmentBindings = responseTimeAdjustmentBindings()
        return adjustmentBindings.none { (left, right, action) ->
            action == "DecreaseValue" &&
                GuidedModeNavigation.isPreviousSequence(left, right)
        } && adjustmentBindings.none { (left, right, action) ->
            action == "IncreaseValue" &&
                GuidedModeNavigation.isNextSequence(left, right)
        }
    }

    fun everyModeHasBackCategoriesAndEmergency(): Boolean {
        val required = setOf("BackCancel", "Categories", "Emergency")
        return listOf(
            vocabularyModeBindings(0),
            categoryMenuModeBindings(),
            preferencesPageBindings(),
            responseTimeAdjustmentBindings(),
            sensitivityAdjustmentBindings()
        ).all { bindings ->
            required.all { action -> bindings.any { it.action == action } }
        }
    }
}

object GuidedVocabularyOverlayVisibility {
    /**
     * @param guidedWorkspaceTrainingActive When true, the real workspace is shown even though
     * onboarding is not yet complete — Guided Learning's navigation lessons teach the real
     * Communication Workspace (Guided Training Mode) instead of a blank placeholder screen.
     */
    fun shouldShowOverlay(
        onboardingCompleted: Boolean,
        cameraPermissionGranted: Boolean,
        emergencyActive: Boolean,
        practiceModeOpen: Boolean,
        quickControlsOpen: Boolean,
        guidedWorkspaceTrainingActive: Boolean = false
    ): Boolean =
        (onboardingCompleted || guidedWorkspaceTrainingActive) &&
            cameraPermissionGranted &&
            !emergencyActive &&
            !practiceModeOpen &&
            !quickControlsOpen
}

/**
 * Contextual phrase resolution for Communication Workspace — only visible/open options are eligible.
 */
object WorkspacePhraseResolver {

    fun visibleEntriesForState(
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext,
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
    ): List<GuidedVocabularyEntry> {
        if (state.screenMode != GuidedOverlayScreenMode.Vocabulary) return emptyList()
        return GuidedNavigationController.visiblePhraseEntries(
            entries = GuidedVocabularyCatalog.currentPageEntries(
                state.categoryIndex,
                language,
                uiStrings,
                catalogContext
            ),
            phrasePageIndex = state.phrasePageIndex,
            visibleCap = visibleEntryCap
        )
    }

    /** Mappings used for partial-sequence continuation checks — scoped to the open workspace context. */
    fun continuationMappings(
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext,
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
    ): List<WinkMapping> = when (state.screenMode) {
        GuidedOverlayScreenMode.CategoryMenu ->
            (
                GuidedCategoryShortcuts.allGestures() +
                    listOf(
                        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to
                            GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
                        GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to
                            GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
                    )
                ).map { (left, right) ->
                WinkMapping(left, right, "", isCustom = true, customPhrase = "")
            }
        GuidedOverlayScreenMode.Vocabulary ->
            visibleEntriesForState(state, language, uiStrings, catalogContext, visibleEntryCap)
                .map { entry ->
                    WinkMapping(entry.left, entry.right, "", isCustom = true, customPhrase = entry.phrase)
                }
    }
}

object GuidedVocabularyCatalogValidation {
    private val allPages get() =
        GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, LisaUiStrings.forLanguage(PreferredLanguage.English))

    fun usesReservedGlobalSequence(entry: GuidedVocabularyEntry): Boolean =
        GuidedModeNavigation.isGlobalNavigationSequence(entry.left, entry.right) ||
            isEmergencySequence(entry.left, entry.right)

    fun isAlternatingEyeSequence(left: Int, right: Int): Boolean =
        left >= 1 && right >= 1

    fun isForbiddenVocabularySequence(left: Int, right: Int): Boolean =
        (left to right) in GuidedPageSequences.forbiddenForVocabulary

    fun noVocabularyUsesForbiddenSequences(): Boolean =
        allPages.flatMap { it.entries }
            .none { isForbiddenVocabularySequence(it.left, it.right) }

    fun allVocabularyUsesAlternatingEyePattern(): Boolean =
        allPages.flatMap { it.entries }
            .all { isAlternatingEyeSequence(it.left, it.right) }

    fun sequencesUniqueWithinEachPage(): Boolean =
        allPages.all { page ->
            page.entries.map { it.left to it.right }.distinct().size == page.entries.size
        }

    /** Pages carrying the standard slot layout — Custom / Phrase Management / Adjust Settings / Preferences use their own sequences. */
    private fun standardSlotPages(): List<GuidedCategoryPage> =
        allPages.filter {
            it.category != GuidedVocabularyCategory.Preferences &&
                it.category != GuidedVocabularyCategory.Custom &&
                it.category != GuidedVocabularyCategory.PhraseManagement &&
                it.category != GuidedVocabularyCategory.AdjustSettings
        }

    fun sequencesRepeatAcrossPages(): Boolean {
        val standardPages = standardSlotPages()
        val page0 = standardPages[0].entries.map { it.left to it.right }
        return standardPages.drop(1).all { page ->
            page.entries.map { it.left to it.right } == page0
        }
    }

    fun preferencesAppearsAfterFamily(): Boolean {
        // RC8.5 — Preferences removed; Settings & Controls remains after Family in the menu.
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        return "Preferences" !in titles &&
            "Basic System Controls" !in titles &&
            titles.indexOf("Family") >= 0 &&
            titles.indexOf("Settings & Controls") > titles.indexOf("Family")
    }

    @Deprecated("Basic System Controls removed from category menu; use preferencesAppearsAfterFamily()")
    fun preferencesAppearsAfterBasicSystemControls(): Boolean = preferencesAppearsAfterFamily()

    fun preferencesEntriesOnlyOnPreferencesPage(): Boolean {
        // RC8.5 — Preferences page removed; no Preferences phrase entries remain in the catalog.
        return allPages.none { it.category == GuidedVocabularyCategory.Preferences } &&
            allPages.flatMap { it.entries }.none {
                it.phrase.contains("Adjust response time", ignoreCase = true) ||
                    it.phrase.contains("Adjust sensitivity", ignoreCase = true)
            }
    }

    fun preferencesMatchWorksOnlyWhenOpen(): Boolean {
        // RC8.5 — Preferences page removed; Settings hub is the authoritative adjust entry.
        return GuidedVocabularyCategory.Preferences !in GuidedVocabularyCategory.ordered &&
            GuidedVocabularyCategory.AdjustSettings in GuidedVocabularyCategory.ordered
    }

    fun preferencesShowsCompactControlsOnly(): Boolean =
        GuidedVocabularyCategory.Preferences !in GuidedVocabularyCategory.ordered

    fun preferencesHasAdjustResponseTime(): Boolean =
        GuidedVocabularyCategory.Preferences !in GuidedVocabularyCategory.ordered

    fun preferencesHasAdjustSensitivity(): Boolean =
        GuidedVocabularyCategory.Preferences !in GuidedVocabularyCategory.ordered

    fun categoryShortcutsDoNotConflictWithGlobalNavigation(): Boolean =
        GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation()

    fun categoryShortcutLabelsMatchExpectedSlots(): Boolean {
        val expected = listOf(
            "L2 R1",
            "L1 R2",
            "L3 R1",
            "L1 R3",
            "L3 R2",
            "L2 R3",
            // Settings & Controls uses the canonical entry sequence, not the next page slot.
            formatWinkSequenceShort(
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT,
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT
            )
        )
        return GuidedVocabularyCategory.PAGE_COUNT == expected.size &&
            (0 until GuidedVocabularyCategory.PAGE_COUNT).all { index ->
                GuidedCategoryShortcuts.sequenceLabelForCategory(index) == expected[index]
            }
    }

    fun sameSlotDifferentPhrasesAcrossPages(): Boolean {
        val standardPages = standardSlotPages()
        val slot0Phrases = standardPages.map { it.entries.first().phrase }.distinct()
        return slot0Phrases.size == standardPages.size
    }

    fun medicalPageHasNoEmergencyPhrase(): Boolean =
        allPages[2].entries.none { isEmergencySequence(it.left, it.right) } &&
            allPages[2].entries.none { it.phrase.contains("emergency", ignoreCase = true) }

    fun medicalPageHasPleaseHelpMeNow(): Boolean =
        allPages[2].entries.any { it.phrase == "Please help me now." }

    fun maxSequenceWinksReasonable(maxTotal: Int = 9): Boolean =
        allPages.flatMap { it.entries }
            .all { it.left + it.right <= maxTotal }

    fun allGlobalNavigationGesturesReserved(): Boolean =
        GuidedPageSequences.forbiddenForVocabulary.containsAll(
            listOf(
                GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
                GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
                GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
                GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
                GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
                GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT,
                EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
                1 to 0,
                0 to 1
            )
        )

    /** No two reserved/global gestures — nav, categories, finish-training, emergency — collide. */
    fun noDuplicateReservedGestures(): Boolean {
        val reserved = listOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT,
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
        )
        return reserved.distinct().size == reserved.size
    }
}
