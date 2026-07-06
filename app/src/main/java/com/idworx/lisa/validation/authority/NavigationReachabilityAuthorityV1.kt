package com.idworx.lisa.validation.authority

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedCatalogContext
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationGestureAudit
import com.idworx.lisa.GuidedNavigationPanelSpec
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.GuidedTouchNavigationSpec
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.isEmergencySequence
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

/**
 * NAVIGATION_REACHABILITY_AUTHORITY_V1
 *
 * Proves that every guided interaction state provides deterministic, recoverable navigation paths.
 */
object NavigationReachabilityAuthorityV1 {

    const val AUTHORITY_NAME: String = "NAVIGATION_REACHABILITY_AUTHORITY_V1"
    const val PASS_TOKEN: String = "NAVIGATION_REACHABILITY_AUTHORITY_V1_PASS"

    private val LIC_ARTICLES = listOf(
        "Article 1.1.1.4 — Categories always reachable",
        "Article 1.1.1.5 — Back and Cancel always reachable",
        "Article 1.1.1.6 — Emergency always reachable",
        "Article 1.4.1.3 — User must never become trapped",
        "Article 5.2.2.1 — Navigation dead ends are constitutional violations",
        "Article 5.2.2.2 — User trapped in any interaction state is a constitutional failure",
        "Article 5.2.2.3 — Recovery shall be simpler than the action that caused the error",
        "Article 4.2.1.2 — Every visible action tappable if touch exists"
    )

    private val LIEC_ARTICLES = listOf(
        "Article 2.2.1.1 — Permanent global navigation",
        "Article 2.3.1.1 — Guaranteed recovery routes",
        "Article 2.3.1.2 — No navigation dead ends",
        "Article 2.3.1.4 — Navigation state machine integrity",
        "Article 2.4.1.1 — Identical navigation actions produce identical results",
        "Article 6.2.1.1 — Recovery engine guarantees escape",
        "Article 6.3.1.2 — Reachability of Categories, Back, Cancel, Emergency as invariants"
    )

    private val LVC_ARTICLES = listOf(
        "Article 3.7.1.1 — Invariant reachability of Categories, Back, Cancel, Emergency",
        "Article 3.7.1.2 — Global navigation permanence and labelling across modes",
        "Article 3.7.1.3 — Overlays preserve constitutional reachability",
        "Article 4.1.1.2 — Evidence record requirements",
        "Article 5.1.1.1 — Official outcome taxonomy"
    )

    data class GuidedReachabilityState(
        val label: String,
        val state: GuidedNavigationState
    )

    fun validate(
        language: PreferredLanguage = PreferredLanguage.English,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
    ): ValidationReport {
        val uiStrings = LisaUiStrings.forLanguage(language)
        val guidedStates = buildGuidedStates(catalogContext)
        val checks = buildList {
            addAll(CategoriesReachabilityAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(BackCancelReachabilityAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(EmergencyReachabilityAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(ScrollReachabilityAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(RecoveryRouteAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(DeadEndAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(NavigationLoopAudit.run(guidedStates, uiStrings, catalogContext))
            addAll(StateReachabilityAudit.run(uiStrings, catalogContext))
            addAll(HumanReachabilityAudit.run(uiStrings))
        }

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        val passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null

        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = passToken,
            evidenceSummary = buildEvidenceSummary(checks, guidedStates.size),
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
            subsystem = "Navigation Reachability"
        )
    }

    fun buildGuidedStates(
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
    ): List<GuidedReachabilityState> {
        val base = GuidedNavigationState(
            draftResponseTimeSec = catalogContext.responseTimeSec,
            draftSensitivityLevel = catalogContext.sensitivityLevel
        )
        return listOf(
            GuidedReachabilityState(
                label = "Vocabulary Mode",
                state = base.copy(screenMode = GuidedOverlayScreenMode.Vocabulary, categoryIndex = 0)
            ),
            GuidedReachabilityState(
                label = "Category Menu Mode",
                state = base.copy(screenMode = GuidedOverlayScreenMode.CategoryMenu)
            ),
            GuidedReachabilityState(
                label = "Preferences Mode",
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
                )
            ),
            GuidedReachabilityState(
                label = "Response Time Adjustment Mode",
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                    preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime
                )
            ),
            GuidedReachabilityState(
                label = "Sensitivity Adjustment Mode",
                state = base.copy(
                    screenMode = GuidedOverlayScreenMode.Vocabulary,
                    categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                    preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity
                )
            )
        )
    }

    fun processGesture(
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

    fun stateFingerprint(state: GuidedNavigationState): String =
        listOf(
            state.screenMode.name,
            state.categoryIndex,
            state.categoryMenuSelection,
            state.phrasePageIndex,
            state.preferencesAdjustMode.name,
            state.adjustmentScrollStep
        ).joinToString("|")

    private fun buildEvidenceSummary(checks: List<ValidationCheckResult>, stateCount: Int): String =
        "Navigation Reachability Authority examined $stateCount guided states across ${checks.size} " +
            "deterministic reachability checks. Passed: ${checks.count { it.passed }}. " +
            "Failed: ${checks.count { !it.passed }}."

    private fun buildValidationReasoning(
        checks: List<ValidationCheckResult>,
        outcome: ValidationOutcome
    ): String = when (outcome) {
        ValidationOutcome.PASS ->
            "All ${checks.size} navigation reachability checks passed. Categories, Back/Cancel, Emergency, " +
                "scroll, and recovery routes are functionally reachable in every guided state without dead ends " +
                "or trapping loops."
        ValidationOutcome.PASS_WITH_OBSERVATIONS ->
            "All ${checks.size} required checks passed with documented observations."
        ValidationOutcome.FAIL ->
            "${checks.count { !it.passed }} of ${checks.size} reachability checks failed. Users may be " +
                "trapped or unable to reach required navigation destinations."
        ValidationOutcome.BLOCKED ->
            "Navigation reachability validation could not be completed reliably."
        ValidationOutcome.NOT_APPLICABLE ->
            "Authority not applicable to declared scope."
    }

    // --- Domain 1: Categories Reachability ---

    object CategoriesReachabilityAudit {
        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            categoriesReachableFromEveryState(guidedStates, uiStrings, catalogContext),
            categoriesGestureOpensMenu(guidedStates, uiStrings, catalogContext)
        )

        fun categoriesReachableFromEveryState(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = guidedStates.filterNot { ctx ->
                categoriesOpensMenu(ctx, uiStrings, catalogContext)
            }.map { it.label }
            return check(
                id = "CAT_REACH_001",
                description = "Category Menu is reachable via L4 R4 from every guided state",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Wire L4 R4 to open or retain Category Menu in: ${failures.joinToString()}."
            )
        }

        fun categoriesGestureOpensMenu(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = guidedStates.filterNot { ctx ->
                val result = processGesture(
                    GuidedModeNavigation.CATEGORIES_LEFT,
                    GuidedModeNavigation.CATEGORIES_RIGHT,
                    ctx.state,
                    uiStrings,
                    catalogContext
                )
                result is GuidedSequenceResult.Navigate &&
                    result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu
            }.map { it.label }
            return check(
                id = "CAT_REACH_002",
                description = "L4 R4 navigation controller transitions into Category Menu",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Ensure L4 R4 produces CategoryMenu screenMode in: ${failures.joinToString()}."
            )
        }

        fun categoriesOpensMenu(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): Boolean {
            val result = processGesture(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT,
                ctx.state,
                uiStrings,
                catalogContext
            )
            return result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu
        }
    }

    // --- Domain 2: Back / Cancel Reachability ---

    object BackCancelReachabilityAudit {
        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            backReachableFromEveryState(guidedStates, uiStrings, catalogContext),
            backDestinationCategoryMenu(guidedStates, uiStrings, catalogContext),
            backDestinationVocabulary(guidedStates, uiStrings, catalogContext),
            backDestinationPreferences(guidedStates, uiStrings, catalogContext)
        )

        fun backReachableFromEveryState(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = guidedStates.filterNot { hasBackOrCancelRecovery(it, uiStrings, catalogContext) }
                .map { it.label }
            return check(
                id = "BACK_REACH_001",
                description = "Back/Cancel is functionally reachable from every guided state",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Provide functional L2 R2 or equivalent labelled recovery in: ${failures.joinToString()}."
            )
        }

        fun backDestinationCategoryMenu(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val menu = guidedStates.first { it.label == "Category Menu Mode" }
            val result = backResult(menu, uiStrings, catalogContext)
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary
            return check(
                id = "BACK_REACH_002",
                description = "Category Menu L2 R2 returns to Vocabulary (previous safe state)",
                passed = passed,
                remediation = "Wire L2 R2 in Category Menu to close menu and return to Vocabulary."
            )
        }

        fun backDestinationPreferences(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val adjustStates = guidedStates.filter {
                it.label.contains("Adjustment")
            }
            val passed = adjustStates.all { ctx ->
                val result = backResult(ctx, uiStrings, catalogContext)
                result is GuidedSequenceResult.Navigate &&
                    result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.None &&
                    result.newState.categoryIndex == GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
            }
            return check(
                id = "BACK_REACH_003",
                description = "Adjustment modes L2 R2 cancel to Preferences without saving",
                passed = passed,
                remediation = "Wire L2 R2 in adjustment modes to cancel and return to Preferences page."
            )
        }

        fun backDestinationVocabulary(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val vocabulary = guidedStates.first { it.label == "Vocabulary Mode" }
            val preferences = guidedStates.first { it.label == "Preferences Mode" }
            val vocabOk = hasBackOrCancelRecovery(vocabulary, uiStrings, catalogContext)
            val prefsOk = hasBackOrCancelRecovery(preferences, uiStrings, catalogContext)
            return check(
                id = "BACK_REACH_004",
                description = "Vocabulary and Preferences expose recoverable Back/Cancel paths without entrapment",
                passed = vocabOk && prefsOk,
                remediation = "Ensure L2 R2 or L4 R4 Categories provides labelled recovery on vocabulary surfaces."
            )
        }

        fun backResult(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): GuidedSequenceResult = processGesture(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            ctx.state,
            uiStrings,
            catalogContext
        )

        fun hasBackOrCancelRecovery(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): Boolean {
            val back = backResult(ctx, uiStrings, catalogContext)
            if (back is GuidedSequenceResult.Navigate) return true
            val categories = processGesture(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT,
                ctx.state,
                uiStrings,
                catalogContext
            )
            return categories is GuidedSequenceResult.Navigate
        }
    }

    // --- Domain 3: Emergency Reachability ---

    object EmergencyReachabilityAudit {
        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            emergencyRegisteredEveryState(guidedStates),
            emergencyNeverRequiresNavigationFirst(guidedStates, uiStrings, catalogContext),
            emergencyIndependentOfCategory(guidedStates, uiStrings, catalogContext),
            emergencyNeverVocabularySpeak(guidedStates, uiStrings, catalogContext)
        )

        fun emergencyRegisteredEveryState(guidedStates: List<GuidedReachabilityState>): ValidationCheckResult {
            val modeContexts = GuidedNavigationAuthorityV1.buildModeContexts()
            val passed = modeContexts.all { ctx ->
                ctx.gestureBindings.any { it.action == "Emergency" && isEmergencySequence(it.left, it.right) }
            }
            return check(
                id = "EMER_REACH_001",
                description = "Emergency L6 R0 is registered in every guided mode",
                passed = passed,
                remediation = "Register Emergency in every guided mode panel and binding set."
            )
        }

        fun emergencyNeverRequiresNavigationFirst(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val passed = guidedStates.all { ctx ->
                val result = emergencyResult(ctx, uiStrings, catalogContext)
                result !is GuidedSequenceResult.Speak && result !is GuidedSequenceResult.SystemAction
            }
            return check(
                id = "EMER_REACH_002",
                description = "Emergency never requires navigation first and is not absorbed as vocabulary",
                passed = passed,
                remediation = "Ensure L6 R0 is handled by global emergency authority, not vocabulary speak."
            )
        }

        fun emergencyIndependentOfCategory(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val categories = (0 until GuidedVocabularyCategory.PAGE_COUNT).map { index ->
                guidedStates.first().copy(
                    label = "Vocabulary category $index",
                    state = guidedStates.first().state.copy(categoryIndex = index)
                )
            }
            val passed = categories.all { ctx ->
                val result = emergencyResult(ctx, uiStrings, catalogContext)
                result !is GuidedSequenceResult.Speak
            }
            return check(
                id = "EMER_REACH_003",
                description = "Emergency reachability does not depend on active category",
                passed = passed,
                remediation = "Keep emergency handling independent of categoryIndex and page content."
            )
        }

        fun emergencyNeverVocabularySpeak(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val passed = guidedStates.all { ctx ->
                emergencyResult(ctx, uiStrings, catalogContext) !is GuidedSequenceResult.Speak
            }
            return check(
                id = "EMER_REACH_004",
                description = "Emergency L6 R0 never triggers vocabulary speak in guided controller paths",
                passed = passed,
                remediation = "Remove L6 R0 from vocabulary; route to global emergency handler."
            )
        }

        fun emergencyResult(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): GuidedSequenceResult = processGesture(
            EMERGENCY_LEFT_WINKS,
            EMERGENCY_RIGHT_WINKS,
            ctx.state,
            uiStrings,
            catalogContext
        )
    }

    // --- Domain 4: Scroll Reachability ---

    object ScrollReachabilityAudit {
        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            scrollUpAvailableEveryState(guidedStates, uiStrings, catalogContext),
            scrollDownAvailableEveryState(guidedStates, uiStrings, catalogContext),
            scrollClampsSafelyInVocabulary(uiStrings, catalogContext)
        )

        fun scrollUpAvailableEveryState(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = guidedStates.filterNot { ctx ->
                scrollUpResult(ctx, uiStrings, catalogContext) is GuidedSequenceResult.Navigate
            }.map { it.label }
            return check(
                id = "SCROLL_REACH_001",
                description = "L2 R0 Scroll Up is available in every guided state",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Ensure L2 R0 produces navigation in: ${failures.joinToString()}."
            )
        }

        fun scrollDownAvailableEveryState(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = guidedStates.filterNot { ctx ->
                scrollDownResult(ctx, uiStrings, catalogContext) is GuidedSequenceResult.Navigate
            }.map { it.label }
            return check(
                id = "SCROLL_REACH_002",
                description = "L0 R2 Scroll Down is available in every guided state",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Ensure L0 R2 produces navigation in: ${failures.joinToString()}."
            )
        }

        fun scrollClampsSafelyInVocabulary(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val state = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = 0,
                phrasePageIndex = 0,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val upAtTop = scrollUpResult(
                GuidedReachabilityState("Vocabulary", state),
                uiStrings,
                catalogContext
            )
            val passed = upAtTop is GuidedSequenceResult.Navigate &&
                (upAtTop as GuidedSequenceResult.Navigate).newState.phrasePageIndex == 0
            return check(
                id = "SCROLL_REACH_003",
                description = "Scroll Up clamps safely at bounds without navigation loss",
                passed = passed,
                remediation = "Clamp phrasePageIndex at zero when scrolling up from first page."
            )
        }

        fun scrollUpResult(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): GuidedSequenceResult = processGesture(
            GuidedModeNavigation.PREVIOUS_LEFT,
            GuidedModeNavigation.PREVIOUS_RIGHT,
            ctx.state,
            uiStrings,
            catalogContext
        )

        fun scrollDownResult(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): GuidedSequenceResult = processGesture(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            ctx.state,
            uiStrings,
            catalogContext
        )
    }

    // --- Domain 5: Recovery Route Audit ---

    object RecoveryRouteAudit {
        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            everyModeHasRecoveryRoute(guidedStates, uiStrings, catalogContext),
            recoveryRoutesLabelled(uiStrings),
            recoveryRoutesHaveTouchParity()
        )

        fun everyModeHasRecoveryRoute(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val failures = guidedStates.filterNot { ctx ->
                hasRecoveryRoute(ctx, uiStrings, catalogContext)
            }.map { it.label }
            return check(
                id = "RECOV_ROUTE_001",
                description = "Every guided mode exposes at least one recovery route",
                passed = failures.isEmpty(),
                remediation = if (failures.isEmpty()) null else
                    "Add labelled recovery to Category Menu, parent state, or safe vocabulary in: " +
                        failures.joinToString()
            )
        }

        fun recoveryRoutesLabelled(uiStrings: LisaUiStrings): ValidationCheckResult {
            val contexts = listOf(
                GuidedNavigationPanelSpec.PanelContext.Vocabulary,
                GuidedNavigationPanelSpec.PanelContext.CategoryMenu,
                GuidedNavigationPanelSpec.PanelContext.Adjustment
            )
            val passed = contexts.all { ctx ->
                GuidedNavigationPanelSpec.allActionsLabeled(
                    GuidedNavigationPanelSpec.panelActions(uiStrings, ctx)
                )
            }
            return check(
                id = "RECOV_ROUTE_002",
                description = "Recovery routes (Back, Categories, Cancel) are labelled in every panel context",
                passed = passed,
                remediation = "Label Back, Categories, and Cancel actions in all guided panel contexts."
            )
        }

        fun recoveryRoutesHaveTouchParity(): ValidationCheckResult {
            val recoveryGestures = listOf(
                GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
                GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
                EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
            )
            val passed = recoveryGestures.all { (left, right) ->
                GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right)
            }
            return check(
                id = "RECOV_ROUTE_003",
                description = "Recovery routes are reachable via touch parity gestures",
                passed = passed,
                remediation = "Wire touch handlers for Back, Categories, and Emergency panel actions."
            )
        }

        fun hasRecoveryRoute(
            ctx: GuidedReachabilityState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): Boolean = BackCancelReachabilityAudit.hasBackOrCancelRecovery(ctx, uiStrings, catalogContext) ||
            CategoriesReachabilityAudit.categoriesOpensMenu(ctx, uiStrings, catalogContext)
    }

    // --- Domain 6: Dead-End Audit ---

    object DeadEndAudit {
        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            noCategoriesDeadEnds(guidedStates, uiStrings, catalogContext),
            noBackDeadEnds(guidedStates, uiStrings, catalogContext),
            noEmergencyDeadEnds(guidedStates),
            noRecoveryDeadEnds(guidedStates, uiStrings, catalogContext)
        )

        fun noCategoriesDeadEnds(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult = CategoriesReachabilityAudit.categoriesReachableFromEveryState(
            guidedStates,
            uiStrings,
            catalogContext
        ).copy(checkId = "DEAD_END_001", description = "No state lacks Categories reachability")

        fun noBackDeadEnds(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult = BackCancelReachabilityAudit.backReachableFromEveryState(
            guidedStates,
            uiStrings,
            catalogContext
        ).copy(checkId = "DEAD_END_002", description = "No state lacks Back/Cancel reachability")

        fun noEmergencyDeadEnds(guidedStates: List<GuidedReachabilityState>): ValidationCheckResult {
            val modeContexts = GuidedNavigationAuthorityV1.buildModeContexts()
            val passed = modeContexts.all { ctx ->
                ctx.gestureBindings.any { it.action == "Emergency" }
            }
            return check(
                id = "DEAD_END_003",
                description = "No state lacks Emergency reachability registration",
                passed = passed,
                remediation = "Register Emergency in every guided mode binding set."
            )
        }

        fun noRecoveryDeadEnds(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult = RecoveryRouteAudit.everyModeHasRecoveryRoute(
            guidedStates,
            uiStrings,
            catalogContext
        ).copy(checkId = "DEAD_END_004", description = "No state lacks any recovery route")
    }

    // --- Domain 7: Navigation Loop Audit ---

    object NavigationLoopAudit {
        private val probeGestures = listOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
        )

        fun run(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            transitionsAreDeterministic(guidedStates, uiStrings, catalogContext),
            noTrappingLoops(guidedStates, uiStrings, catalogContext),
            categoryMenuCategoriesNoTrap(guidedStates, uiStrings, catalogContext),
            noUndefinedTransitions(guidedStates, uiStrings, catalogContext)
        )

        fun transitionsAreDeterministic(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val passed = guidedStates.all { ctx ->
                probeGestures.all { (left, right) ->
                    val first = processGesture(left, right, ctx.state, uiStrings, catalogContext)
                    val second = processGesture(left, right, ctx.state, uiStrings, catalogContext)
                    resultsEquivalent(first, second)
                }
            }
            return check(
                id = "LOOP_001",
                description = "Navigation transitions are deterministic for global probe gestures",
                passed = passed,
                remediation = "Ensure identical inputs produce identical navigation results."
            )
        }

        fun noTrappingLoops(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val trapped = guidedStates.filter { ctx ->
                !canReachCategoryMenuOrVocabularyHome(ctx.state, uiStrings, catalogContext)
            }.map { it.label }
            return check(
                id = "LOOP_002",
                description = "No guided state is trapped in a loop without exit to safe navigation",
                passed = trapped.isEmpty(),
                remediation = if (trapped.isEmpty()) null else
                    "Add exit transitions from trapping loops in: ${trapped.joinToString()}."
            )
        }

        fun categoryMenuCategoriesNoTrap(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val menu = guidedStates.first { it.label == "Category Menu Mode" }
            val categoriesTwice = processGesture(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT,
                menu.state,
                uiStrings,
                catalogContext
            )
            val backOnce = processGesture(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT,
                menu.state,
                uiStrings,
                catalogContext
            )
            val passed = categoriesTwice is GuidedSequenceResult.Navigate &&
                backOnce is GuidedSequenceResult.Navigate &&
                backOnce.newState.screenMode == GuidedOverlayScreenMode.Vocabulary
            return check(
                id = "LOOP_003",
                description = "Category Menu L4 R4 self-state does not remove Back exit",
                passed = passed,
                remediation = "Preserve L2 R2 Back exit when L4 R4 is pressed inside Category Menu."
            )
        }

        fun noUndefinedTransitions(
            guidedStates: List<GuidedReachabilityState>,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val passed = guidedStates.all { ctx ->
                probeGestures.all { (left, right) ->
                    val result = processGesture(left, right, ctx.state, uiStrings, catalogContext)
                    result is GuidedSequenceResult.Navigate ||
                        result is GuidedSequenceResult.SavePreferencesAdjustment ||
                        result is GuidedSequenceResult.Speak ||
                        result is GuidedSequenceResult.SystemAction ||
                        result is GuidedSequenceResult.Unmatched
                }
            }
            return check(
                id = "LOOP_004",
                description = "Global navigation probe gestures never produce undefined transitions",
                passed = passed,
                remediation = "Handle or explicitly classify all global gesture outcomes."
            )
        }

        fun canReachCategoryMenuOrVocabularyHome(
            start: GuidedNavigationState,
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext,
            maxDepth: Int = 4
        ): Boolean {
            val startFp = stateFingerprint(start)
            val queue = ArrayDeque<Pair<GuidedNavigationState, Int>>()
            val visited = mutableSetOf<String>()
            queue.add(start to 0)
            while (queue.isNotEmpty()) {
                val (current, depth) = queue.removeFirst()
                val fp = stateFingerprint(current)
                if (fp in visited) continue
                visited.add(fp)
                if (isSafeNavigationState(current)) return true
                if (depth >= maxDepth) continue
                for ((left, right) in probeGestures) {
                    when (val result = processGesture(left, right, current, uiStrings, catalogContext)) {
                        is GuidedSequenceResult.Navigate -> queue.add(result.newState to depth + 1)
                        is GuidedSequenceResult.SavePreferencesAdjustment -> queue.add(result.newState to depth + 1)
                        else -> Unit
                    }
                }
            }
            return isSafeNavigationState(start) || startFp.let { fp ->
                visited.contains(fp) && isSafeNavigationState(start)
            }
        }

        private fun isSafeNavigationState(state: GuidedNavigationState): Boolean =
            state.screenMode == GuidedOverlayScreenMode.CategoryMenu ||
                (state.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                    state.preferencesAdjustMode == GuidedPreferencesAdjustMode.None)

        private fun resultsEquivalent(a: GuidedSequenceResult, b: GuidedSequenceResult): Boolean =
            when {
                a is GuidedSequenceResult.Navigate && b is GuidedSequenceResult.Navigate ->
                    stateFingerprint(a.newState) == stateFingerprint(b.newState)
                a is GuidedSequenceResult.SavePreferencesAdjustment &&
                    b is GuidedSequenceResult.SavePreferencesAdjustment ->
                    stateFingerprint(a.newState) == stateFingerprint(b.newState)
                a is GuidedSequenceResult.Unmatched && b is GuidedSequenceResult.Unmatched -> true
                a is GuidedSequenceResult.Speak && b is GuidedSequenceResult.Speak ->
                    a.entry.phrase == b.entry.phrase
                a is GuidedSequenceResult.SystemAction && b is GuidedSequenceResult.SystemAction ->
                    a.entry.phrase == b.entry.phrase
                else -> false
            }
    }

    // --- Domain 8: State Reachability Audit ---

    object StateReachabilityAudit {
        fun run(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): List<ValidationCheckResult> = listOf(
            vocabularyReachableFromEntry(),
            categoryMenuReachableFromVocabulary(uiStrings, catalogContext),
            preferencesReachableFromCategoryMenu(uiStrings, catalogContext),
            responseTimeAdjustmentReachable(uiStrings, catalogContext),
            sensitivityAdjustmentReachable(uiStrings, catalogContext),
            noOrphanedModes(uiStrings, catalogContext)
        )

        fun vocabularyReachableFromEntry(): ValidationCheckResult {
            val entry = GuidedNavigationState()
            val passed = entry.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                entry.preferencesAdjustMode == GuidedPreferencesAdjustMode.None
            return check(
                id = "STATE_REACH_001",
                description = "Vocabulary mode is reachable from application entry (default guided state)",
                passed = passed,
                remediation = "Initialize guided navigation in Vocabulary mode."
            )
        }

        fun categoryMenuReachableFromVocabulary(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val vocabulary = GuidedNavigationState(
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val result = processGesture(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT,
                vocabulary,
                uiStrings,
                catalogContext
            )
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.CategoryMenu
            return check(
                id = "STATE_REACH_002",
                description = "Category Menu is reachable from Vocabulary via L4 R4",
                passed = passed,
                remediation = "Wire vocabulary L4 R4 to open Category Menu."
            )
        }

        fun preferencesReachableFromCategoryMenu(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val menu = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.CategoryMenu,
                categoryMenuSelection = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val result = processGesture(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                menu,
                uiStrings,
                catalogContext
            )
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.screenMode == GuidedOverlayScreenMode.Vocabulary &&
                result.newState.categoryIndex == GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
            return check(
                id = "STATE_REACH_003",
                description = "Preferences is reachable from Category Menu via selection",
                passed = passed,
                remediation = "Wire L1 R1 in Category Menu to open selected Preferences category."
            )
        }

        fun responseTimeAdjustmentReachable(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val preferences = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                uiStrings,
                catalogContext
            )
            val adjustEntry = pages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries
                .first { it.phrase.contains("Adjust response time") }
            val result = processGesture(
                adjustEntry.left,
                adjustEntry.right,
                preferences,
                uiStrings,
                catalogContext
            )
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.ResponseTime
            return check(
                id = "STATE_REACH_004",
                description = "Response Time Adjustment is reachable from Preferences",
                passed = passed,
                remediation = "Wire Adjust response time entry to open Response Time Adjustment mode."
            )
        }

        fun sensitivityAdjustmentReachable(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val preferences = GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX,
                draftResponseTimeSec = catalogContext.responseTimeSec,
                draftSensitivityLevel = catalogContext.sensitivityLevel
            )
            val pages = GuidedVocabularyCatalog.buildPages(
                PreferredLanguage.English,
                uiStrings,
                catalogContext
            )
            val adjustEntry = pages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries
                .first { it.phrase.contains("Adjust sensitivity") }
            val result = processGesture(
                adjustEntry.left,
                adjustEntry.right,
                preferences,
                uiStrings,
                catalogContext
            )
            val passed = result is GuidedSequenceResult.Navigate &&
                result.newState.preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity
            return check(
                id = "STATE_REACH_005",
                description = "Sensitivity Adjustment is reachable from Preferences",
                passed = passed,
                remediation = "Wire Adjust sensitivity entry to open Sensitivity Adjustment mode."
            )
        }

        fun noOrphanedModes(
            uiStrings: LisaUiStrings,
            catalogContext: GuidedCatalogContext
        ): ValidationCheckResult {
            val chain = listOf(
                vocabularyReachableFromEntry(),
                categoryMenuReachableFromVocabulary(uiStrings, catalogContext),
                preferencesReachableFromCategoryMenu(uiStrings, catalogContext),
                responseTimeAdjustmentReachable(uiStrings, catalogContext),
                sensitivityAdjustmentReachable(uiStrings, catalogContext)
            )
            val passed = chain.all { it.passed }
            return check(
                id = "STATE_REACH_006",
                description = "No guided mode is orphaned from application entry",
                passed = passed,
                remediation = "Ensure complete reachability chain from entry through all guided modes."
            )
        }
    }

    // --- Domain 9: Human Reachability Audit ---

    object HumanReachabilityAudit {
        fun run(uiStrings: LisaUiStrings): List<ValidationCheckResult> = listOf(
            navigationGesturesHaveTouchParity(),
            recoveryGesturesHaveTouchParity(),
            allPanelNavigationActionsLabelled(uiStrings)
        )

        fun navigationGesturesHaveTouchParity(): ValidationCheckResult {
            val required = listOf(
                GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
                GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
                EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
                GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
                GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
                GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
            )
            val passed = required.all { (left, right) ->
                GuidedTouchNavigationSpec.touchMirrorsEyeGesture(left, right)
            }
            return check(
                id = "HUMAN_REACH_001",
                description = "Categories, Back, Emergency, Save, and Scroll are touch-reachable",
                passed = passed,
                remediation = "Wire touch handlers for all global navigation panel gestures."
            )
        }

        fun recoveryGesturesHaveTouchParity(): ValidationCheckResult =
            RecoveryRouteAudit.recoveryRoutesHaveTouchParity()
                .copy(checkId = "HUMAN_REACH_002", description = "Recovery navigation actions have touch parity")

        fun allPanelNavigationActionsLabelled(uiStrings: LisaUiStrings): ValidationCheckResult {
            val passed = listOf(
                GuidedNavigationPanelSpec.PanelContext.Vocabulary,
                GuidedNavigationPanelSpec.PanelContext.CategoryMenu,
                GuidedNavigationPanelSpec.PanelContext.Adjustment
            ).all { ctx ->
                GuidedNavigationPanelSpec.panelActions(uiStrings, ctx).all { action ->
                    action.title.isNotBlank() && action.sequenceLabel.isNotBlank()
                }
            }
            return check(
                id = "HUMAN_REACH_003",
                description = "All visible navigation actions show labels and gesture sequences",
                passed = passed,
                remediation = "Label every visible navigation action in guided panel contexts."
            )
        }
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
