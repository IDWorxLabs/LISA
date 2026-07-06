package com.idworx.lisa.validation.authority

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedCatalogContext
import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationGestureAudit
import com.idworx.lisa.GuidedNavigationGestureAudit.GestureBinding
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPageSequences
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCatalogValidation
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.isEmergencySequence
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

/**
 * GESTURE_CONFLICT_AUTHORITY_V1
 *
 * Verifies gesture registry safety, mode-aware uniqueness, and deterministic resolution
 * against the LIC, LIEC, and LVC.
 */
object GestureConflictAuthorityV1 {

    const val AUTHORITY_NAME: String = "GESTURE_CONFLICT_AUTHORITY_V1"
    const val PASS_TOKEN: String = "GESTURE_CONFLICT_AUTHORITY_V1_PASS"

    private val LIC_ARTICLES = listOf(
        "Article 2.1.1.1 — Global gestures are reserved",
        "Article 2.1.1.4 — Constitutional global gesture assignments",
        "Article 2.1.1.6 — Global gestures take precedence over local gestures",
        "Article 2.2.1.1 — Vocabulary gestures are local",
        "Article 2.2.1.2 — Same local gesture may mean different things in different categories/pages",
        "Article 2.2.2.1 — One gesture, one meaning, one context",
        "Article 2.3.1.1 — Reserved gestures shall not appear as vocabulary",
        "Article 2.4.1.1 — Single accidental gesture must not trigger communication",
        "Article 2.4.2.1 — Ambiguous gestures are safety failures",
        "Article 5.4.1.5 — Global navigation prevails over local vocabulary"
    )

    private val LIEC_ARTICLES = listOf(
        "Article 3.1.1.1 — Reserved gesture registry",
        "Article 3.2.1.1 — Global gesture registry",
        "Article 3.3.1.1 — Local gesture registries scoped to context",
        "Article 3.3.1.4 — Current-context-only processing",
        "Article 3.4.1.4 — One active meaning per gesture",
        "Article 3.5.1.1 — Deterministic gesture resolution order",
        "Article 3.5.1.3 — Global and emergency assignments preserved first",
        "Article 3.6.1.1 — Gesture fatigue engineering",
        "Article 6.3.1.4 — Gesture safety mechanisms"
    )

    private val LVC_ARTICLES = listOf(
        "Article 3.3.1.1 — Registry collision, duplication, namespace violation",
        "Article 3.3.1.2 — Resolution order and accidental activation prohibition",
        "Article 3.3.1.3 — Reserved gestures not vocabulary selections",
        "Article 4.1.1.2 — Evidence record requirements",
        "Article 5.1.1.1 — Official outcome taxonomy"
    )

    val constitutionalGlobalGestures: Map<Pair<Int, Int>, String> = mapOf(
        (GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT) to "Scroll Up / Previous",
        (GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT) to "Scroll Down / Next",
        (GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT) to "Select / Save",
        (GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT) to "Back / Cancel",
        (GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT) to "Categories",
        (EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS) to "Emergency"
    )

    val expectedCategoryShortcuts: List<Pair<String, Pair<Int, Int>>> = listOf(
        "Conversation" to (2 to 1),
        "Basic Needs" to (1 to 2),
        "Medical" to (3 to 1),
        "Family" to (1 to 3),
        "Basic System Controls" to (3 to 2),
        "Preferences" to (2 to 3)
    )

    fun validate(
        language: PreferredLanguage = PreferredLanguage.English,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
    ): ValidationReport {
        val uiStrings = LisaUiStrings.forLanguage(language)
        val modeContexts = GuidedNavigationAuthorityV1.buildModeContexts(catalogContext)
        val checks = buildList {
            addAll(ReservedGestureRegistryAudit.run())
            addAll(GlobalGestureAudit.run())
            addAll(LocalVocabularyGestureAudit.run(uiStrings, catalogContext))
            addAll(CategoryShortcutGestureAudit.run())
            addAll(AdjustmentGestureAudit.run())
            addAll(ModeConflictAudit.run(modeContexts, catalogContext))
            addAll(ResolutionOrderAudit.run(uiStrings, catalogContext))
        }

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        val passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null

        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = passToken,
            evidenceSummary = buildEvidenceSummary(checks),
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = checks.mapNotNull { it.observation }.distinct(),
            affectedLicArticles = LIC_ARTICLES,
            affectedLiecArticles = LIEC_ARTICLES,
            affectedLvcArticles = LVC_ARTICLES,
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = buildValidationReasoning(checks, outcome),
            subsystem = "Gesture Conflict"
        )
    }

    private fun buildEvidenceSummary(checks: List<ValidationCheckResult>): String =
        "Gesture Conflict Authority executed ${checks.size} deterministic checks across reserved, global, " +
            "local vocabulary, category shortcut, adjustment, mode conflict, and resolution-order domains. " +
            "Passed: ${checks.count { it.passed }}. Failed: ${checks.count { !it.passed }}."

    private fun buildValidationReasoning(
        checks: List<ValidationCheckResult>,
        outcome: ValidationOutcome
    ): String = when (outcome) {
        ValidationOutcome.PASS ->
            "All ${checks.size} gesture conflict checks passed. Reserved and global gestures are intact, " +
                "local gestures are unique within context, mode-specific assignments do not collide, and " +
                "deterministic resolution preserves emergency and global precedence."
        ValidationOutcome.PASS_WITH_OBSERVATIONS ->
            "All ${checks.size} required checks passed with documented observations."
        ValidationOutcome.FAIL ->
            "${checks.count { !it.passed }} of ${checks.size} gesture conflict checks failed. " +
                "Ambiguous or conflicting gesture assignments must be remediated before release."
        ValidationOutcome.BLOCKED ->
            "Gesture conflict validation could not be completed reliably."
        ValidationOutcome.NOT_APPLICABLE ->
            "Authority not applicable to declared scope."
    }

    // --- Domain 1: Reserved Gesture Registry Audit ---

    object ReservedGestureRegistryAudit {
        fun run(): List<ValidationCheckResult> = listOf(
            constitutionalGlobalsRegistered(),
            reservedNeverVocabularyPhrase(),
            reservedNeverCategoryShortcut(),
            reservedNeverAdjustmentGesture(),
            forbiddenSingleEyeProhibitedInRegistry(),
            allGlobalGesturesInForbiddenSet()
        )

        fun constitutionalGlobalsRegistered(): ValidationCheckResult {
            val bindings = GuidedNavigationGestureAudit.globalPanelBindings()
            val passed = bindings.size == constitutionalGlobalGestures.size &&
                verifyGlobalBindingActions(bindings) &&
                constitutionalGlobalGestures.keys.all { (left, right) ->
                    bindings.any { it.left == left && it.right == right }
                }
            return check(
                id = "RESV_001",
                description = "Reserved global gestures exist with constitutional assignments",
                passed = passed,
                remediation = "Register all six constitutional global gestures in the global panel registry."
            )
        }

        private fun verifyGlobalBindingActions(bindings: List<GestureBinding>): Boolean {
            val expected = setOf("ScrollUp", "ScrollDown", "SelectSave", "BackCancel", "Categories", "Emergency")
            return bindings.map { it.action }.toSet() == expected
        }

        fun reservedNeverVocabularyPhrase(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.noVocabularyUsesForbiddenSequences()
            return check(
                id = "RESV_002",
                description = "Reserved gestures are never assigned as vocabulary phrase gestures",
                passed = passed,
                remediation = "Remove reserved global and emergency sequences from all vocabulary pages."
            )
        }

        fun reservedNeverCategoryShortcut(): ValidationCheckResult {
            val passed = GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation()
            return check(
                id = "RESV_003",
                description = "Reserved gestures are never assigned as category shortcut gestures",
                passed = passed,
                remediation = "Reassign category shortcuts that collide with global or emergency gestures."
            )
        }

        fun reservedNeverAdjustmentGesture(): ValidationCheckResult {
            val decrease = GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT
            val increase = GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT
            val passed = !constitutionalGlobalGestures.containsKey(decrease) &&
                !constitutionalGlobalGestures.containsKey(increase)
            return check(
                id = "RESV_004",
                description = "Reserved global gestures are never assigned as adjustment value gestures",
                passed = passed,
                remediation = "Keep L3 R1 and L1 R3 as adjustment-only; do not reuse global assignments."
            )
        }

        fun forbiddenSingleEyeProhibitedInRegistry(): ValidationCheckResult {
            val passed = GuidedPageSequences.forbiddenForVocabulary.contains(1 to 0) &&
                GuidedPageSequences.forbiddenForVocabulary.contains(0 to 1)
            return check(
                id = "RESV_005",
                description = "Single-eye accidental triggers L1 R0 and L0 R1 are prohibited in vocabulary registry",
                passed = passed,
                remediation = "Add L1 R0 and L0 R1 to forbidden vocabulary sequences."
            )
        }

        fun allGlobalGesturesInForbiddenSet(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.allGlobalNavigationGesturesReserved()
            return check(
                id = "RESV_006",
                description = "All global and emergency gestures appear in vocabulary forbidden registry",
                passed = passed,
                remediation = "Extend forbiddenForVocabulary to include every reserved global gesture."
            )
        }

        fun vocabularyUsesReservedGesture(left: Int, right: Int): Boolean =
            GuidedVocabularyCatalogValidation.isForbiddenVocabularySequence(left, right)
    }

    // --- Domain 2: Global Gesture Audit ---

    object GlobalGestureAudit {
        fun run(): List<ValidationCheckResult> = listOf(
            oneCommandPerGlobalGesture(),
            noDuplicateGlobalCommands(),
            globalMeaningStableAcrossModes(),
            emergencyHighestPrecedence(),
            globalPrecedenceOverLocal()
        )

        fun oneCommandPerGlobalGesture(): ValidationCheckResult {
            val bindings = GuidedNavigationGestureAudit.globalPanelBindings()
            val passed = bindings.size == constitutionalGlobalGestures.size &&
                GuidedNavigationGestureAudit.noDuplicateGestures(bindings)
            return check(
                id = "GLOB_001",
                description = "Each global gesture maps to exactly one global command",
                passed = passed,
                remediation = "Ensure bijective mapping between global gestures and global commands."
            )
        }

        fun noDuplicateGlobalCommands(): ValidationCheckResult {
            val bindings = GuidedNavigationGestureAudit.globalPanelBindings()
            val passed = bindings.map { it.action }.distinct().size == bindings.size
            return check(
                id = "GLOB_002",
                description = "No two global commands share the same gesture",
                passed = passed,
                remediation = "Remove duplicate gesture assignments from global panel bindings."
            )
        }

        fun globalMeaningStableAcrossModes(): ValidationCheckResult {
            val modeBindings = listOf(
                GuidedNavigationGestureAudit.vocabularyModeBindings(0),
                GuidedNavigationGestureAudit.categoryMenuModeBindings(),
                GuidedNavigationGestureAudit.preferencesPageBindings(),
                GuidedNavigationGestureAudit.responseTimeAdjustmentBindings(),
                GuidedNavigationGestureAudit.sensitivityAdjustmentBindings()
            )
            val globals = GuidedNavigationGestureAudit.globalPanelBindings()
            val passed = modeBindings.all { bindings ->
                globals.all { global ->
                    bindings.any { it.left == global.left && it.right == global.right && it.action == global.action }
                }
            }
            return check(
                id = "GLOB_003",
                description = "Global gesture meaning does not vary by mode",
                passed = passed,
                remediation = "Keep global gesture action names identical in every mode binding set."
            )
        }

        fun emergencyHighestPrecedence(): ValidationCheckResult {
            val emergencyPresent = GuidedNavigationGestureAudit.globalPanelBindings()
                .any { it.action == "Emergency" && isEmergencySequence(it.left, it.right) }
            val notInVocabulary = GuidedVocabularyCatalogValidation.noVocabularyUsesForbiddenSequences()
            return check(
                id = "GLOB_004",
                description = "Emergency has highest precedence and is not vocabulary",
                passed = emergencyPresent && notInVocabulary,
                remediation = "Register L6 R0 as Emergency globally and exclude from all vocabulary registries."
            )
        }

        fun globalPrecedenceOverLocal(): ValidationCheckResult {
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                LisaUiStrings.forLanguage(PreferredLanguage.English)
            )
            val passed = pages.flatMap { it.entries }.none { entry ->
                GuidedModeNavigation.isGlobalNavigationSequence(entry.left, entry.right) ||
                    isEmergencySequence(entry.left, entry.right)
            }
            return check(
                id = "GLOB_005",
                description = "Global gestures take precedence over local gestures (locals do not shadow globals)",
                passed = passed,
                remediation = "Remove global gesture sequences from vocabulary slot assignments."
            )
        }

        fun globalGesturesUnique(bindings: List<GestureBinding>): Boolean =
            GuidedNavigationGestureAudit.noDuplicateGestures(bindings)
    }

    // --- Domain 3: Local Vocabulary Gesture Audit ---

    object LocalVocabularyGestureAudit {
        fun run(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> {
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                uiStrings,
                catalogContext
            )
            return listOf(
                uniqueWithinEachPage(pages),
                reuseAcrossCategoriesAllowed(),
                localsDoNotUseReserved(),
                singleEyeTriggersProhibited(),
                noBothEyeBlinkTriggers(),
                sequencesShortEnoughForFatigue(),
                alternatingEyePatternRequired(),
                hiddenPageCannotTrigger(uiStrings, catalogContext)
            )
        }

        fun uniqueWithinEachPage(pages: List<com.idworx.lisa.GuidedCategoryPage>): ValidationCheckResult {
            val passed = pages.all { page ->
                page.entries.map { it.left to it.right }.distinct().size == page.entries.size
            }
            return check(
                id = "VOCAB_001",
                description = "Local vocabulary gestures are unique within each category/page",
                passed = passed,
                remediation = "Assign distinct gesture sequences to each entry on the same page."
            )
        }

        fun reuseAcrossCategoriesAllowed(): ValidationCheckResult {
            val slotsRepeat = GuidedVocabularyCatalogValidation.sequencesRepeatAcrossPages()
            val phrasesDiffer = GuidedVocabularyCatalogValidation.sameSlotDifferentPhrasesAcrossPages()
            return check(
                id = "VOCAB_002",
                description = "Local vocabulary gestures may repeat across categories/pages with different phrases",
                passed = slotsRepeat && phrasesDiffer,
                remediation = "Reuse standard slot sequences across pages; vary phrase content by category."
            )
        }

        fun localsDoNotUseReserved(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.noVocabularyUsesForbiddenSequences()
            return check(
                id = "VOCAB_003",
                description = "Local vocabulary gestures do not use reserved global gestures",
                passed = passed,
                remediation = "Replace vocabulary slots that use reserved global or emergency sequences."
            )
        }

        fun singleEyeTriggersProhibited(): ValidationCheckResult {
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                LisaUiStrings.forLanguage(PreferredLanguage.English)
            )
            val passed = pages.flatMap { it.entries }.none { entry ->
                (entry.left == 1 && entry.right == 0) || (entry.left == 0 && entry.right == 1)
            }
            return check(
                id = "VOCAB_004",
                description = "Local vocabulary does not use single-eye accidental triggers L1 R0 or L0 R1",
                passed = passed,
                remediation = "Remove L1 R0 and L0 R1 from all vocabulary assignments."
            )
        }

        fun noBothEyeBlinkTriggers(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.allVocabularyUsesAlternatingEyePattern()
            return check(
                id = "VOCAB_005",
                description = "Local vocabulary gestures do not use zero-wink both-eye blink triggers",
                passed = passed,
                remediation = "Require at least one left and one right wink for every vocabulary gesture."
            )
        }

        fun sequencesShortEnoughForFatigue(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.maxSequenceWinksReasonable()
            return check(
                id = "VOCAB_006",
                description = "Local vocabulary gestures stay short enough for fatigue safety",
                passed = passed,
                remediation = "Reduce total wink count per vocabulary gesture to fatigue-safe limits."
            )
        }

        fun alternatingEyePatternRequired(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.allVocabularyUsesAlternatingEyePattern()
            return check(
                id = "VOCAB_007",
                description = "Local vocabulary uses alternating eye pattern (minimum one wink per eye)",
                passed = passed,
                remediation = "Require at least one left and one right wink for every vocabulary gesture."
            )
        }

        fun hiddenPageCannotTrigger(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val preferencesIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                uiStrings,
                catalogContext
            )
            val adjustEntry = pages[preferencesIndex].entries.first { it.phrase.contains("Adjust response time") }
            val preferencesState = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = preferencesIndex,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val conversationState = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = 0,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val onPreferences = ResolutionOrderAudit.process(
                adjustEntry.left,
                adjustEntry.right,
                preferencesState,
                uiStrings,
                catalogContext
            )
            val onConversation = ResolutionOrderAudit.process(
                adjustEntry.left,
                adjustEntry.right,
                conversationState,
                uiStrings,
                catalogContext
            )
            val opensAdjustOnPreferences = onPreferences is GuidedSequenceResult.Navigate &&
                onPreferences.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.ResponseTime
            val doesNotOpenAdjustOnConversation = onConversation !is GuidedSequenceResult.Navigate ||
                onConversation.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
            val passed = opensAdjustOnPreferences && doesNotOpenAdjustOnConversation
            return check(
                id = "VOCAB_008",
                description = "Hidden-page vocabulary cannot trigger outside its active page",
                passed = passed,
                remediation = "Scope preference and system actions to active page context before navigation."
            )
        }

        fun isUniqueWithinPage(entries: List<Pair<Int, Int>>): Boolean =
            entries.distinct().size == entries.size
    }

    // --- Domain 4: Category Shortcut Gesture Audit ---

    object CategoryShortcutGestureAudit {
        fun run(): List<ValidationCheckResult> = listOf(
            shortcutsOnlyInCategoryMenuMode(),
            shortcutsDoNotConflictWithReserved(),
            expectedShortcutAssignments(),
            eachShortcutOpensOneCategory(),
            noShortcutOpensTwoCategories(),
            shortcutsMayReuseVocabularySlots()
        )

        fun shortcutsOnlyInCategoryMenuMode(): ValidationCheckResult {
            val menuHas = GuidedNavigationGestureAudit.categoryMenuModeBindings()
                .any { it.action.startsWith("CategoryShortcut:") }
            val vocabLacks = GuidedNavigationGestureAudit.vocabularyModeBindings(0)
                .none { it.action.startsWith("CategoryShortcut:") }
            val adjustLacks = GuidedNavigationGestureAudit.responseTimeAdjustmentBindings()
                .none { it.action.startsWith("CategoryShortcut:") }
            return check(
                id = "CAT_001",
                description = "Category shortcut gestures are active only in Category Menu Mode bindings",
                passed = menuHas && vocabLacks && adjustLacks,
                remediation = "Restrict CategoryShortcut bindings to Category Menu Mode."
            )
        }

        fun shortcutsDoNotConflictWithReserved(): ValidationCheckResult {
            val passed = GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation()
            return check(
                id = "CAT_002",
                description = "Category shortcut gestures do not conflict with reserved global gestures",
                passed = passed,
                remediation = "Reassign shortcuts that use global or emergency gesture sequences."
            )
        }

        fun expectedShortcutAssignments(): ValidationCheckResult {
            val passed = GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots()
            return check(
                id = "CAT_003",
                description = "Category shortcuts match expected assignments (L2 R1 through L2 R3)",
                passed = passed,
                remediation = "Align category shortcuts with constitutional category shortcut table."
            )
        }

        fun eachShortcutOpensOneCategory(): ValidationCheckResult {
            val gestures = GuidedCategoryShortcuts.allGestures()
            val passed = gestures.size == GuidedCategoryShortcuts.SHORTCUT_COUNT &&
                gestures.distinct().size == gestures.size &&
                gestures.indices.all { index ->
                    GuidedCategoryShortcuts.categoryIndexForGesture(gestures[index].first, gestures[index].second) == index
                }
            return check(
                id = "CAT_004",
                description = "Every category shortcut opens exactly one category",
                passed = passed,
                remediation = "Ensure bijective mapping between shortcut gestures and category indices."
            )
        }

        fun noShortcutOpensTwoCategories(): ValidationCheckResult {
            val passed = GuidedCategoryShortcuts.allGestures()
                .groupBy { it }
                .all { it.value.size == 1 }
            return check(
                id = "CAT_005",
                description = "No category shortcut gesture opens two categories",
                passed = passed,
                remediation = "Remove duplicate shortcut gestures targeting multiple categories."
            )
        }

        fun shortcutsMayReuseVocabularySlots(): ValidationCheckResult {
            val firstSlot = GuidedPageSequences.slotAt(0)
            val shortcutZero = GuidedCategoryShortcuts.gestureForCategory(0)
            val passed = firstSlot == shortcutZero
            return check(
                id = "CAT_006",
                description = "Category shortcuts may reuse local vocabulary slots (mode-dependent meaning)",
                passed = passed,
                remediation = "Document slot reuse; meaning must differ by mode, not by collision."
            )
        }

        fun shortcutActiveOnlyInCategoryMenu(left: Int, right: Int, modeBindings: List<GestureBinding>): Boolean =
            modeBindings.none { it.left == left && it.right == right && it.action.startsWith("CategoryShortcut:") }
    }

    // --- Domain 5: Adjustment Gesture Audit ---

    object AdjustmentGestureAudit {
        fun run(): List<ValidationCheckResult> = listOf(
            adjustmentOnlyInAdjustmentModes(),
            adjustmentDoNotConflictWithReserved(),
            decreaseAndIncreaseAssignments(),
            noDuplicateImportantInAdjustmentMode(),
            scrollRemainsScrollInAdjustment()
        )

        fun adjustmentOnlyInAdjustmentModes(): ValidationCheckResult {
            val adjustModes = listOf(
                GuidedNavigationGestureAudit.responseTimeAdjustmentBindings(),
                GuidedNavigationGestureAudit.sensitivityAdjustmentBindings()
            )
            val nonAdjust = listOf(
                GuidedNavigationGestureAudit.vocabularyModeBindings(0),
                GuidedNavigationGestureAudit.categoryMenuModeBindings()
            )
            val passed = adjustModes.all { it.any { b -> b.action == "DecreaseValue" } && it.any { b -> b.action == "IncreaseValue" } } &&
                nonAdjust.all { bindings -> bindings.none { it.action == "DecreaseValue" || it.action == "IncreaseValue" } }
            return check(
                id = "ADJ_001",
                description = "Adjustment gestures are active only in adjustment mode binding sets",
                passed = passed,
                remediation = "Restrict L3 R1/L1 R3 value gestures to adjustment modes."
            )
        }

        fun adjustmentDoNotConflictWithReserved(): ValidationCheckResult {
            val decrease = GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT
            val increase = GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT
            val passed = !constitutionalGlobalGestures.containsKey(decrease) &&
                !constitutionalGlobalGestures.containsKey(increase) &&
                !isEmergencySequence(decrease.first, decrease.second) &&
                !isEmergencySequence(increase.first, increase.second)
            return check(
                id = "ADJ_002",
                description = "Adjustment gestures do not conflict with reserved global gestures",
                passed = passed,
                remediation = "Use L3 R1 and L1 R3 exclusively for adjustment value changes."
            )
        }

        fun decreaseAndIncreaseAssignments(): ValidationCheckResult {
            val bindings = GuidedNavigationGestureAudit.responseTimeAdjustmentBindings()
            val passed = bindings.any {
                it.left == GuidedModeNavigation.DECREASE_VALUE_LEFT &&
                    it.right == GuidedModeNavigation.DECREASE_VALUE_RIGHT &&
                    it.action == "DecreaseValue"
            } && bindings.any {
                it.left == GuidedModeNavigation.INCREASE_VALUE_LEFT &&
                    it.right == GuidedModeNavigation.INCREASE_VALUE_RIGHT &&
                    it.action == "IncreaseValue"
            }
            return check(
                id = "ADJ_003",
                description = "L3 R1 decreases value and L1 R3 increases value in adjustment modes",
                passed = passed,
                remediation = "Register L3 R1 as DecreaseValue and L1 R3 as IncreaseValue."
            )
        }

        fun noDuplicateImportantInAdjustmentMode(): ValidationCheckResult {
            val passed = GuidedNavigationGestureAudit.noDuplicateGestures(
                GuidedNavigationGestureAudit.responseTimeAdjustmentBindings()
            ) && GuidedNavigationGestureAudit.noDuplicateGestures(
                GuidedNavigationGestureAudit.sensitivityAdjustmentBindings()
            )
            return check(
                id = "ADJ_004",
                description = "Adjustment modes do not duplicate save/cancel/categories/emergency/scroll gestures",
                passed = passed,
                remediation = "Resolve duplicate gesture bindings in adjustment mode registries."
            )
        }

        fun scrollRemainsScrollInAdjustment(): ValidationCheckResult {
            val passed = GuidedNavigationGestureAudit.adjustmentDoesNotUseScrollGesturesForValues()
            return check(
                id = "ADJ_005",
                description = "L2 R0 and L0 R2 remain scroll gestures, not adjustment value gestures",
                passed = passed,
                remediation = "Keep scroll and value change gestures separate in adjustment modes."
            )
        }
    }

    // --- Domain 6: Mode Conflict Audit ---

    object ModeConflictAudit {
        fun run(
            modeContexts: List<GuidedNavigationAuthorityV1.GuidedModeContext>,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> {
            val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
            return listOf(
                oneMeaningPerActiveMode(modeContexts),
                allVocabularyPagesUniqueWithinPage(uiStrings, catalogContext),
            phraseGesturesOnlyInVocabularyModes(modeContexts),
            categoryShortcutsOnlyInMenu(modeContexts),
                adjustmentGesturesOnlyInAdjustModes(),
                globalsActiveEverywhere(modeContexts)
            )
        }

        fun oneMeaningPerActiveMode(
            modeContexts: List<GuidedNavigationAuthorityV1.GuidedModeContext>
        ): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                GuidedNavigationGestureAudit.noDuplicateGestures(ctx.gestureBindings)
            } && GuidedNavigationGestureAudit.auditAllModes()
            return check(
                id = "MODE_001",
                description = "Every active gesture has exactly one meaning in each guided mode",
                passed = passed,
                remediation = "Resolve duplicate active gesture meanings within each mode context."
            )
        }

        fun allVocabularyPagesUniqueWithinPage(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val passed = (0 until GuidedVocabularyCategory.PAGE_COUNT).all { pageIndex ->
                GuidedNavigationGestureAudit.noDuplicateGestures(
                    GuidedNavigationGestureAudit.vocabularyModeBindings(pageIndex)
                )
            }
            return check(
                id = "MODE_002",
                description = "All vocabulary category pages maintain unique gestures within page",
                passed = passed,
                remediation = "Fix duplicate vocabulary gestures on any category page."
            )
        }

        fun phraseGesturesOnlyInVocabularyModes(
            modeContexts: List<GuidedNavigationAuthorityV1.GuidedModeContext>
        ): ValidationCheckResult {
            val vocabModes = modeContexts.filter {
                it.logicalMode == GuidedNavigationAuthorityV1.GuidedLogicalMode.Vocabulary ||
                    it.logicalMode == GuidedNavigationAuthorityV1.GuidedLogicalMode.Preferences
            }
            val others = modeContexts.filter {
                it.logicalMode != GuidedNavigationAuthorityV1.GuidedLogicalMode.Vocabulary &&
                    it.logicalMode != GuidedNavigationAuthorityV1.GuidedLogicalMode.Preferences
            }
            val passed = vocabModes.all { it.gestureBindings.any { b -> b.action.startsWith("Phrase:") } || it.logicalMode == GuidedNavigationAuthorityV1.GuidedLogicalMode.Preferences } &&
                others.all { it.gestureBindings.none { b -> b.action.startsWith("Phrase:") } }
            return check(
                id = "MODE_003",
                description = "Local phrase gestures are active only in Vocabulary and Preferences modes",
                passed = passed,
                remediation = "Remove Phrase bindings from non-vocabulary mode registries."
            )
        }

        fun categoryShortcutsOnlyInMenu(
            modeContexts: List<GuidedNavigationAuthorityV1.GuidedModeContext>
        ): ValidationCheckResult {
            val menu = modeContexts.first { it.logicalMode == GuidedNavigationAuthorityV1.GuidedLogicalMode.CategoryMenu }
            val others = modeContexts.filter { it.logicalMode != GuidedNavigationAuthorityV1.GuidedLogicalMode.CategoryMenu }
            val passed = menu.gestureBindings.any { it.action.startsWith("CategoryShortcut:") } &&
                others.all { ctx -> ctx.gestureBindings.none { it.action.startsWith("CategoryShortcut:") } }
            return check(
                id = "MODE_004",
                description = "Category shortcut gestures are active only in Category Menu Mode",
                passed = passed,
                remediation = "Remove CategoryShortcut bindings outside Category Menu Mode."
            )
        }

        fun adjustmentGesturesOnlyInAdjustModes(): ValidationCheckResult {
            val result = AdjustmentGestureAudit.adjustmentOnlyInAdjustmentModes()
            return result.copy(checkId = "MODE_005", description = "Adjustment gestures are active only in Adjustment modes")
        }

        fun globalsActiveEverywhere(
            modeContexts: List<GuidedNavigationAuthorityV1.GuidedModeContext>
        ): ValidationCheckResult {
            val required = setOf("ScrollUp", "ScrollDown", "SelectSave", "BackCancel", "Categories", "Emergency")
            val passed = modeContexts.all { ctx ->
                required.all { action -> ctx.gestureBindings.any { it.action == action } }
            }
            return check(
                id = "MODE_006",
                description = "Global gestures are active in every guided mode binding set",
                passed = passed,
                remediation = "Include full global gesture set in every mode registry."
            )
        }

        fun hasDuplicateMeaningInMode(bindings: List<GestureBinding>): Boolean =
            !GuidedNavigationGestureAudit.noDuplicateGestures(bindings)
    }

    // --- Domain 7: Resolution Order Audit ---

    object ResolutionOrderAudit {
        fun run(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            emergencyCannotBeShadowed(),
            globalGesturesCannotBeShadowed(uiStrings, catalogContext),
            hiddenPageVocabularyCannotTrigger(uiStrings, catalogContext),
            inactiveCategoryShortcutsCannotTrigger(uiStrings, catalogContext),
            globalBeforeLocalInVocabulary(uiStrings, catalogContext),
            adjustmentValueBeforeScrollConflictAbsent()
        )

        fun emergencyCannotBeShadowed(): ValidationCheckResult {
            val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
            val vocabularyHasEmergency = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                uiStrings
            ).flatMap { it.entries }.any { isEmergencySequence(it.left, it.right) }
            val globalHasEmergency = GuidedNavigationGestureAudit.globalPanelBindings()
                .any { it.action == "Emergency" }
            return check(
                id = "RESOL_001",
                description = "Emergency cannot be shadowed by vocabulary or local assignments",
                passed = globalHasEmergency && !vocabularyHasEmergency,
                remediation = "Remove L6 R0 from vocabulary; preserve global emergency handler."
            )
        }

        fun globalGesturesCannotBeShadowed(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = 0,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val categoriesResult = process(4, 4, state, uiStrings, catalogContext)
            val passed = categoriesResult is GuidedSequenceResult.Navigate &&
                categoriesResult.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu
            return check(
                id = "RESOL_002",
                description = "Global gestures cannot be shadowed by local vocabulary on the same page",
                passed = passed,
                remediation = "Process global navigation before vocabulary matching in controller."
            )
        }

        fun hiddenPageVocabularyCannotTrigger(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val preferencesIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                uiStrings,
                catalogContext
            )
            val adjustEntry = pages[preferencesIndex].entries.first { it.phrase.contains("Adjust response time") }
            val conversationState = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = 0,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val result = process(adjustEntry.left, adjustEntry.right, conversationState, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Unmatched || result is GuidedSequenceResult.Speak
            return check(
                id = "RESOL_003",
                description = "Hidden-page vocabulary cannot trigger when another page is active",
                passed = passed && result !is GuidedSequenceResult.Navigate,
                remediation = "Match vocabulary only on the active category page."
            )
        }

        fun inactiveCategoryShortcutsCannotTrigger(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val shortcut = GuidedCategoryShortcuts.gestureForCategory(0)
            val vocabularyState = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = 0,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val result = process(shortcut.first, shortcut.second, vocabularyState, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Speak || result is GuidedSequenceResult.SystemAction
            return check(
                id = "RESOL_004",
                description = "Inactive-mode category shortcuts cannot trigger in Vocabulary Mode",
                passed = passed,
                remediation = "Process category shortcuts only in Category Menu Mode."
            )
        }

        fun globalBeforeLocalInVocabulary(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val scrollDown = process(0, 2, state, uiStrings, catalogContext)
            val passed = scrollDown is GuidedSequenceResult.Navigate
            return check(
                id = "RESOL_005",
                description = "Global navigation resolves before local vocabulary actions",
                passed = passed,
                remediation = "Handle global scroll/select/back/categories before phrase matching."
            )
        }

        fun adjustmentValueBeforeScrollConflictAbsent(): ValidationCheckResult {
            val passed = GuidedNavigationGestureAudit.adjustmentDoesNotUseScrollGesturesForValues()
            return check(
                id = "RESOL_006",
                description = "Adjustment mode resolves value and scroll gestures without conflict",
                passed = passed,
                remediation = "Separate L3 R1/L1 R3 value changes from L2 R0/L0 R2 scroll in adjustment."
            )
        }

        fun process(
            left: Int,
            right: Int,
            state: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): GuidedSequenceResult =
            GuidedNavigationController.processSequence(
                left = left,
                right = right,
                state = state,
                language = PreferredLanguage.English,
                uiStrings = uiStrings,
                catalogContext = catalogContext
            )
    }

    private fun check(
        id: String,
        description: String,
        passed: Boolean,
        remediation: String? = null,
        observation: String? = null
    ): ValidationCheckResult = ValidationCheckResult(
        checkId = id,
        description = description,
        passed = passed,
        observation = observation,
        remediation = if (passed) null else remediation
    )
}
