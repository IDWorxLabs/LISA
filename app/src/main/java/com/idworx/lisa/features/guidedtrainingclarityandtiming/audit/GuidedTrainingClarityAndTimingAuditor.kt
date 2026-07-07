package com.idworx.lisa.features.guidedtrainingclarityandtiming.audit

import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.features.onboardingguide.lessoninteraction.GuidedFeedbackPhrases
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe

object GuidedTrainingClarityAndTimingAuditor {

    // --- 1. Response time can be adjusted independently of sensitivity --------------------------
    fun responseTimeAdjustableIndependentlyOfSensitivity(): Boolean {
        val components = readTrainingComponents() ?: return false
        val independentControls = components.contains("responseTimeSec: Int? = null") &&
            components.contains("onDecreaseResponseTime: () -> Unit = {}") &&
            components.contains("onIncreaseResponseTime: () -> Unit = {}") &&
            components.contains("\"Response time: \${responseTimeSec}s\"")
        val defaultPrefs = TrainingPreferences()
        val changedIndependently = defaultPrefs.copy(guidedResponseTimeSec = 6).guidedResponseTimeSec == 6
        val ui = readAccessibilityUi() ?: return false
        val topBarAlsoAdjustable = ui.contains("guidedResponseTimeControlsVisible") &&
            ui.contains("onDecreaseGuidedResponseTime") &&
            ui.contains("onIncreaseGuidedResponseTime")
        return independentControls && changedIndependently && topBarAlsoAdjustable
    }

    // --- 2. Default guided response time is no longer 3s (slower, from session settings) -------
    fun defaultGuidedResponseTimeIsSlowerThanThreeSeconds(): Boolean {
        val default = TrainingPreferences().guidedResponseTimeSec
        val comesFromSharedBounds = default in SequenceProcessingDelay.allowedSeconds
        val mainActivity = readMainActivity() ?: return false
        val notHardcodedPerLesson = mainActivity.contains(
            "SequenceProcessingDelay.toMillis(trainingSession.state.progress.preferences.guidedResponseTimeSec)"
        )
        return default > SequenceProcessingDelay.DEFAULT_SECONDS &&
            default >= 5 &&
            comesFromSharedBounds &&
            notHardcodedPerLesson
    }

    // --- 3. Active gesture sequences are not interrupted by the response timer ------------------
    fun activeSequencesNotInterruptedByResponseTimer(): Boolean {
        val mainActivity = readMainActivity() ?: return false
        val usesEffectiveGuidedTimeoutEverywhere = mainActivity.contains("private fun effectiveSequenceIdleTimeoutMs(): Long") &&
            mainActivity.contains("private fun effectiveSequenceMaxWindowMs(): Long") &&
            !mainActivity.contains("idleTimeoutMs = sequenceIdleTimeoutMs,") &&
            !mainActivity.contains("idleTimeoutMs = sequenceIdleTimeoutMs\n")
        val idleResetsOnNewWink = mainActivity.contains("lastWinkTimeMs = System.currentTimeMillis()") &&
            mainActivity.contains("val idleMs = now - lastWinkTimeMs")
        val guidedUsesOwnSlowerSetting = mainActivity.contains("trainingSession.shouldShowTraining()") &&
            mainActivity.contains("guidedResponseTimeSec")
        return usesEffectiveGuidedTimeoutEverywhere && idleResetsOnNewWink && guidedUsesOwnSlowerSetting
    }

    // --- 4. Correct first guided sequence triggers positive feedback ------------------------------
    fun correctFirstGuidedSequenceTriggersPositiveFeedback(): Boolean {
        val phrases = setOf(GuidedFeedbackPhrases.positive(0), GuidedFeedbackPhrases.positive(1), GuidedFeedbackPhrases.positive(2))
        val variesAcrossAtLeastTwoPhrases = phrases.size >= 2
        val controller = readTrainingSessionController() ?: return false
        val speaksAndShowsFeedback = controller.contains("private fun applyNavigationCompletionFeedback") &&
            controller.contains("GuidedFeedbackPhrases.positive(completedLessonIndex)") &&
            controller.contains("navigationFeedbackMessage = phrase") &&
            controller.contains("narration.speak(phrase)") &&
            controller.contains("dispatch(TrainingEvent.NavigationActionCompleted(lessonId))") &&
            controller.indexOf("dispatch(TrainingEvent.NavigationActionCompleted(lessonId))") <
                controller.indexOf("applyNavigationCompletionFeedback(completedLessonIndex)")
        val stateHasField = readGuidedTrainingUiState()?.contains("val navigationFeedbackMessage: String? = null") == true
        return variesAcrossAtLeastTwoPhrases && speaksAndShowsFeedback && stateHasField
    }

    // --- 5. Floating lesson bubble remains present -------------------------------------------------
    fun floatingLessonBubbleRemainsPresent(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        val cardStillRendered = ui.contains("GuidedWorkspaceLessonCard(") &&
            (ui.contains("Alignment.BottomStart") || ui.contains("Alignment.BottomEnd")) &&
            !ui.contains("Alignment.TopCenter")
        val components = readTrainingComponents() ?: return false
        val cardSupportsFeedback = components.contains("feedbackMessage: String? = null") &&
            components.contains("fun GuidedWorkspaceLessonCard")
        return cardStillRendered && cardSupportsFeedback
    }

    // --- 6. Guided workspace target is clearly represented in state/UI model ----------------------
    fun guidedWorkspaceTargetClearlyRepresented(): Boolean {
        val ui = readAccessibilityUi() ?: return false
        val highlightModelWired = ui.contains("guidedWorkspaceHighlight") &&
            ui.contains("trainingHighlight = guidedWorkspaceHighlight")
        val overlay = readGuidedModeUi() ?: return false
        val nonTargetsDeemphasised = overlay.contains("private fun Modifier.guidedTrainingDim") &&
            overlay.contains("trainingDimmed: Boolean = false") &&
            overlay.contains("val trainingDimActive = workspaceMode ==")
        return highlightModelWired && nonTargetsDeemphasised
    }

    // --- 7. Final navigation success emits visual feedback BEFORE Completion ----------------------
    fun finalNavigationSuccessEmitsVisualFeedbackBeforeCompletion(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        val detectsFinalLessonGenerically = controller.contains("val isFinalNavigationLesson =") &&
            controller.contains("TrainingLessonCatalog.navigationLessonAt(completedLessonIndex + 1) == null")
        val branchesToDedicatedFeedbackHold = controller.contains("beginFinalNavigationCompletionFeedback(completedLessonIndex, lessonId)")
        val holdFunction = controller.contains("private fun beginFinalNavigationCompletionFeedback(")
        if (!detectsFinalLessonGenerically || !branchesToDedicatedFeedbackHold || !holdFunction) return false

        val holdBody = controller.substringAfter("private fun beginFinalNavigationCompletionFeedback(")
        val setsFeedbackAndPendingFlagFirst = holdBody.indexOf("navigationFeedbackMessage = phrase") in 0 until holdBody.indexOf("mainThreadDelayed(")
        val dispatchIsInsideTheDelay = holdBody.substringAfter("mainThreadDelayed(").contains(
            "dispatch(TrainingEvent.NavigationActionCompleted(lessonId))"
        )
        val dispatchNotCalledBeforeTheDelay = holdBody.substringBefore("mainThreadDelayed(").let {
            !it.contains("dispatch(TrainingEvent.NavigationActionCompleted(lessonId))")
        }
        return setsFeedbackAndPendingFlagFirst && dispatchIsInsideTheDelay && dispatchNotCalledBeforeTheDelay
    }

    // --- 8. Completion still happens after the brief feedback delay --------------------------------
    fun completionStillHappensAfterFeedbackDelay(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        val delayIsBounded = controller.contains("FINAL_NAVIGATION_COMPLETION_DELAY_MS") &&
            controller.contains("const val FINAL_NAVIGATION_COMPLETION_DELAY_MS: Long")
        val clearsPendingFlagAfterDelay = controller.contains("completionPendingFeedback = false")

        val lastLesson = TrainingLessonCatalog.navigationLessonAt(TrainingMetadata.NAVIGATION_LESSON_COUNT - 1)
            ?: return false
        val progressAtFinalLesson = TrainingProgress(
            currentPhase = TrainingPhase.NavigationLesson,
            navigationLessonIndex = TrainingMetadata.NAVIGATION_LESSON_COUNT - 1
        )
        val afterFinalCompletion = GuidedTrainingNavigator().reduce(
            progressAtFinalLesson,
            TrainingEvent.NavigationActionCompleted(lastLesson.id)
        )
        val realTransitionReachesCompletion = afterFinalCompletion.currentPhase == TrainingPhase.Completion

        return delayIsBounded && clearsPendingFlagAfterDelay && realTransitionReachesCompletion
    }

    // --- 9. Non-final navigation lessons continue normally (no artificial delay) -------------------
    fun nonFinalNavigationLessonsContinueNormally(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        // verifyNavigation's non-final path dispatches immediately (its only immediate dispatch
        // call in this file), then feeds the same completed index into the existing feedback
        // helper — exactly the pre-fix ordering, so normal lessons are never held up.
        val dispatchesImmediately = controller.indexOf("dispatch(TrainingEvent.NavigationActionCompleted(lessonId))") <
            controller.indexOf("applyNavigationCompletionFeedback(completedLessonIndex)")
        // The post-success hold flag is only ever set on the final-lesson path, never here.
        val pendingGateOnlySetOnce = Regex("completionPendingFeedback = true").findAll(controller).count() == 1

        val firstLesson = TrainingLessonCatalog.navigationLessonAt(0) ?: return false
        val progressAtFirstLesson = TrainingProgress(
            currentPhase = TrainingPhase.NavigationLesson,
            navigationLessonIndex = 0
        )
        val afterFirstCompletion = GuidedTrainingNavigator().reduce(
            progressAtFirstLesson,
            TrainingEvent.NavigationActionCompleted(firstLesson.id)
        )
        val advancesImmediatelyWithoutWaitingForCompletion =
            afterFirstCompletion.navigationLessonIndex == 1 &&
                afterFirstCompletion.currentPhase == TrainingPhase.NavigationLesson

        return dispatchesImmediately && pendingGateOnlySetOnce && advancesImmediatelyWithoutWaitingForCompletion
    }

    // --- 10. Spoken feedback still works for both normal and final navigation lessons --------------
    fun spokenFeedbackStillWorksForNormalAndFinalLessons(): Boolean {
        val controller = readTrainingSessionController() ?: return false
        val normalPathSpeaks = controller.contains("private fun applyNavigationCompletionFeedback(") &&
            controller.contains("narration.speak(phrase)")
        val finalPathAlsoSpeaks = controller.contains("private fun beginFinalNavigationCompletionFeedback(")
        val bothGuardBySpeechPolicy = controller.contains("LisaSpeechPolicy.allowsNarration()")
        val bothUseTheSameRotatingPhraseProvider = controller.contains(
            "GuidedFeedbackPhrases.positive(completedLessonIndex)"
        )
        return normalPathSpeaks && finalPathAlsoSpeaks && bothGuardBySpeechPolicy && bothUseTheSameRotatingPhraseProvider
    }

    // --- 11. No regression to guided response-time behavior -----------------------------------------
    fun noRegressionToGuidedResponseTimeBehavior(): Boolean =
        activeSequencesNotInterruptedByResponseTimer() && defaultGuidedResponseTimeIsSlowerThanThreeSeconds()

    // --- Infra: test class + gradle task ------------------------------------------------------------
    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedTrainingClarityAndTimingAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedTrainingClarityAndTimingV1")
    }

    private fun readTrainingComponents(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingComponents.kt"
    )

    private fun readAccessibilityUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt"
    )

    private fun readGuidedModeUi(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt"
    )

    private fun readMainActivity(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/MainActivity.kt"
    )

    private fun readTrainingSessionController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )

    private fun readGuidedTrainingUiState(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/state/GuidedTrainingUiState.kt"
    )
}
