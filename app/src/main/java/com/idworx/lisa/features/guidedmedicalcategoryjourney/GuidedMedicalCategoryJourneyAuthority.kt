package com.idworx.lisa.features.guidedmedicalcategoryjourney

import com.idworx.lisa.GuidedCatalogContext
import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.GuidedVocabularyEntry
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.formatWinkSequenceShort

/**
 * RC8.15 — Guided Communication lessons 16–18: continuous Medical-category journey.
 *
 * Derives labels and sequences from the same production category/phrase definitions
 * Communication uses — never an independent training-only catalogue.
 */
object GuidedMedicalCategoryJourneyAuthority {

    const val ID_MOVE_TO_MEDICAL: String = "nav_move_medical"
    const val ID_OPEN_MEDICAL: String = "nav_select_category"
    const val ID_USE_MEDICAL_PHRASE: String = "nav_select_phrase"

    const val MOVE_LESSON_TITLE: String = "Explore Communication"

    /** Lesson 16 intro — scroll then open the selected category. */
    const val MOVE_DESCRIPTION: String =
        "Scroll to a category, then open it with Select."

    const val MOVE_METHOD_1_TITLE: String = "Method 1"
    const val MOVE_METHOD_1_BODY: String =
        "Use L0 R2 to scroll down one category at a time until Medical is selected."

    const val MOVE_METHOD_1_OPEN_TITLE: String = "Method 1"
    const val MOVE_METHOD_1_OPEN_BODY: String =
        "Medical is selected. Use L1 R1 to open it."

    const val MOVE_METHOD_2_TITLE: String = "Method 2"
    const val MOVE_METHOD_2_BODY: String =
        "Use the category's labelled sequence to jump directly to it."
    /** Format with the production Medical sequence label (e.g. L3 R1). */
    const val MOVE_METHOD_2_MEDICAL_LABEL: String = "Medical is labelled %s."

    /** RC8.25 — Lesson 17 direct-open teaching. */
    const val OPEN_DIRECT_TITLE: String = "Open Medical directly."
    const val OPEN_DIRECT_BODY: String = "Use L3 R1 to open Medical directly."

    /** @deprecated Prefer OPEN_DIRECT_BODY (RC8.25). */
    const val MOVE_PHASE2_DESCRIPTION: String =
        "Every category also has its own labelled sequence."
    /** @deprecated Prefer OPEN_DIRECT_BODY. Format with Medical sequence. */
    const val MOVE_PHASE2_BODY: String =
        "Use the Medical sequence %s to jump directly to Medical and open it."

    const val MOVE_PHASE_FEEDBACK_TITLE: String = "Well done!"
    const val MOVE_PHASE1_FEEDBACK_DETAIL: String =
        "You selected Medical and opened it using L1 R1."
    const val OPEN_DIRECT_FEEDBACK_DETAIL: String =
        "L3 R1 opened Medical directly."
    /** @deprecated Prefer OPEN_DIRECT_FEEDBACK_DETAIL. */
    const val MOVE_PHASE2_FEEDBACK_DETAIL: String = OPEN_DIRECT_FEEDBACK_DETAIL

    /** RC8.25 — Lesson 18 phrase teaching. */
    const val SAY_PHRASE_BODY: String = "Use L2 R1 to say \"I am in pain.\""
    const val SAY_PHRASE_FEEDBACK_DETAIL: String =
        "L2 R1 spoke \"I am in pain.\""

    /** RC8.25 — Lesson 19 Back teaching. */
    const val BACK_TO_CATEGORIES_BODY: String =
        "Use L2 R2 to go back to the category menu."
    const val BACK_TO_CATEGORIES_FEEDBACK_DETAIL: String =
        "You returned to the category menu with L2 R2."

    const val PHASE_ID_METHOD1_SCROLL: String = "Method1ScrollToMedical"
    const val PHASE_ID_METHOD1_OPEN: String = "Method1OpenSelectedMedical"
    const val PHASE_ID_METHOD2_DIRECT: String = "Method2DirectOpenMedical"
    /** @deprecated RC8.34 — use [PHASE_ID_METHOD1_SCROLL]. */
    const val PHASE_ID_PART1_SCROLL: String = PHASE_ID_METHOD1_SCROLL
    /** @deprecated RC8.34 — use [PHASE_ID_METHOD1_OPEN]. */
    const val PHASE_ID_PART1_OPEN: String = PHASE_ID_METHOD1_OPEN
    /** RC8.34 — Method 2 lives in Lesson 16 again (also taught alone as Lesson 17). */
    const val PHASE_ID_PART2_JUMP: String = PHASE_ID_METHOD2_DIRECT

    /** @deprecated Prefer PHASE_ID_METHOD1_SCROLL. */
    const val PHASE_ID_METHOD_1: String = PHASE_ID_METHOD1_SCROLL
    /** @deprecated Prefer PHASE_ID_METHOD2_DIRECT. */
    const val PHASE_ID_METHOD_2: String = PHASE_ID_METHOD2_DIRECT

    /** @deprecated Prefer Method 1 / Method 2 fields (RC8.22). */
    const val MOVE_NEXT_ACTION_INSTRUCTION: String = MOVE_METHOD_1_BODY
    const val MOVE_NEXT_STEP_SCROLL: String = MOVE_METHOD_1_BODY
    const val MOVE_SEQUENCE_EMPHASIS: String = MOVE_METHOD_1_BODY
    const val MOVE_NEXT_STEP_UNTIL_MEDICAL: String = MOVE_METHOD_1_BODY
    const val MOVE_INTRO_LINE_1: String = MOVE_DESCRIPTION
    const val MOVE_ACTION_INSTRUCTION: String = MOVE_METHOD_1_BODY

    val MOVE_INSTRUCTION: String =
        "$MOVE_DESCRIPTION\n\n$MOVE_METHOD_1_BODY"

    const val OPEN_MEDICAL_TITLE: String = OPEN_DIRECT_TITLE

    val medicalCategoryIndex: Int =
        GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.Medical)

    val conversationCategoryIndex: Int =
        GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.Conversation)

    val journeyLessonIds: Set<String> = setOf(
        ID_MOVE_TO_MEDICAL,
        ID_OPEN_MEDICAL,
        ID_USE_MEDICAL_PHRASE
    )

    fun isJourneyLessonId(id: String): Boolean = id in journeyLessonIds

    fun isJourneyAction(action: NavigationAction): Boolean = when (action) {
        NavigationAction.MoveToMedicalCategory,
        NavigationAction.SelectCategory,
        NavigationAction.SelectPhrase -> true
        else -> false
    }

    fun moveDownSequenceLabel(): String =
        formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)

    fun openSelectedSequenceLabel(): String =
        formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)

    fun openMedicalSequenceLabel(): String =
        GuidedCategoryShortcuts.sequenceLabelForCategory(medicalCategoryIndex)

    fun openMedicalGesture(): Pair<Int, Int> =
        GuidedCategoryShortcuts.gestureForCategory(medicalCategoryIndex)

    fun matchesOpenMedical(left: Int, right: Int): Boolean =
        GuidedCategoryShortcuts.categoryIndexForGesture(left, right) == medicalCategoryIndex

    fun matchesOpenSelected(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isSelectSequence(left, right)

    /** Production Category Menu with General Conversation selected (lesson 16 entry). */
    fun isLesson16StartState(state: com.idworx.lisa.GuidedNavigationState): Boolean =
        state.screenMode == com.idworx.lisa.GuidedOverlayScreenMode.CategoryMenu &&
            state.categoryMenuSelection == conversationCategoryIndex &&
            state.preferencesAdjustMode == com.idworx.lisa.GuidedPreferencesAdjustMode.None

    /** Medical row selected in the category list (end of lesson 16 scroll stage). */
    fun isMedicalSelectedInCategoryMenu(state: com.idworx.lisa.GuidedNavigationState): Boolean =
        state.screenMode == com.idworx.lisa.GuidedOverlayScreenMode.CategoryMenu &&
            state.categoryMenuSelection == medicalCategoryIndex

    /** Real Medical phrase workspace open (end of lesson 17 / start of 18). */
    fun isMedicalPhraseWorkspaceOpen(state: com.idworx.lisa.GuidedNavigationState): Boolean =
        state.screenMode == com.idworx.lisa.GuidedOverlayScreenMode.Vocabulary &&
            state.categoryIndex == medicalCategoryIndex &&
            state.preferencesAdjustMode == com.idworx.lisa.GuidedPreferencesAdjustMode.None

    /** RC8.24 — Medical opened via production Select (L1 R1). */
    fun isMedicalOpenedViaSelect(state: com.idworx.lisa.GuidedNavigationState): Boolean =
        isMedicalPhraseWorkspaceOpen(state) &&
            state.categoryNavigationCause == com.idworx.lisa.CategoryNavigationCause.OPEN_SELECTED

    /** RC8.24 — Medical opened via production direct shortcut (L3 R1). */
    fun isMedicalOpenedViaDirectShortcut(state: com.idworx.lisa.GuidedNavigationState): Boolean =
        isMedicalPhraseWorkspaceOpen(state) &&
            state.categoryNavigationCause == com.idworx.lisa.CategoryNavigationCause.DIRECT_SHORTCUT

    /**
     * RC8.34 — Method 1 open gate: fresh L1 R1 while Medical was selected in the category menu
     * must produce a visible Medical workspace via OPEN_SELECTED. Selection alone / stale open
     * / direct shortcut must not pass.
     */
    fun isMethod1OpenCompleted(
        before: com.idworx.lisa.GuidedNavigationState,
        after: com.idworx.lisa.GuidedNavigationState,
        left: Int,
        right: Int
    ): Boolean {
        if (!matchesOpenSelected(left, right)) return false
        if (!isMedicalSelectedInCategoryMenu(before)) return false
        if (isMedicalPhraseWorkspaceOpen(before)) return false
        return isMedicalOpenedViaSelect(after)
    }

    /**
     * RC8.34 — Method 2 gate: fresh L3 R1 from category menu must open Medical via
     * DIRECT_SHORTCUT. Stale Method 1 open state must not pass.
     */
    fun isMethod2DirectCompleted(
        before: com.idworx.lisa.GuidedNavigationState,
        after: com.idworx.lisa.GuidedNavigationState,
        left: Int,
        right: Int
    ): Boolean {
        if (!matchesOpenMedical(left, right)) return false
        if (before.screenMode != com.idworx.lisa.GuidedOverlayScreenMode.CategoryMenu) return false
        if (isMedicalPhraseWorkspaceOpen(before)) return false
        return isMedicalOpenedViaDirectShortcut(after)
    }

    /** Deterministic Lesson 16 Method 2 start — Category Menu, Conversation selected, no open. */
    fun isMethod2StartState(state: com.idworx.lisa.GuidedNavigationState): Boolean =
        isLesson16StartState(state) && !isMedicalPhraseWorkspaceOpen(state)

    /**
     * First built-in Medical phrase from the production vocabulary catalogue
     * (currently "I am in pain." at page slot 0 → L2 R1).
     */
    fun firstMedicalPhraseEntry(
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language),
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): GuidedVocabularyEntry {
        val page = GuidedVocabularyCatalog.categoryAt(
            medicalCategoryIndex,
            language,
            uiStrings,
            catalogContext
        ) ?: error("Medical category missing from production catalogue")
        return page.entries.firstOrNull()
            ?: error("Medical category has no production phrases")
    }

    fun firstMedicalPhraseSequenceLabel(
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language)
    ): String = firstMedicalPhraseEntry(language, uiStrings).sequenceLabel

    /** RC8.25 — true when [entry] is the production first Medical phrase (Lesson 18 target). */
    fun matchesFirstMedicalPhrase(
        entry: GuidedVocabularyEntry,
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language)
    ): Boolean {
        val target = firstMedicalPhraseEntry(language, uiStrings)
        return entry.left == target.left &&
            entry.right == target.right &&
            entry.phrase.equals(target.phrase, ignoreCase = true)
    }

    fun sayPhraseLessonTitle(
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language)
    ): String = "Say \"${firstMedicalPhraseEntry(language, uiStrings).phrase}\""

    fun downsFromConversationToMedical(): Int = medicalCategoryIndex

    fun exactlyOneCategorySelected(selectedIndex: Int, pageCount: Int = GuidedVocabularyCategory.PAGE_COUNT): Boolean =
        selectedIndex in 0 until pageCount
}
