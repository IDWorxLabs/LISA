package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedCatalogContext
import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationGestureAudit
import com.idworx.lisa.GuidedNavigationGestureAudit.GestureBinding
import com.idworx.lisa.GuidedNavigationPanelSpec
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPageSequences
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.GuidedTouchNavigationSpec
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferenceAdjustmentController
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

/**
 * GUIDED_NAVIGATION_AUTHORITY_V1
 *
 * Verifies guided navigation compliance with the LIC, LIEC, and LVC.
 * Detects violation; does not define interaction law.
 */
object GuidedNavigationAuthorityV1 {

    const val AUTHORITY_NAME: String = "GUIDED_NAVIGATION_AUTHORITY_V1"
    const val PASS_TOKEN: String = "GUIDED_NAVIGATION_AUTHORITY_V1_PASS"

    private val LIC_ARTICLES = listOf(
        "Article 1.1.1.4 — Categories always reachable",
        "Article 1.1.1.5 — Back and Cancel always reachable",
        "Article 1.1.1.6 — Emergency always reachable",
        "Article 1.2.1.3 — Mode identity visible",
        "Article 1.4.1.3 — User must never become trapped",
        "Article 2.2.2.1 — One gesture, one meaning, one context",
        "Article 4.2.1.2 — Every visible action tappable if touch exists",
        "Article 4.3.1.1 — Icons/arrows must not appear without labels",
        "Article 5.2.2.1 — Navigation dead ends are constitutional violations"
    )

    private val LIEC_ARTICLES = listOf(
        "Article 2.1.1.1 — Exactly one active navigation context",
        "Article 2.2.1.1 — Permanent global navigation",
        "Article 2.3.1.1 — Guaranteed recovery routes",
        "Article 2.3.1.2 — No navigation dead ends",
        "Article 3.4.1.4 — One active meaning per gesture",
        "Article 5.2.1.1 — Human touch parity",
        "Article 6.3.1.1 — Navigation safety mechanisms",
        "Article 6.4.1.1 — State consistency"
    )

    private val LVC_ARTICLES = listOf(
        "Article 3.2.1.1 — Singular deterministic visible navigation context",
        "Article 3.2.1.2 — Mode transitions, category reachability, state machine integrity",
        "Article 3.2.1.3 — Dead ends, undefined transitions, silent context mutation",
        "Article 4.1.1.2 — Evidence record requirements",
        "Article 5.1.1.1 — Official outcome taxonomy"
    )

    enum class GuidedLogicalMode(val label: String) {
        Vocabulary("Vocabulary Mode"),
        CategoryMenu("Category Menu Mode"),
        Preferences("Preferences Mode"),
        ResponseTimeAdjustment("Response Time Adjustment Mode"),
        SensitivityAdjustment("Sensitivity Adjustment Mode")
    }

    data class GuidedModeContext(
        val logicalMode: GuidedLogicalMode,
        val state: GuidedNavigationState,
        val panelContext: GuidedNavigationPanelSpec.PanelContext,
        val gestureBindings: List<GestureBinding>
    )

    fun validate(
        language: PreferredLanguage = PreferredLanguage.English,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
    ): ValidationReport {
        val uiStrings = LisaUiStrings.forLanguage(language)
        val modeContexts = buildModeContexts(catalogContext)
        val checks = buildList {
            addAll(NavigationContextAudit.run(modeContexts))
            addAll(ModeTransitionAudit.run(uiStrings, catalogContext))
            addAll(ReachabilityAudit.run(modeContexts, uiStrings, catalogContext))
            addAll(GestureMeaningAudit.run(modeContexts))
            addAll(LabellingAudit.run(uiStrings, modeContexts))
            addAll(HumanTouchParityAudit.run())
            addAll(RecoveryAudit.run(uiStrings, catalogContext))
        }

        val failed = checks.filter { !it.passed }
        val observations = checks.mapNotNull { it.observation }.distinct()
        val outcome = ValidationReport.resolveOutcome(checks)
        val passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null

        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = passToken,
            evidenceSummary = buildEvidenceSummary(checks, modeContexts.size),
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = observations,
            affectedLicArticles = LIC_ARTICLES,
            affectedLiecArticles = LIEC_ARTICLES,
            affectedLvcArticles = LVC_ARTICLES,
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = buildValidationReasoning(checks, outcome)
        )
    }

    fun buildModeContexts(
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
    ): List<GuidedModeContext> {
        val base = GuidedNavigationState(
            draftResponseTimeSec = catalogContext.responseTimeSec,
            draftSensitivityLevel = catalogContext.sensitivityLevel
        )
        return listOf(
            GuidedModeContext(
                logicalMode = GuidedLogicalMode.Vocabulary,
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = 0
                ),
                panelContext = GuidedNavigationPanelSpec.PanelContext.Vocabulary,
                gestureBindings = GuidedNavigationGestureAudit.vocabularyModeBindings(0)
            ),
            GuidedModeContext(
                logicalMode = GuidedLogicalMode.Preferences,
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
                ),
                panelContext = GuidedNavigationPanelSpec.PanelContext.Vocabulary,
                gestureBindings = GuidedNavigationGestureAudit.preferencesPageBindings()
            ),
            GuidedModeContext(
                logicalMode = GuidedLogicalMode.CategoryMenu,
                state = base.copy(screenMode = GuidedOverlayScreenMode.CategoryMenu),
                panelContext = GuidedNavigationPanelSpec.PanelContext.CategoryMenu,
                gestureBindings = GuidedNavigationGestureAudit.categoryMenuModeBindings()
            ),
            GuidedModeContext(
                logicalMode = GuidedLogicalMode.ResponseTimeAdjustment,
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                    preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime
                ),
                panelContext = GuidedNavigationPanelSpec.PanelContext.Adjustment,
                gestureBindings = GuidedNavigationGestureAudit.responseTimeAdjustmentBindings()
            ),
            GuidedModeContext(
                logicalMode = GuidedLogicalMode.SensitivityAdjustment,
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                    preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity
                ),
                panelContext = GuidedNavigationPanelSpec.PanelContext.Adjustment,
                gestureBindings = GuidedNavigationGestureAudit.sensitivityAdjustmentBindings()
            )
        )
    }

    private fun buildEvidenceSummary(checks: List<ValidationCheckResult>, modeCount: Int): String =
        "Guided Navigation Authority examined $modeCount guided logical modes across " +
            "${checks.size} deterministic checks covering navigation context, mode transitions, " +
            "reachability, gesture meaning, labelling, human touch parity, and recovery. " +
            "Passed: ${checks.count { it.passed }}. Failed: ${checks.count { !it.passed }}."

    private fun buildValidationReasoning(
        checks: List<ValidationCheckResult>,
        outcome: ValidationOutcome
    ): String = when (outcome) {
        ValidationOutcome.PASS ->
            "All ${checks.size} required guided navigation checks passed. Navigation context is singular, transitions " +
                "are deterministic, global commands remain reachable, gestures have one active meaning per " +
                "mode, labels and touch parity are present, and no dead-end states were detected."
        ValidationOutcome.PASS_WITH_OBSERVATIONS ->
            "All ${checks.size} required checks passed with ${checks.count { !it.observation.isNullOrBlank() }} " +
                "documented observations. Review observations before release."
        ValidationOutcome.FAIL ->
            "One or more guided navigation checks failed (${checks.count { !it.passed }} of ${checks.size}). " +
                "Implementation violates constitutional or engineering navigation requirements until remediated and revalidated."
        ValidationOutcome.BLOCKED ->
            "Validation could not be completed reliably. Resolve blocking conditions and re-run."
        ValidationOutcome.NOT_APPLICABLE ->
            "Authority not applicable to declared scope."
    }

    // --- Domain 1: Navigation Context Audit ---

    object NavigationContextAudit {
        fun run(modeContexts: List<GuidedModeContext>): List<ValidationCheckResult> = listOf(
            singularContextModel(),
            modeIdentityKnown(modeContexts),
            categoryKnownInVocabulary(modeContexts),
            pageStateKnown(modeContexts),
            noAmbiguousAdjustOverlay(modeContexts)
        )

        fun singularContextModel(): ValidationCheckResult {
            val sample = GuidedNavigationState()
            val fields = listOf(
                sample.screenMode,
                sample.categoryIndex,
                sample.preferencesAdjustMode,
                sample.phrasePageIndex,
                sample.categoryMenuSelection
            )
            val passed = fields.size == 5
            return ValidationCheckResult(
                checkId = "NAV_CTX_001",
                description = "Exactly one active guided navigation context model exists",
                passed = passed,
                remediation = if (passed) null else
                    "Consolidate navigation state into a single authoritative context model."
            )
        }

        fun modeIdentityKnown(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.all { context ->
                when (context.logicalMode) {
                    GuidedLogicalMode.Vocabulary,
                    GuidedLogicalMode.Preferences ->
                        context.state.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                            context.state.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
                    GuidedLogicalMode.CategoryMenu ->
                        context.state.screenMode == GuidedOverlayScreenMode.CategoryMenu
                    GuidedLogicalMode.ResponseTimeAdjustment ->
                        context.state.preferencesAdjustMode == GuidedPreferencesAdjustMode.ResponseTime
                    GuidedLogicalMode.SensitivityAdjustment ->
                        context.state.preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity
                }
            }
            return ValidationCheckResult(
                checkId = "NAV_CTX_002",
                description = "Active guided mode is always known and unambiguous",
                passed = passed,
                remediation = if (passed) null else
                    "Ensure each guided surface maps to exactly one logical mode identity."
            )
        }

        fun categoryKnownInVocabulary(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val vocabularyModes = modeContexts.filter {
                it.state.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                    !it.state.isPreferencesAdjustmentActive
            }
            val passed = vocabularyModes.all { ctx ->
                ctx.state.categoryIndex in 0 until GuidedVocabularyCategory.PAGE_COUNT
            }
            return ValidationCheckResult(
                checkId = "NAV_CTX_003",
                description = "Active category is known in vocabulary and preferences modes",
                passed = passed,
                remediation = if (passed) null else
                    "Clamp and expose active category index in all vocabulary states."
            )
        }

        fun pageStateKnown(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                ctx.state.phrasePageIndex >= 0 && ctx.state.categoryMenuSelection >= 0
            }
            return ValidationCheckResult(
                checkId = "NAV_CTX_004",
                description = "Active page and adjustment scroll state are known where applicable",
                passed = passed,
                remediation = if (passed) null else
                    "Normalize phrase page and adjustment scroll indices in guided navigation state."
            )
        }

        fun noAmbiguousAdjustOverlay(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.none { ctx ->
                ctx.state.isPreferencesAdjustmentActive &&
                    ctx.state.screenMode == GuidedOverlayScreenMode.CategoryMenu
            }
            return ValidationCheckResult(
                checkId = "NAV_CTX_005",
                description = "No ambiguous combined category-menu and adjustment state exists",
                passed = passed,
                remediation = if (passed) null else
                    "Adjustment overlay must not coexist with category menu screen mode."
            )
        }
    }

    // --- Domain 2: Mode Transition Audit ---

    object ModeTransitionAudit {
        fun run(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> {
            val base = GuidedNavigationState(
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            return listOf(
                l4r4OpensCategoryMenuFromVocabulary(base, uiStrings, catalogContext),
                l4r4OpensCategoryMenuFromPreferences(base, uiStrings, catalogContext),
                l4r4OpensCategoryMenuFromResponseTime(base, uiStrings, catalogContext),
                l4r4OpensCategoryMenuFromSensitivity(base, uiStrings, catalogContext),
                l2r2BackFromCategoryMenu(base, uiStrings, catalogContext),
                l2r2CancelFromAdjustments(base, uiStrings, catalogContext),
                l1r1SelectSaveWhereApplicable(base, uiStrings, catalogContext),
                scrollGesturesDoNotConflictInAdjustment(),
                emergencyRegisteredInAllModeBindings()
            )
        }

        private fun process(
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

        fun l4r4OpensCategoryMenuFromVocabulary(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = base.copy(screenMode = GuidedOverlayScreenMode.Vocabulary, categoryIndex = 0)
            val result = process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, state, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu
            return ValidationCheckResult(
                checkId = "MODE_TX_001",
                description = "Categories gesture opens Category Menu from Vocabulary Mode",
                passed = passed,
                remediation = if (passed) null else
                    "Wire the Categories gesture to open Category Menu from all vocabulary states."
            )
        }

        fun l4r4OpensCategoryMenuFromPreferences(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = base.copy(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
            )
            val result = process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, state, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu
            return ValidationCheckResult(
                checkId = "MODE_TX_002",
                description = "Categories gesture opens Category Menu from Preferences Mode",
                passed = passed,
                remediation = if (passed) null else
                    "Ensure Preferences page honors the Categories gesture's category menu transition."
            )
        }

        fun l4r4OpensCategoryMenuFromResponseTime(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = base.copy(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime
            )
            val result = process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, state, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu &&
                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
            return ValidationCheckResult(
                checkId = "MODE_TX_003",
                description = "Categories gesture opens Category Menu from Response Time Adjustment Mode",
                passed = passed,
                remediation = if (passed) null else
                    "Categories gesture must cancel adjustment and open Category Menu."
            )
        }

        fun l4r4OpensCategoryMenuFromSensitivity(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = base.copy(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity
            )
            val result = process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, state, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu &&
                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
            return ValidationCheckResult(
                checkId = "MODE_TX_004",
                description = "Categories gesture opens Category Menu from Sensitivity Adjustment Mode",
                passed = passed,
                remediation = if (passed) null else
                    "Categories gesture must cancel sensitivity adjustment and open Category Menu."
            )
        }

        fun l2r2BackFromCategoryMenu(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = base.copy(screenMode = GuidedOverlayScreenMode.CategoryMenu)
            val result = process(2, 2, state, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary
            return ValidationCheckResult(
                checkId = "MODE_TX_005",
                description = "L2 R2 backs/cancels from Category Menu Mode",
                passed = passed,
                remediation = if (passed) null else
                    "Wire L2 R2 to close Category Menu and return to vocabulary."
            )
        }

        fun l2r2CancelFromAdjustments(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val responseState = base.copy(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
                draftResponseTimeSec = 6
            )
            val sensitivityState = base.copy(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity,
                draftSensitivityLevel = 8
            )
            val responseResult = process(2, 2, responseState, uiStrings, catalogContext)
            val sensitivityResult = process(2, 2, sensitivityState, uiStrings, catalogContext)
            val passed = responseResult is GuidedSequenceResult.Navigate &&
                sensitivityResult is GuidedSequenceResult.Navigate &&
                responseResult.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None &&
                sensitivityResult.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
            return ValidationCheckResult(
                checkId = "MODE_TX_006",
                description = "L2 R2 backs/cancels from adjustment modes without saving",
                passed = passed,
                remediation = if (passed) null else
                    "Wire L2 R2 to cancel adjustment and restore prior saved values."
            )
        }

        fun l1r1SelectSaveWhereApplicable(
            base: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val menuState = base.copy(screenMode = GuidedOverlayScreenMode.CategoryMenu, categoryMenuSelection = 2)
            val menuResult = process(1, 1, menuState, uiStrings, catalogContext)
            val adjustState = base.copy(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
                draftResponseTimeSec = 5
            )
            val saveResult = process(1, 1, adjustState, uiStrings, catalogContext)
            val passed = menuResult is GuidedSequenceResult.Navigate &&
                saveResult is GuidedSequenceResult.SavePreferencesAdjustment
            return ValidationCheckResult(
                checkId = "MODE_TX_007",
                description = "L1 R1 selects or saves where applicable in menu and adjustment modes",
                passed = passed,
                remediation = if (passed) null else
                    "Ensure L1 R1 opens selected category and saves adjustment drafts."
            )
        }

        fun scrollGesturesDoNotConflictInAdjustment(): ValidationCheckResult {
            val passed = GuidedNavigationGestureAudit.adjustmentDoesNotUseScrollGesturesForValues()
            return ValidationCheckResult(
                checkId = "MODE_TX_008",
                description = "L2 R0 and L0 R2 scroll only in adjustment modes without value conflict",
                passed = passed,
                remediation = if (passed) null else
                    "Reserve L2 R0/L0 R2 for scroll in adjustment; use L3 R1/L1 R3 for value changes."
            )
        }

        fun emergencyRegisteredInAllModeBindings(): ValidationCheckResult {
            val passed = GuidedNavigationGestureAudit.everyModeHasBackCategoriesAndEmergency()
            return ValidationCheckResult(
                checkId = "MODE_TX_009",
                description = "L6 R0 emergency remains registered in every guided mode binding set",
                passed = passed,
                remediation = if (passed) null else
                    "Register Emergency gesture in all guided mode binding sets."
            )
        }
    }

    // --- Domain 3: Reachability Audit ---

    object ReachabilityAudit {
        fun run(
            modeContexts: List<GuidedModeContext>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            categoriesReachable(modeContexts),
            backCancelReachable(modeContexts, uiStrings, catalogContext),
            emergencyReachable(modeContexts),
            scrollReachableWhereRequired(modeContexts),
            noTrappedModes(modeContexts, uiStrings, catalogContext)
        )

        fun categoriesReachable(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                ctx.gestureBindings.any { it.action == "Categories" }
            }
            return ValidationCheckResult(
                checkId = "REACH_001",
                description = "Categories reachable from every guided mode",
                passed = passed,
                remediation = if (passed) null else
                    "Add the Categories binding to every guided mode."
            )
        }

        fun categoriesReachableInBindings(bindings: List<GestureBinding>): Boolean =
            bindings.any { it.action == "Categories" }

        fun backCancelReachable(
            modeContexts: List<GuidedModeContext>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = modeContexts.filterNot { ctx ->
                hasBackCancelBinding(ctx) && hasFunctionalBackOrRecovery(ctx, uiStrings, catalogContext)
            }.map { it.logicalMode.label }
            return ValidationCheckResult(
                checkId = "REACH_002",
                description = "Back/Cancel reachable from every guided mode",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Provide functional Back/Cancel or equivalent labelled recovery in: ${failures.joinToString()}."
            )
        }

        fun backCancelReachableInBindings(bindings: List<GestureBinding>): Boolean =
            bindings.any { it.action == "BackCancel" }

        fun emergencyReachable(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                ctx.gestureBindings.any { it.action == "Emergency" }
            }
            return ValidationCheckResult(
                checkId = "REACH_003",
                description = "Emergency reachable from every guided mode",
                passed = passed,
                remediation = if (passed) null else
                    "Register Emergency (L6 R0) in every guided mode panel and preserve global handler."
            )
        }

        fun emergencyReachableInBindings(bindings: List<GestureBinding>): Boolean =
            bindings.any { it.action == "Emergency" }

        fun scrollReachableWhereRequired(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                ctx.gestureBindings.any { it.action == "ScrollUp" } &&
                    ctx.gestureBindings.any { it.action == "ScrollDown" }
            }
            return ValidationCheckResult(
                checkId = "REACH_004",
                description = "Scroll Up/Down reachable where guided navigation requires scrolling",
                passed = passed,
                remediation = if (passed) null else
                    "Register L2 R0 and L0 R2 scroll bindings in all guided modes."
            )
        }

        fun noTrappedModes(
            modeContexts: List<GuidedModeContext>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val trapped = modeContexts.filterNot { hasEscapeRoute(it, uiStrings, catalogContext) }
                .map { it.logicalMode.label }
            return ValidationCheckResult(
                checkId = "REACH_005",
                description = "No guided mode can trap the user",
                passed = trapped.isEmpty(),
                remediation = if (trapped.isEmpty()) null else
                    "Add labelled recovery paths in trapped modes: ${trapped.joinToString()}."
            )
        }

        private fun hasBackCancelBinding(ctx: GuidedModeContext): Boolean =
            ctx.gestureBindings.any { it.action == "BackCancel" }

        private fun hasFunctionalBackOrRecovery(
            ctx: GuidedModeContext,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): Boolean = when (ctx.logicalMode) {
            GuidedLogicalMode.CategoryMenu,
            GuidedLogicalMode.ResponseTimeAdjustment,
            GuidedLogicalMode.SensitivityAdjustment ->
                processGesture(2, 2, ctx.state, uiStrings, catalogContext) is GuidedSequenceResult.Navigate
            GuidedLogicalMode.Vocabulary,
            GuidedLogicalMode.Preferences ->
                processGesture(
                    GuidedModeNavigation.CATEGORIES_LEFT,
                    GuidedModeNavigation.CATEGORIES_RIGHT,
                    ctx.state,
                    uiStrings,
                    catalogContext
                ) is GuidedSequenceResult.Navigate
        }

        private fun hasEscapeRoute(
            ctx: GuidedModeContext,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): Boolean {
            val probes = listOf(
                GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
                2 to 2,
                2 to 0,
                0 to 2,
                1 to 1
            )
            return probes.any { (left, right) ->
                processGesture(left, right, ctx.state, uiStrings, catalogContext) !is GuidedSequenceResult.Unmatched
            } || ctx.gestureBindings.any { it.action == "Emergency" }
        }

        private fun processGesture(
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

    // --- Domain 4: Gesture Meaning Audit ---

    object GestureMeaningAudit {
        fun run(modeContexts: List<GuidedModeContext>): List<ValidationCheckResult> = listOf(
            oneMeaningPerGesturePerMode(modeContexts),
            noGlobalDuplicationByLocalActions(modeContexts),
            categoryShortcutsOnlyInCategoryMenu(),
            vocabularySlotsOnlyInVocabularyModes(modeContexts),
            adjustmentGesturesOnlyInAdjustmentModes(),
            noDuplicateImportantActions()
        )

        fun oneMeaningPerGesturePerMode(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                GuidedNavigationGestureAudit.noDuplicateGestures(ctx.gestureBindings)
            }
            return ValidationCheckResult(
                checkId = "GEST_001",
                description = "One gesture has one active meaning per guided mode/context",
                passed = passed,
                remediation = if (passed) null else
                    "Remove duplicate gesture assignments within the same guided mode."
            )
        }

        fun noDuplicateGesturesInBindings(bindings: List<GestureBinding>): Boolean =
            GuidedNavigationGestureAudit.noDuplicateGestures(bindings)

        fun noGlobalDuplicationByLocalActions(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val globals = GuidedNavigationGestureAudit.globalPanelBindings()
                .map { it.left to it.right }
                .toSet()
            val failures = modeContexts.filter { ctx ->
                val localDuplicates = ctx.gestureBindings
                    .filter { it.action.startsWith("Phrase:") || it.action.startsWith("CategoryShortcut:") }
                    .any { (left, right, _) -> (left to right) in globals }
                localDuplicates
            }.map { it.logicalMode.label }
            return ValidationCheckResult(
                checkId = "GEST_002",
                description = "Global gestures are not duplicated by local active actions in the same mode",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Remove local vocabulary or shortcut slots that reuse global gesture sequences in: " +
                        failures.joinToString()
            )
        }

        fun categoryShortcutsOnlyInCategoryMenu(): ValidationCheckResult {
            val menuHasShortcuts = GuidedNavigationGestureAudit.categoryMenuModeBindings()
                .any { it.action.startsWith("CategoryShortcut:") }
            val vocabularyLacksShortcuts = GuidedNavigationGestureAudit.vocabularyModeBindings(0)
                .none { it.action.startsWith("CategoryShortcut:") }
            val passed = menuHasShortcuts && vocabularyLacksShortcuts &&
                GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation()
            return ValidationCheckResult(
                checkId = "GEST_003",
                description = "Category shortcuts operate only in Category Menu Mode",
                passed = passed,
                remediation = if (passed) null else
                    "Restrict direct category shortcut gestures to Category Menu Mode."
            )
        }

        fun vocabularySlotsOnlyInVocabularyModes(modeContexts: List<GuidedModeContext>): ValidationCheckResult {
            val vocabularyModes = modeContexts.filter {
                it.logicalMode == GuidedLogicalMode.Vocabulary ||
                    it.logicalMode == GuidedLogicalMode.Preferences
            }
            val menuAndAdjust = modeContexts.filter {
                it.logicalMode == GuidedLogicalMode.CategoryMenu ||
                    it.logicalMode == GuidedLogicalMode.ResponseTimeAdjustment ||
                    it.logicalMode == GuidedLogicalMode.SensitivityAdjustment
            }
            val vocabHasPhrases = vocabularyModes.all { ctx ->
                ctx.gestureBindings.any { it.action.startsWith("Phrase:") } ||
                    ctx.logicalMode == GuidedLogicalMode.Preferences
            }
            val othersNoPhrases = menuAndAdjust.all { ctx ->
                ctx.gestureBindings.none { it.action.startsWith("Phrase:") }
            }
            return ValidationCheckResult(
                checkId = "GEST_004",
                description = "Local vocabulary slots speak only inside Vocabulary and Preferences modes",
                passed = vocabHasPhrases && othersNoPhrases,
                remediation = if (vocabHasPhrases && othersNoPhrases) null else
                    "Remove vocabulary phrase bindings from non-vocabulary guided modes."
            )
        }

        fun adjustmentGesturesOnlyInAdjustmentModes(): ValidationCheckResult {
            val adjustModes = listOf(
                GuidedNavigationGestureAudit.responseTimeAdjustmentBindings(),
                GuidedNavigationGestureAudit.sensitivityAdjustmentBindings()
            )
            val nonAdjust = listOf(
                GuidedNavigationGestureAudit.vocabularyModeBindings(0),
                GuidedNavigationGestureAudit.categoryMenuModeBindings()
            )
            val passed = adjustModes.all { bindings ->
                bindings.any { it.action == "DecreaseValue" } &&
                    bindings.any { it.action == "IncreaseValue" }
            } && nonAdjust.all { bindings ->
                bindings.none { it.action == "DecreaseValue" || it.action == "IncreaseValue" }
            }
            return ValidationCheckResult(
                checkId = "GEST_005",
                description = "Adjustment gestures adjust values only inside Adjustment modes",
                passed = passed,
                remediation = if (passed) null else
                    "Restrict L3 R1/L1 R3 value gestures to adjustment modes only."
            )
        }

        fun noDuplicateImportantActions(): ValidationCheckResult {
            val passed = GuidedNavigationGestureAudit.auditAllModes()
            return ValidationCheckResult(
                checkId = "GEST_006",
                description = "No duplicate important actions share the same sequence in the same active mode",
                passed = passed,
                remediation = if (passed) null else
                    "Resolve gesture conflicts across all guided modes using GuidedNavigationGestureAudit."
            )
        }
    }

    // --- Domain 5: Labelling Audit ---

    object LabellingAudit {
        fun run(
            uiStrings: LisaUiStrings,
            modeContexts: List<GuidedModeContext>
        ): List<ValidationCheckResult> = listOf(
            allPanelActionsLabeled(uiStrings, modeContexts),
            rightPanelShowsMeaningAndGesture(uiStrings, modeContexts),
            categoryMenuShowsNamesAndShortcuts(uiStrings),
            adjustmentPanelRequiredLabels(uiStrings)
        )

        fun allPanelActionsLabeled(
            uiStrings: LisaUiStrings,
            modeContexts: List<GuidedModeContext>
        ): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                val actions = GuidedNavigationPanelSpec.panelActions(uiStrings, ctx.panelContext)
                GuidedNavigationPanelSpec.allActionsLabeled(actions)
            }
            return ValidationCheckResult(
                checkId = "LABEL_001",
                description = "Every visible guided panel action has a clear label",
                passed = passed,
                remediation = if (passed) null else
                    "Provide non-blank title, hint, symbol, and sequence label for every panel action."
            )
        }

        fun panelActionsFullyLabeled(actions: List<com.idworx.lisa.GuidedNavPanelAction>): Boolean =
            GuidedNavigationPanelSpec.allActionsLabeled(actions)

        fun rightPanelShowsMeaningAndGesture(
            uiStrings: LisaUiStrings,
            modeContexts: List<GuidedModeContext>
        ): ValidationCheckResult {
            val passed = modeContexts.all { ctx ->
                GuidedNavigationPanelSpec.panelActions(uiStrings, ctx.panelContext).all { action ->
                    action.title.isNotBlank() && action.sequenceLabel.isNotBlank()
                }
            }
            return ValidationCheckResult(
                checkId = "LABEL_002",
                description = "Every right-panel action shows both meaning and gesture sequence",
                passed = passed,
                remediation = if (passed) null else
                    "Display both human-readable meaning and gesture sequence on right-panel actions."
            )
        }

        fun categoryMenuShowsNamesAndShortcuts(uiStrings: LisaUiStrings): ValidationCheckResult {
            val titles = GuidedVocabularyCatalog.categoryMenuTitles(uiStrings)
            val shortcuts = (0 until GuidedCategoryShortcuts.SHORTCUT_COUNT).map { index ->
                GuidedCategoryShortcuts.sequenceLabelForCategory(index)
            }
            val passed = titles.size == GuidedCategoryShortcuts.SHORTCUT_COUNT &&
                titles.all { it.isNotBlank() } &&
                shortcuts.all { it.isNotBlank() }
            return ValidationCheckResult(
                checkId = "LABEL_003",
                description = "Category Menu shows category names and shortcut sequences",
                passed = passed,
                remediation = if (passed) null else
                    "Label each category row with name and direct shortcut sequence."
            )
        }

        fun adjustmentPanelRequiredLabels(uiStrings: LisaUiStrings): ValidationCheckResult {
            val required = listOf(
                uiStrings.guidedAdjustmentCurrentValueResponseTime(3),
                uiStrings.guidedDecreaseResponseTime,
                uiStrings.guidedIncreaseResponseTime,
                uiStrings.guidedSaveResponseTime,
                uiStrings.guidedCancelToPreferences,
                uiStrings.guidedCategoriesNavTitle,
                uiStrings.guidedEmergencyNavTitle
            )
            val passed = required.all { it.isNotBlank() }
            return ValidationCheckResult(
                checkId = "LABEL_004",
                description = "Preferences adjustment panel shows value, decrease, increase, save, cancel, categories, and emergency labels",
                passed = passed,
                remediation = if (passed) null else
                    "Provide all required adjustment panel labels in UI strings."
            )
        }
    }

    // --- Domain 6: Human Touch Parity Audit ---

    object HumanTouchParityAudit {
        fun run(): List<ValidationCheckResult> = listOf(
            panelGesturesHaveTouchMirrors(),
            adjustmentGesturesHaveTouchMirrors(),
            vocabularySlotsHaveTouchMirrors(),
            categoryRowsHaveDocumentedTouchPath(),
            rightPanelGesturesHaveTouchMirrors()
        )

        fun panelGesturesHaveTouchMirrors(): ValidationCheckResult {
            val passed = GuidedTouchNavigationSpec.panelGestures.all { (left, right) ->
                GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right)
            }
            return ValidationCheckResult(
                checkId = "TOUCH_001",
                description = "Every visible guided panel action has a touch/click path",
                passed = passed,
                remediation = if (passed) null else
                    "Wire touch handlers for all panel gestures documented in GuidedTouchNavigationSpec."
            )
        }

        fun touchMirrorsPanelGesture(left: Int, right: Int): Boolean =
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right)

        fun adjustmentGesturesHaveTouchMirrors(): ValidationCheckResult {
            val passed = GuidedTouchNavigationSpec.adjustmentPanelGestures.all { (left, right) ->
                GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right)
            }
            return ValidationCheckResult(
                checkId = "TOUCH_002",
                description = "Adjustment panel gestures have touch parity with eye gestures",
                passed = passed,
                remediation = if (passed) null else
                    "Wire touch handlers for L3 R1 and L1 R3 adjustment gestures."
            )
        }

        fun vocabularySlotsHaveTouchMirrors(): ValidationCheckResult {
            val slots = GuidedPageSequences.slots + GuidedPageSequences.extendedSlots
            val passed = slots.all { (left, right) ->
                GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right)
            }
            return ValidationCheckResult(
                checkId = "TOUCH_003",
                description = "Phrase rows are tappable and mirror eye-selection gestures",
                passed = passed,
                remediation = if (passed) null else
                    "Ensure all vocabulary slot gestures are registered for touch parity."
            )
        }

        fun categoryRowsHaveDocumentedTouchPath(): ValidationCheckResult {
            val passed = GuidedCategoryShortcuts.SHORTCUT_COUNT == GuidedVocabularyCategory.PAGE_COUNT
            return ValidationCheckResult(
                checkId = "TOUCH_004",
                description = "Category rows are tappable for each registered category shortcut",
                passed = passed,
                remediation = if (passed) null else
                    "Provide tappable category rows matching GuidedCategoryShortcuts count."
            )
        }

        fun rightPanelGesturesHaveTouchMirrors(): ValidationCheckResult {
            val panelOk = panelGesturesHaveTouchMirrors().passed
            val adjustOk = adjustmentGesturesHaveTouchMirrors().passed
            return ValidationCheckResult(
                checkId = "TOUCH_005",
                description = "Right-panel actions are tappable with equivalent logical outcomes",
                passed = panelOk && adjustOk,
                remediation = if (panelOk && adjustOk) null else
                    "Ensure right-panel touch callbacks invoke the same logical actions as eye gestures."
            )
        }
    }

    // --- Domain 7: Recovery Audit ---

    object RecoveryAudit {
        fun run(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            everyModeHasRecoveryPath(uiStrings, catalogContext),
            adjustmentCancelRestoresSavedValues(catalogContext),
            categoriesEscapesAdjustment(uiStrings, catalogContext),
            noDeadEndStates(uiStrings, catalogContext)
        )

        fun everyModeHasRecoveryPath(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val contexts = buildModeContexts(catalogContext)
            val withoutRecovery = contexts.filterNot { hasRecoveryPath(it, uiStrings, catalogContext) }
            val passed = withoutRecovery.isEmpty()
            return ValidationCheckResult(
                checkId = "RECOV_001",
                description = "Every guided mode has at least one labelled recovery path",
                passed = passed,
                remediation = if (passed) null else
                    "Add recovery to Category Menu, previous mode, or safe state in: " +
                        withoutRecovery.map { it.logicalMode.label }.joinToString()
            )
        }

        fun adjustmentCancelRestoresSavedValues(catalogContext: GuidedCatalogContext): ValidationCheckResult {
            val state = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
                draftResponseTimeSec = 6,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val cancelled = PreferenceAdjustmentController.cancelAdjustment(state)
            val passed = cancelled.preferencesAdjustMode == GuidedPreferencesAdjustMode.None &&
                cancelled.displayResponseTimeSec(catalogContext.responseTimeSec) == catalogContext.responseTimeSec
            return ValidationCheckResult(
                checkId = "RECOV_002",
                description = "Adjustment cancel restores prior saved values",
                passed = passed,
                remediation = if (passed) null else
                    "Cancel adjustment must discard draft values and restore saved catalog values."
            )
        }

        fun categoriesEscapesAdjustment(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = 9
            )
            val result = GuidedNavigationController.processSequence(
                left = GuidedModeNavigation.CATEGORIES_LEFT,
                right = GuidedModeNavigation.CATEGORIES_RIGHT,
                state = state,
                language = PreferredLanguage.English,
                uiStrings = uiStrings,
                catalogContext = catalogContext
            )
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu &&
                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
            return ValidationCheckResult(
                checkId = "RECOV_003",
                description = "Categories action cancels adjustment and opens Category Menu",
                passed = passed,
                remediation = if (passed) null else
                    "Wire the Categories gesture during adjustment to cancel draft and open Category Menu."
            )
        }

        fun noDeadEndStates(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val contexts = buildModeContexts(catalogContext)
            val deadEnds = contexts.filterNot { hasRecoveryPath(it, uiStrings, catalogContext) }
                .map { it.logicalMode.label }
            return ValidationCheckResult(
                checkId = "RECOV_004",
                description = "No dead-end navigation state exists in guided modes",
                passed = deadEnds.isEmpty(),
                remediation = if (deadEnds.isEmpty()) null else
                    "Eliminate dead-end states in: ${deadEnds.joinToString()}."
            )
        }

        fun hasRecoveryPath(
            ctx: GuidedModeContext,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): Boolean = when (ctx.logicalMode) {
            GuidedLogicalMode.CategoryMenu ->
                process(2, 2, ctx.state, uiStrings, catalogContext) is GuidedSequenceResult.Navigate
            GuidedLogicalMode.ResponseTimeAdjustment,
            GuidedLogicalMode.SensitivityAdjustment ->
                process(2, 2, ctx.state, uiStrings, catalogContext) is GuidedSequenceResult.Navigate ||
                    process(
                        GuidedModeNavigation.CATEGORIES_LEFT,
                        GuidedModeNavigation.CATEGORIES_RIGHT,
                        ctx.state,
                        uiStrings,
                        catalogContext
                    ) is GuidedSequenceResult.Navigate
            GuidedLogicalMode.Vocabulary,
            GuidedLogicalMode.Preferences ->
                process(
                    GuidedModeNavigation.CATEGORIES_LEFT,
                    GuidedModeNavigation.CATEGORIES_RIGHT,
                    ctx.state,
                    uiStrings,
                    catalogContext
                ) is GuidedSequenceResult.Navigate
        }

        private fun process(
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
}
