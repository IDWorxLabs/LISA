package com.idworx.lisa.features.guidedtraininglessonfocus.audit

import com.idworx.lisa.features.guidedtrainingclarityandtiming.validation.GuidedTrainingClarityAndTimingAuthorityV1
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingFocusPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.validation.ValidationOutcome

object GuidedTrainingLessonFocusAuditor {

    /** `handleNavigationTrainingSequence`'s body, isolated so ordering checks never spill into unrelated functions. */
    private fun handlerBody(main: String): String =
        main.substringAfter("private fun handleNavigationTrainingSequence(left: Int, right: Int) {")
            .substringBefore("private fun verifyTrainingNavigation")

    // --- 1. During "Open Categories", phrase gestures are blocked and do not speak phrases ------
    fun phraseGesturesBlockedDuringUnrelatedLesson(): Boolean {
        // Pure policy check: a phrase-classified attempt is never allowed while a different,
        // single-target lesson (e.g. Open Categories) is active — no highlighted-row exception
        // applies to non-Select actions.
        val phraseAttemptDuringOpenCategories = !GuidedTrainingFocusPolicy.isTargetAllowed(
            expected = NavigationAction.OpenCategories,
            attemptedTarget = NavigationAction.SelectPhrase
        )
        val main = readMainActivity() ?: return false
        val body = handlerBody(main)
        val gateBlocksBeforeAnythingElse = body.contains("if (!acceptedByCurrentNavigationLesson(left, right)) {") &&
            body.contains("rejectNavigationTrainingGesture()") &&
            body.indexOf("if (!acceptedByCurrentNavigationLesson(left, right)) {") < body.indexOf("when {")

        // The reject path never calls dispatch/verifyTrainingNavigation/speak — it only shows
        // feedback and resets the sequence, so no phrase can be spoken from a blocked attempt.
        val rejectFn = main.substringAfter("private fun rejectNavigationTrainingGesture() {")
            .substringBefore("private fun handleNavigationTrainingSequence")
        val rejectNeverAdvancesOrSpeaks = rejectFn.contains("applyNavigationWrongGestureFeedback()") &&
            rejectFn.contains("resetSequence()") &&
            !rejectFn.contains("verifyTrainingNavigation") &&
            !rejectFn.contains("speak(")
        return phraseAttemptDuringOpenCategories && gateBlocksBeforeAnythingElse && rejectNeverAdvancesOrSpeaks
    }

    // --- 2. During "Select phrase", only the expected (highlighted) phrase can be selected -------
    fun onlyHighlightedPhraseSelectableDuringSelectPhraseLesson(): Boolean {
        val highlightedPhraseAllowed = GuidedTrainingFocusPolicy.isTargetAllowed(
            expected = NavigationAction.SelectPhrase,
            attemptedTarget = NavigationAction.SelectPhrase,
            isAttemptedTargetHighlighted = true
        )
        val nonHighlightedPhraseBlocked = !GuidedTrainingFocusPolicy.isTargetAllowed(
            expected = NavigationAction.SelectPhrase,
            attemptedTarget = NavigationAction.SelectPhrase,
            isAttemptedTargetHighlighted = false
        )
        val main = readMainActivity() ?: return false
        val wiredForSelectPhraseUsingHighlightedEntry = main.contains("private fun isNavigationLessonOffTargetAttempt") &&
            main.contains("WorkspacePhraseResolver.visibleEntriesForState(") &&
            main.contains("highlightedEntry != null &&") &&
            main.contains("highlightedEntry.left == left && highlightedEntry.right == right")
        val body = handlerBody(main)
        val gateAppliesBeforeExecution = body.contains("if (isNavigationLessonOffTargetAttempt(left, right)) {") &&
            body.indexOf("if (isNavigationLessonOffTargetAttempt(left, right)) {") < body.indexOf("when {")
        return highlightedPhraseAllowed && nonHighlightedPhraseBlocked &&
            wiredForSelectPhraseUsingHighlightedEntry && gateAppliesBeforeExecution
    }

    // --- 3. Wrong gesture during workspace lesson shows red wrong-sequence feedback ---------------
    fun wrongGestureShowsRedFeedback(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        val controllerHasWrongFeedbackFn = controller.contains("fun applyNavigationWrongGestureFeedback()") &&
            controller.contains("GuidedFeedbackPhrases.wrongGesture()") &&
            controller.contains("navigationWrongGestureMessage = message")
        val state = readGuidedTrainingUiState()?.contains("val navigationWrongGestureMessage: String? = null") == true
        val card = readTrainingComponents() ?: return false
        val cardRendersRed = card.contains("wrongGestureMessage: String? = null") &&
            card.contains("wrongGestureMessage != null -> LisaEmergencyRed")
        val ui = readAccessibilityUi() ?: return false
        val wired = ui.contains("wrongGestureMessage = guidedTrainingState.navigationWrongGestureMessage")
        return controllerHasWrongFeedbackFn && state && cardRendersRed && wired
    }

    // --- 4. Wrong gesture resets the active sequence without advancing the lesson ------------------
    fun wrongGestureResetsSequenceWithoutAdvancing(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        // The feedback-only function never dispatches a NavigationActionCompleted event, so lesson
        // progress can never advance from a rejected attempt.
        val feedbackFn = controller.substringAfter("fun applyNavigationWrongGestureFeedback() {")
            .substringBefore("fun updatePreferences(")
        val neverAdvancesProgress = !feedbackFn.contains("dispatch(TrainingEvent.NavigationActionCompleted")
        val main = readMainActivity() ?: return false
        val resetsBlinkBuffer = main.contains("private fun rejectNavigationTrainingGesture() {") &&
            main.contains("resetSequence()")
        return neverAdvancesProgress && resetsBlinkBuffer
    }

    // --- 5. Non-target dimmed items are also functionally inactive ----------------------------------
    fun dimmedItemsAreFunctionallyInactive(): Boolean {
        val overlay = readGuidedModeUi() ?: return false
        val clickableGatedByDim = overlay.contains(".clickable(role = Role.Button, enabled = !trainingDimmed, onClick = onClick)")
        val entryButtonGatedByDim = overlay.contains("enabled = enabled && !trainingDimmed,")
        // Every dimmable row/button passes trainingDimmed through to its own clickable — the
        // phrase and category-menu list rows plus the two navigation-panel button styles (four
        // call sites in total; the redundant in-list "Choose Category" access row was removed as
        // a duplicate of the Navigation Panel's own Categories button).
        val dimUsageCount = Regex("guidedTrainingDim\\(trainingDimmed\\)").findAll(overlay).count()
        return clickableGatedByDim && entryButtonGatedByDim && dimUsageCount >= 4
    }

    // --- 6. Normal workspace outside Guided Training still allows normal phrase speaking ----------
    fun normalWorkspaceUnaffectedOutsideGuidedTraining(): Boolean {
        // Outside a lesson there is no expected action, so the focus policy is never consulted —
        // every real action classification is allowed through untouched.
        val noExpectedLessonAllowsEverything = NavigationAction.entries.all { action ->
            GuidedTrainingFocusPolicy.isTargetAllowed(expected = action, attemptedTarget = action)
        }
        val overlay = readGuidedModeUi() ?: return false
        // Dimming (and therefore the new functional gating) only ever activates in GUIDED_TRAINING
        // mode with an active highlight — never in NORMAL workspace use.
        val dimOnlyActiveInGuidedTraining = overlay.contains(
            "val trainingDimActive = workspaceMode == com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode.GUIDED_TRAINING &&"
        )
        val main = readMainActivity() ?: return false
        val touchGateOnlyWhenTrainingActive = main.contains("if (trainingSession.isNavigationTrainingActive() &&") &&
            main.contains("if (trainingSession.isNavigationTrainingActive()) {") &&
            main.contains("val expected = trainingSession.expectedNavigationAction()")
        return noExpectedLessonAllowsEverything && dimOnlyActiveInGuidedTraining && touchGateOnlyWhenTrainingActive
    }

    // --- 7. Correct expected gesture still completes the lesson -------------------------------------
    fun correctExpectedGestureStillCompletesLesson(): Boolean {
        val allSingleTargetLessonsStillAccepted = NavigationAction.entries.all { action ->
            GuidedTrainingFocusPolicy.isTargetAllowed(expected = action, attemptedTarget = action, isAttemptedTargetHighlighted = true)
        }
        val main = readMainActivity() ?: return false
        val body = handlerBody(main)
        val bothGatesPassBeforeExecution = body.contains("if (!acceptedByCurrentNavigationLesson(left, right)) {") &&
            body.contains("if (isNavigationLessonOffTargetAttempt(left, right)) {") &&
            body.indexOf("if (!acceptedByCurrentNavigationLesson(left, right)) {") <
                body.indexOf("if (isNavigationLessonOffTargetAttempt(left, right)) {") &&
            body.indexOf("if (isNavigationLessonOffTargetAttempt(left, right)) {") < body.indexOf("when {")
        return allSingleTargetLessonsStillAccepted && bothGatesPassBeforeExecution
    }

    // --- 8. Existing response-time and positive-feedback behavior still passes ---------------------
    fun existingResponseTimeAndPositiveFeedbackStillPasses(): Boolean =
        GuidedTrainingClarityAndTimingAuthorityV1.validate().outcome == ValidationOutcome.PASS

    /** `triggerGuidedEmergencyTouch`'s body, isolated for ordering/no-real-alarm checks. */
    private fun emergencyTouchFn(main: String): String =
        main.substringAfter("private fun triggerGuidedEmergencyTouch() {")
            .substringBefore("private fun executeGuidedOverlayAction(")

    // --- 9. Emergency touch is rejected when another lesson is active, via the SAME policy --------
    fun emergencyTouchRejectedWhenOffTarget(): Boolean {
        // Emergency is governed by the exact same isTargetAllowed used for every other real
        // workspace target — no dedicated Emergency validator.
        val emergencyBlockedDuringUnrelatedLesson = !GuidedTrainingFocusPolicy.isTargetAllowed(
            expected = NavigationAction.OpenCategories,
            attemptedTarget = NavigationAction.TriggerEmergency
        )
        val main = readMainActivity() ?: return false
        val fn = emergencyTouchFn(main)
        val routesThroughFocusPolicyBeforeAnyRealAction = fn.contains("if (trainingSession.isNavigationTrainingActive()) {") &&
            fn.contains("GuidedTrainingFocusPolicy.isTargetAllowed(expected, NavigationAction.TriggerEmergency)") &&
            fn.indexOf("GuidedTrainingFocusPolicy.isTargetAllowed(expected, NavigationAction.TriggerEmergency)") <
                fn.indexOf("trainingSession.beginEmergencyConfirm()")
        return emergencyBlockedDuringUnrelatedLesson && routesThroughFocusPolicyBeforeAnyRealAction
    }

    // --- 10. Emergency touch shows red feedback + resets the sequence, never advances progress -----
    fun emergencyTouchShowsRedFeedbackAndResetsWithoutAdvancing(): Boolean {
        val main = readMainActivity() ?: return false
        val fn = emergencyTouchFn(main)
        // The off-target branch calls the exact same reject helper every other lesson uses —
        // which shows the red wrong-gesture feedback and resets the sequence (verified by GTLF_003
        // and GTLF_004 already) — and never reaches beginEmergencyConfirm() or
        // verifyTrainingNavigation() from that branch.
        val rejectBranch = fn.substringAfter("if (!allowed) {").substringBefore("}")
        val usesSharedRejectHelper = rejectBranch.contains("rejectNavigationTrainingGesture()")
        val rejectBranchNeverExecutesEmergency = !rejectBranch.contains("beginEmergencyConfirm") &&
            !rejectBranch.contains("verifyTrainingNavigation")
        return usesSharedRejectHelper && rejectBranchNeverExecutesEmergency
    }

    // --- 11. Emergency touch succeeds during the Emergency lesson, without the real alarm flow -----
    fun emergencyTouchSucceedsDuringEmergencyLesson(): Boolean {
        val emergencyAllowedDuringEmergencyLesson = GuidedTrainingFocusPolicy.isTargetAllowed(
            expected = NavigationAction.TriggerEmergency,
            attemptedTarget = NavigationAction.TriggerEmergency
        )
        val main = readMainActivity() ?: return false
        val fn = emergencyTouchFn(main)
        // The allowed path completes the lesson via the same verifyTrainingNavigation every other
        // lesson uses (preserving existing positive-feedback/completion behavior) and never starts
        // the real Brain1 confirm flow during training.
        val allowedPath = fn.substringAfter("if (!allowed) {").substringAfter("}")
            .substringBefore("leftWinks = EMERGENCY_LEFT_WINKS")
        val completesLessonSafely = allowedPath.contains("verifyTrainingNavigation(NavigationAction.TriggerEmergency)") &&
            !allowedPath.contains("beginEmergencyConfirm")
        return emergencyAllowedDuringEmergencyLesson && completesLessonSafely
    }

    // --- 12. Existing blink-path Emergency behavior is unchanged ------------------------------------
    fun blinkPathEmergencyBehaviorUnchanged(): Boolean {
        val main = readMainActivity() ?: return false
        val body = handlerBody(main)
        val branch = body.substringAfter("isEmergencySequence(left, right) -> {").substringBefore("}")
        return body.contains("isEmergencySequence(left, right) -> {") &&
            body.indexOf("isEmergencySequence(left, right) -> {") <
                body.indexOf("verifyTrainingNavigation(NavigationAction.TriggerEmergency)") &&
            branch.contains("verifyTrainingNavigation(NavigationAction.TriggerEmergency)") &&
            !branch.contains("beginEmergencyConfirm")
    }

    // --- Infra: test class + gradle task ------------------------------------------------------------
    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedTrainingLessonFocusAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedTrainingLessonFocusV1")
    }

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readTrainingSessionController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readGuidedTrainingUiState(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/state/GuidedTrainingUiState.kt"
    )

    private fun readTrainingComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )

    private fun readGuidedModeUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt"
    )
}
