package com.idworx.lisa.features.sequencetimingpolicy.validation

import com.idworx.lisa.features.sequencetimingpolicy.audit.SequenceTimingPolicyAuditor
import com.idworx.lisa.features.sequencetimingpolicy.metadata.SequenceTimingPolicyMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

/**
 * SEQUENCE_TIMING_POLICY_V1
 *
 * System-wide audit proving the strict, single sequence-timing policy: NO gesture — phrase,
 * category, navigation, confirm, cancel, or Emergency — ever executes while the user is still
 * blinking/winking. Every sequence is finalized exactly once, only after the full configured
 * response-time idle window (default 5s) elapses with no further input, identically in Guided
 * Training and the normal Communication Workspace. There is no early-execution / quick-resolve /
 * "unambiguous visible match can execute early" fast path left anywhere in the app.
 */
object SequenceTimingPolicyAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_SEQUENCE_TIMING_POLICY_V1"
    const val PASS_TOKEN: String = SequenceTimingPolicyMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "STP_001",
                "Default response time is 5 seconds everywhere (workspace and Guided Training)",
                SequenceTimingPolicyAuditor.defaultResponseTimeIsFiveSecondsEverywhere(),
                "Keep SequenceProcessingDelay.DEFAULT_SECONDS, GUIDED_DEFAULT_SECONDS, and SEQUENCE_IDLE_TIMEOUT_MS all equal to 5s in LisaResponseSpeed.kt."
            ),
            check(
                "STP_002",
                "No hardcoded 3000ms/3_000ms sequence-timing literal or separate PARTIAL_SEQUENCE_IDLE_MS constant remains",
                SequenceTimingPolicyAuditor.noHardcodedThreeSecondMillisecondLiteralRemains(),
                "Remove any hardcoded 3000L/3_000L sequence-idle literal and route it through SequenceProcessingDelay/effectiveSequenceIdleTimeoutMs() instead."
            ),
            check(
                "STP_003",
                "Every finalize decision in MainActivity reads the authoritative idle timeout/max window (never a separate constant)",
                SequenceTimingPolicyAuditor.mainActivityUsesTheAuthoritativeIdleTimeoutAtEveryFinalizeDecision(),
                "Route processSequenceWinks(), updateSequencePauseState(), and syncLessonPartialSequenceTimeout() all through effectiveSequenceIdleTimeoutMs()/effectiveSequenceMaxWindowMs()."
            ),
            check(
                "STP_004",
                "Guided Training and Communication Workspace both resolve their idle timeout through SequenceProcessingDelay",
                SequenceTimingPolicyAuditor.guidedLearningAndWorkspaceShareTheSamePolicyObject(),
                "Keep effectiveSequenceIdleTimeoutMs()/effectiveSequenceMaxWindowMs() switching only which seconds value feeds SequenceProcessingDelay.toMillis()/maxWindowMs(), never a second parallel implementation."
            ),
            check(
                "STP_005",
                "QUICK_RESOLVE_IDLE_MS no longer exists anywhere in the codebase",
                SequenceTimingPolicyAuditor.noQuickResolveIdleConstantRemainsAnywhere(),
                "Delete the QUICK_RESOLVE_IDLE_MS constant entirely — there is no early-resolve grace window anymore, only the full idle timeout."
            ),
            check(
                "STP_006",
                "isQuicklyResolvableGesture()/currentVisibleGestureSet()/quickResolved no longer exist in MainActivity",
                SequenceTimingPolicyAuditor.noQuickResolveFunctionRemainsInMainActivity(),
                "Remove isQuicklyResolvableGesture(), currentVisibleGestureSet(), and the quickResolved local from processSequenceWinks()."
            ),
            check(
                "STP_007",
                "The finalize decision in processSequenceWinks() is gated solely by shouldFinalizeSequence() — no OR quick-resolve branch, no immediate Emergency short-circuit",
                SequenceTimingPolicyAuditor.finalizeIsGatedSolelyByShouldFinalizeSequence(),
                "processSequenceWinks() must compute `finalize` as exactly hasCountedWinks && !activelyWinking && shouldFinalizeSequence(...), with no other path to finalizeSequence()."
            ),
            check(
                "STP_008",
                "Emergency has no short-circuit before the idle-timeout gate — it finalizes through the same path as every other gesture",
                SequenceTimingPolicyAuditor.emergencyHasNoShortCircuitBeforeTheIdleTimeoutGate(),
                "Do not call finalizeSequence() directly from inside the acceptLeft/acceptRight wink-accept blocks; let the shared finalize gate at the bottom of processSequenceWinks() decide, exactly like every other sequence."
            ),
            check(
                "STP_009",
                "Stop (L2 R3) does not trigger Yes (L2 R1) early — both simply wait for the full idle window",
                SequenceTimingPolicyAuditor.stopDoesNotTriggerYesEarly_bothWaitForTheFullIdleWindow(),
                "shouldFinalizeSequence() must return false for any count while idleMs is below idleTimeoutMs, regardless of which visible phrase the count matches."
            ),
            check(
                "STP_010",
                "Emergency (L6 R0) build-up does not trigger shorter partial nav actions (Previous L2 R0, Categories L3 R0) early",
                SequenceTimingPolicyAuditor.emergencyBuildUpDoesNotTriggerShorterNavActionsEarly(),
                "Keep shouldFinalizeSequence() as the only finalize gate — it never special-cases navigation counts for early resolution."
            ),
            check(
                "STP_011",
                "Confirm (L1 R1) and Cancel (same counts, opposite order) both wait for the full idle window before resolving",
                SequenceTimingPolicyAuditor.confirmAndCancelWaitForTheFullIdleWindow(),
                "Confirm/Cancel reach finalizeSequence() only through the shared idle-timeout gate, exactly like phrases and categories."
            ),
            check(
                "STP_012",
                "Every new blink/wink restarts the idle timer",
                SequenceTimingPolicyAuditor.everyNewBlinkRestartsTheIdleTimer(),
                "Keep onWinkCounted() updating lastWinkTimeMs on every accepted blink so idleMs in processSequenceWinks() is always measured from the most recent wink, not the start of the sequence."
            ),
            check(
                "STP_013",
                "Hidden/off-screen phrase-page entries never execute",
                SequenceTimingPolicyAuditor.hiddenPhrasePageEntryNeverExecutes(),
                "Keep GuidedNavigationController.processSequence returning Unmatched for entries on other pages/categories than the one currently visible."
            ),
            check(
                "STP_014",
                "Emergency's 6-left sequence is never cut off across up to 5s gaps between each blink",
                SequenceTimingPolicyAuditor.emergencySequenceNeverCutOffAcrossFiveSecondGapsBetweenEachBlink(),
                "Keep shouldFinalizeSequence() only finalizing on left==0&&right==0 short-circuit, idle timeout, or absolute max-window safety cap — never a partial-progress cutoff."
            ),
            check(
                "STP_015",
                "Confirm (Left-then-Right) and Cancel (Right-then-Left) remain order-sensitive and distinct",
                SequenceTimingPolicyAuditor.confirmAndCancelRemainOrderSensitive(),
                "Keep UniversalInteractionGestures.isConfirm/isCancel keyed off BlinkSequenceOrder, never raw left/right counts alone."
            ),
            check(
                "STP_016",
                "Guided Training uses its own adjustable delay sourced from the same authoritative policy, never a hardcoded one",
                SequenceTimingPolicyAuditor.guidedTrainingUsesItsOwnAdjustableDelayNotAHardcodedOne(),
                "Keep effectiveSequenceIdleTimeoutMs() branching on trainingSession.shouldShowTraining()."
            ),
            check(
                "STP_017",
                "Response-time +/- controls update the actual runtime idle timeout used by the finalize gate",
                SequenceTimingPolicyAuditor.responseTimeControlsUpdateTheActualRuntimeIdleTimeout(),
                "Keep applySequenceProcessingDelay() writing sequenceIdleTimeoutMs/sequenceMaxWindowMs from SequenceProcessingDelay so response-time controls take effect immediately."
            ),
            check(
                "STP_018",
                "Test class exists and Gradle validation task is registered",
                SequenceTimingPolicyAuditor.testClassExists() && SequenceTimingPolicyAuditor.gradleTaskRegistered(),
                "Add SequenceTimingPolicyAuthorityV1Test and register validateLisaSequenceTimingPolicyV1 in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Sequence Timing Policy V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                SequenceTimingPolicyMetadata.TIMING_RULE,
                SequenceTimingPolicyMetadata.NO_EARLY_EXECUTION_RULE
            ),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped",
                "Article 3.3.1.3 — Reserved gestures not vocabulary selections"
            ),
            affectedLiecArticles = listOf(
                "Article 2.16 — Guided Learning teaches the real interface, not mock screens"
            ),
            affectedLvcArticles = listOf(
                "Article 3.37 — Sequence Timing Policy V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "One authoritative sequence-timing policy governs the whole app: the default 5s " +
                    "response time and idle-restart-per-blink rule apply identically in Guided " +
                    "Training and the normal Communication Workspace, and NO gesture — phrase, " +
                    "category, navigation, confirm, cancel, or Emergency — ever executes before the " +
                    "user has stopped blinking/winking for the full idle window. There is no " +
                    "early-execution/quick-resolve fast path left anywhere; reliability is " +
                    "prioritized over speed."
            } else {
                "${failed.size} Sequence Timing Policy V1 checks failed."
            },
            subsystem = "Sequence Timing Policy"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
