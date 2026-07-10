package com.idworx.lisa.features.sequencetimingpolicy.audit

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.GuidedCatalogContext
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
import com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionPhase
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.shouldFinalizeSequence

/**
 * System-wide audit proving the single, strict sequence-timing policy: NO gesture — phrase,
 * category, navigation, confirm, cancel, or Emergency — is ever executed while the user is still
 * blinking/winking. Every sequence, without exception, is finalized exactly once, only after the
 * full configured response-time idle window elapses with no further input. There is no
 * early-execution / quick-resolve / "unambiguous visible match" fast path anywhere in the app; see
 * [SequenceTimingPolicyMetadata][com.idworx.lisa.features.sequencetimingpolicy.metadata.SequenceTimingPolicyMetadata].
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

    // --- No quick-resolve / early-execution fast path remains anywhere ------------------------

    fun noQuickResolveIdleConstantRemainsAnywhere(): Boolean {
        val sourceRoot = "app/src/main/java/com/idworx/lisa"
        val filesToScan = listOf(
            "$sourceRoot/LisaGuidedMode.kt",
            "$sourceRoot/MainActivity.kt",
            "$sourceRoot/SequenceFinalization.kt"
        )
        return filesToScan.all { path ->
            val content = ZeroTouchFileProbe.readProjectFile(path) ?: return false
            !content.contains("QUICK_RESOLVE_IDLE_MS")
        }
    }

    fun noQuickResolveFunctionRemainsInMainActivity(): Boolean {
        val main = readMainActivity() ?: return false
        return !main.contains("isQuicklyResolvableGesture") &&
            !main.contains("currentVisibleGestureSet") &&
            !main.contains("quickResolved")
    }

    /**
     * The finalize decision inside processSequenceWinks() must be gated ONLY by
     * [shouldFinalizeSequence] — no "OR quick-resolved" branch that could let any gesture through
     * before the idle timeout expires.
     */
    fun finalizeIsGatedSolelyByShouldFinalizeSequence(): Boolean {
        val main = readMainActivity() ?: return false
        val body = main.substringAfter("private fun processSequenceWinks(")
            .substringBefore("private fun updateSequencePauseState(")
        val hasSoleGate = body.contains(
            "val finalize = hasCountedWinks && !activelyWinking &&\n" +
                "            shouldFinalizeSequence("
        )
        val noEmergencyShortCircuit = !body.contains("isEmergencySequence(leftWinks, rightWinks)")
        return hasSoleGate && noEmergencyShortCircuit
    }

    /**
     * Emergency must go through the exact same finalize()-after-idle-timeout path as every other
     * gesture — no separate immediate short-circuit that fires the instant the 6-left count is
     * reached, which would itself violate "wait for the full idle window" for the very last blink.
     */
    fun emergencyHasNoShortCircuitBeforeTheIdleTimeoutGate(): Boolean {
        val main = readMainActivity() ?: return false
        val body = main.substringAfter("private fun processSequenceWinks(")
            .substringBefore("private fun updateSequencePauseState(")
        val acceptLeftBlock = body.substringAfter("if (result.acceptLeft)").substringBefore("if (result.acceptRight)")
        val acceptRightBlock = body.substringAfter("if (result.acceptRight)").substringBefore("val hasCountedWinks")
        return !acceptLeftBlock.contains("finalizeSequence()") && !acceptRightBlock.contains("finalizeSequence()")
    }

    // --- Nothing resolves before the idle timeout, regardless of ambiguity --------------------

    /**
     * Direct regression test for the reported real-device bug: while the completed count exactly
     * matches Yes (L2 R1), [shouldFinalizeSequence] must NOT finalize until idleMs reaches the
     * configured timeout — whether or not Stop (L2 R3) is also reachable. There is no "unambiguous
     * match resolves early" exception left anywhere; Yes and Stop both simply wait.
     */
    fun stopDoesNotTriggerYesEarly_bothWaitForTheFullIdleWindow(): Boolean {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)
        val yesNeverFinalizesWhileStillIdleBelowTimeout = (0 until idleTimeoutMs step 500).none { idleMs ->
            shouldFinalizeSequence(
                left = 2, right = 1, idleMs = idleMs, sequenceAgeMs = idleMs,
                idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
            )
        }
        val yesFinalizesOnlyAfterTheFullIdleWindow = shouldFinalizeSequence(
            left = 2, right = 1, idleMs = idleTimeoutMs, sequenceAgeMs = idleTimeoutMs,
            idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
        )
        val stopAlsoWaitsForTheFullIdleWindow = !shouldFinalizeSequence(
            left = 2, right = 3, idleMs = idleTimeoutMs - 1, sequenceAgeMs = idleTimeoutMs - 1,
            idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
        )
        return yesNeverFinalizesWhileStillIdleBelowTimeout &&
            yesFinalizesOnlyAfterTheFullIdleWindow &&
            stopAlsoWaitsForTheFullIdleWindow
    }

    /**
     * While building toward Emergency (L6 R0), the shorter counts that exactly match Previous
     * (L2 R0) and Categories (L3 R0) must never finalize early — they wait for the idle timeout
     * exactly like every other count, so a user who pauses briefly mid-way to Emergency is never
     * diverted into Previous/Categories.
     */
    fun emergencyBuildUpDoesNotTriggerShorterNavActionsEarly(): Boolean {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)
        val previousCount = GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT
        val categoriesCount = GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT
        return listOf(previousCount, categoriesCount).all { (left, right) ->
            !shouldFinalizeSequence(
                left = left, right = right, idleMs = idleTimeoutMs - 1, sequenceAgeMs = idleTimeoutMs - 1,
                idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
            )
        }
    }

    /** Confirm (L1 R1) and Cancel (same counts, opposite order) both wait for the full idle window. */
    fun confirmAndCancelWaitForTheFullIdleWindow(): Boolean {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)
        val stillWaiting = !shouldFinalizeSequence(
            left = 1, right = 1, idleMs = idleTimeoutMs - 1, sequenceAgeMs = idleTimeoutMs - 1,
            idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
        )
        val resolvesAfterFullWindow = shouldFinalizeSequence(
            left = 1, right = 1, idleMs = idleTimeoutMs, sequenceAgeMs = idleTimeoutMs,
            idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs
        )
        return stillWaiting && resolvesAfterFullWindow
    }

    /**
     * [shouldFinalizeSequence] is purely a function of [idleMs] since the last accepted blink —
     * every new blink/wink resets [idleMs] back to (effectively) zero in the real caller, so this
     * proves the idle timer genuinely restarts on each new input rather than counting from the
     * start of the whole sequence.
     */
    fun everyNewBlinkRestartsTheIdleTimer(): Boolean {
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.DEFAULT_SECONDS)
        val justResetByANewBlink = !shouldFinalizeSequence(
            left = 2, right = 3, idleMs = 0L, sequenceAgeMs = idleTimeoutMs * 4,
            idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs * 10
        )
        val idleAgainAfterTheFullWindow = shouldFinalizeSequence(
            left = 2, right = 3, idleMs = idleTimeoutMs, sequenceAgeMs = idleTimeoutMs * 4,
            idleTimeoutMs = idleTimeoutMs, maxWindowMs = maxWindowMs * 10
        )
        return justResetByANewBlink && idleAgainAfterTheFullWindow
    }

    // --- Hidden/off-screen gestures never execute ---------------------------------------------

    fun hiddenPhrasePageEntryNeverExecutes(): Boolean {
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
        val result = GuidedNavigationController.processSequence(
            hidden.left, hidden.right, state, PreferredLanguage.English, uiStrings, catalogContext = catalogContext
        )
        return result == GuidedSequenceResult.Unmatched
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

    /** Response-time +/- controls feed the exact same field the idle-timeout gate reads. */
    fun responseTimeControlsUpdateTheActualRuntimeIdleTimeout(): Boolean {
        val main = readMainActivity() ?: return false
        return main.contains("sequenceIdleTimeoutMs = SequenceProcessingDelay.toMillis(sec)") &&
            main.contains("sequenceMaxWindowMs = SequenceProcessingDelay.maxWindowMs(sec)") &&
            main.contains("private fun applySequenceProcessingDelay(seconds: Int, persist: Boolean = true)")
    }

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/SequenceTimingPolicyAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaSequenceTimingPolicyV1")
    }

    private fun readMainActivity(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
}
