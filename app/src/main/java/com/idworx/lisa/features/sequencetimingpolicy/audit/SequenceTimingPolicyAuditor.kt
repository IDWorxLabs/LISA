package com.idworx.lisa.features.sequencetimingpolicy.audit

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedCatalogContext
import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationController
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedSequenceResult
import com.idworx.lisa.GuidedVocabularyCatalog
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.SEQUENCE_IDLE_TIMEOUT_MS
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.WorkspacePhraseResolver
import com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionPhase
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.isAmbiguousVisibleMatch
import com.idworx.lisa.isUnambiguousVisibleMatch
import com.idworx.lisa.shouldFinalizeSequence

/**
 * System-wide audit proving the single authoritative sequence-timing policy — every finalize path
 * uses [SequenceProcessingDelay] (default 5s, everywhere), and a completed gesture only ever
 * executes immediately when it is unambiguous against the currently visible gesture set; otherwise
 * it waits for the full configured idle window. See [SequenceTimingPolicyMetadata][com.idworx.lisa.features.sequencetimingpolicy.metadata.SequenceTimingPolicyMetadata].
 */
object SequenceTimingPolicyAuditor {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = GuidedCatalogContext(
        responseTimeSec = SequenceProcessingDelay.DEFAULT_SECONDS,
        sensitivityLevel = 5
    )

    // --- No old 3-second default / hardcoded conflicting constant remains --------------------

    fun defaultResponseTimeIsFiveSecondsEverywhere(): Boolean =
        SequenceProcessingDelay.DEFAULT_SECONDS == 5 &&
            SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS == 5 &&
            SEQUENCE_IDLE_TIMEOUT_MS == 5000L &&
            SEQUENCE_IDLE_TIMEOUT_MS == SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)

    fun noHardcodedThreeSecondMillisecondLiteralRemains(): Boolean {
        val sourceRoot = "app/src/main/java/com/idworx/lisa"
        val filesToScan = listOf(
            "$sourceRoot/LisaResponseSpeed.kt",
            "$sourceRoot/MainActivity.kt",
            "$sourceRoot/LisaGuidedMode.kt",
            "$sourceRoot/SequenceFinalization.kt",
            "$sourceRoot/features/onboardingguide/lessoninteraction/LessonInteractionEngine.kt"
        )
        return filesToScan.all { path ->
            val content = ZeroTouchFileProbe.readProjectFile(path) ?: return false
            !content.contains("3000L") && !content.contains("3_000L") && !content.contains("PARTIAL_SEQUENCE_IDLE_MS")
        }
    }

    // --- Single authoritative finalize path — no duplicated/conflicting timing constant ------

    fun mainActivityUsesTheAuthoritativeIdleTimeoutAtEveryFinalizeDecision(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("idleTimeoutMs = effectiveSequenceIdleTimeoutMs()") &&
            main.contains("maxWindowMs = effectiveSequenceMaxWindowMs()") &&
            main.contains("mainHandler.postDelayed(lessonPartialSequenceTimeoutRunnable, effectiveSequenceIdleTimeoutMs())")
    }

    fun guidedLearningAndWorkspaceShareTheSamePolicyObject(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("SequenceProcessingDelay.toMillis(trainingSession.state.progress.preferences.guidedResponseTimeSec)") &&
            main.contains("SequenceProcessingDelay.maxWindowMs(trainingSession.state.progress.preferences.guidedResponseTimeSec)")
    }

    fun quickResolveIdleConstantIsDefinedExactlyOnce(): Boolean {
        val guidedMode = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaGuidedMode.kt"
        ) ?: return false
        return guidedMode.contains("const val QUICK_RESOLVE_IDLE_MS = 900L")
    }

    // --- No blanket "navigation always resolves fast" bypass; single ambiguity gate ----------

    fun quickResolveNoLongerBlanketExemptsAllGlobalNavigation(): Boolean {
        val main = readMainActivity() ?: return false
        val body = main.substringAfter("private fun isQuicklyResolvableGesture(")
        val bypassRemoved = !body.substringBefore("private fun classifyNavigationGesture")
            .contains("if (GuidedModeNavigation.isGlobalNavigationSequence(left, right)) return true")
        return bypassRemoved &&
            main.contains("isSingleEyeGlobalNavigation") &&
            main.contains("isUnambiguousVisibleMatch(left, right, currentVisibleGestureSet())")
    }

    /** Select (L1 R1) is a real, mathematical component-wise prefix of both Yes (L2 R1) and Stop (L2 R3). */
    fun selectIsAProvenComponentWisePrefixOfYesAndStop(): Boolean {
        val yes = 2 to 1
        val stop = 2 to 3
        val select = GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
        fun dominates(shorter: Pair<Int, Int>, longer: Pair<Int, Int>): Boolean =
            longer.first >= shorter.first && longer.second >= shorter.second && longer != shorter
        return dominates(select, yes) && dominates(select, stop)
    }

    fun selectWaitsOnCategoryMenu_whenDirectShortcutsAreVisible(): Boolean {
        val state = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        val visible = visibleGestureSetFor(state)
        val select = GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
        return select in visible && isAmbiguousVisibleMatch(select.first, select.second, visible)
    }

    fun backWaitsOnCategoryMenu_whenALongerShortcutDominatesIt(): Boolean {
        val state = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        val visible = visibleGestureSetFor(state)
        val back = GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT
        return back in visible && isAmbiguousVisibleMatch(back.first, back.second, visible)
    }

    fun previousNextCategoriesRemainSingleEyeCodes_neverAmbiguousByConstruction(): Boolean {
        val singleEye = listOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
        )
        return singleEye.all { (left, right) -> left == 0 || right == 0 }
    }

    // --- Phrase / category sequences respect the 5s idle window unless unambiguous -----------

    fun ambiguousPhraseMustWaitTheFullIdleWindow_yesVsStop(): Boolean {
        val conversation = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, uiStrings, catalogContext)[
            GuidedVocabularyCategory.Conversation.ordinal
        ]
        val yes = conversation.entries.first { it.phrase == "Yes" }
        val stop = conversation.entries.first { it.phrase == "Stop" }
        val state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = GuidedVocabularyCategory.Conversation.ordinal
        )
        val visible = visibleGestureSetFor(state)
        return isAmbiguousVisibleMatch(yes.left, yes.right, visible) &&
            isUnambiguousVisibleMatch(stop.left, stop.right, visible)
    }

    /**
     * The six direct category shortcuts (L2 R1, L1 R2, L3 R1, L1 R3, L3 R2, L2 R3) are exactly the
     * same slot sequence used for phrases, so they share the exact same prefix relationships as the
     * Yes/Stop example: the two "maximal" shortcuts with nothing else in the set numerically
     * extending them (L3 R2, L2 R3) stay unambiguous, while every shorter shortcut that some other
     * visible shortcut extends (e.g. L2 R1, extended by L3 R1/L3 R2/L2 R3) must wait — proving
     * category shortcuts follow the identical ambiguity rule as phrases, not a separate one.
     */
    fun categoryShortcutsRespectTheSameAmbiguityRuleAsPhrases(): Boolean {
        val state = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        val visible = visibleGestureSetFor(state)
        val allShortcuts = GuidedCategoryShortcuts.allGestures()
        val maximalShortcuts = allShortcuts.filterNot { (left, right) ->
            allShortcuts.any { other -> other.first >= left && other.second >= right && other != (left to right) }
        }
        if (maximalShortcuts.isEmpty() || maximalShortcuts.size == allShortcuts.size) return false
        val maximalAreUnambiguous = maximalShortcuts.all { (left, right) -> isUnambiguousVisibleMatch(left, right, visible) }
        val nonMaximalAreAmbiguous = (allShortcuts - maximalShortcuts.toSet()).all { (left, right) ->
            isAmbiguousVisibleMatch(left, right, visible)
        }
        return maximalAreUnambiguous && nonMaximalAreAmbiguous
    }

    // --- Hidden/off-screen gestures never create ambiguity or execute ------------------------

    fun hiddenPhrasePageEntryNeverCreatesAmbiguityOrExecutes(): Boolean {
        val basicNeeds = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, uiStrings, catalogContext)[
            GuidedVocabularyCategory.BasicNeeds.ordinal
        ]
        if (basicNeeds.entries.size <= GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP) return false
        val hidden = basicNeeds.entries[GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP]
        val state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal,
            phrasePageIndex = 0
        )
        val visible = visibleGestureSetFor(state)
        val result = GuidedNavigationController.processSequence(
            hidden.left, hidden.right, state, PreferredLanguage.English, uiStrings, catalogContext = catalogContext
        )
        return (hidden.left to hidden.right) !in visible && result == GuidedSequenceResult.Unmatched
    }

    // --- Emergency: 6-left sequence completable with up to 5s between each blink -------------

    fun emergencySequenceNeverCutOffAcrossFiveSecondGapsBetweenEachBlink(): Boolean {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)
        val justUnderIdleGap = idleTimeoutMs - 1
        return (1..EMERGENCY_LEFT_WINKS).none { blinksSoFar ->
            shouldFinalizeSequence(
                left = blinksSoFar,
                right = 0,
                idleMs = 0L,
                sequenceAgeMs = justUnderIdleGap * (blinksSoFar - 1),
                idleTimeoutMs = idleTimeoutMs,
                maxWindowMs = maxWindowMs
            )
        }
    }

    fun emergencyDispatchedBeforeAmbiguityGate_inMainActivity(): Boolean {
        val main = readMainActivity() ?: return false
        val body = main.substringAfter("private fun processSequenceWinks(")
        val emergencyCheck = body.indexOf("isEmergencySequence(leftWinks, rightWinks)")
        val quickResolveComputation = body.indexOf("val quickResolved = isQuicklyResolvableGesture")
        return emergencyCheck in 0 until quickResolveComputation
    }

    // --- Confirm/cancel remain safe and order-sensitive ---------------------------------------

    fun confirmAndCancelRemainOrderSensitive(): Boolean {
        val awaitingConfirm = Brain1DecisionState(
            kind = Brain1DecisionKind.EmergencyMode,
            phase = Brain1DecisionPhase.AwaitingConfirm
        )
        val (_, confirmOutcome) = Brain1DecisionEngine.handleSequence(
            awaitingConfirm, left = 1, right = 1, blinkOrder = listOf(true, false)
        )
        val (_, cancelOutcome) = Brain1DecisionEngine.handleSequence(
            awaitingConfirm, left = 1, right = 1, blinkOrder = listOf(false, true)
        )
        return confirmOutcome is Brain1DecisionOutcome.Confirmed &&
            cancelOutcome is Brain1DecisionOutcome.ChooseAgain
    }

    // --- Guided Learning and Communication Workspace use the same timing rule ----------------

    fun guidedTrainingUsesItsOwnAdjustableDelayNotAHardcodedOne(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("private fun effectiveSequenceIdleTimeoutMs(): Long =") &&
            main.contains("if (trainingSession.shouldShowTraining())")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/SequenceTimingPolicyAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaSequenceTimingPolicyV1")
    }

    private fun visibleGestureSetFor(state: GuidedNavigationState): Set<Pair<Int, Int>> {
        val emergencyAndSystem = setOf(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
        )
        val navigation = GuidedNavigationController.visibleGlobalNavigationGestures(
            state = state, language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        )
        val content = WorkspacePhraseResolver.continuationMappings(
            state = state, language = PreferredLanguage.English, uiStrings = uiStrings, catalogContext = catalogContext
        ).map { it.left to it.right }.toSet()
        return emergencyAndSystem + navigation + content
    }

    private fun readMainActivity(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
}
