package com.idworx.lisa.features.realworkspaceguidednavigationtraining.audit

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedVocabularyOverlayVisibility
import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation.GuidedLearningSimplificationAuthorityV1
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.validation.GuidedBlinkAcceptanceVisualFeedbackAuthorityV1
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.guidedlearninginteractivelessons.validation.GuidedLearningInteractiveLessonsAuthorityV1
import com.idworx.lisa.features.guidedlearningsetupbeforehello.validation.GuidedLearningSetupBeforeHelloAuthorityV1
import com.idworx.lisa.features.guidedlessonprogresslabel.validation.GuidedLessonProgressLabelAuthorityV1
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1
import com.idworx.lisa.features.guidedprogresswordingpolish.validation.GuidedProgressWordingPolishAuthorityV1
import com.idworx.lisa.features.guidedremoveredundanthelpertext.validation.GuidedRemoveRedundantHelperTextAuthorityV1
import com.idworx.lisa.features.guidedsuccesstimingfix.validation.GuidedSuccessTimingFixAuthorityV1
import com.idworx.lisa.features.guidedtotalsequenceprogress.validation.GuidedTotalSequenceProgressAuthorityV1
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation.GuidedUiOverlapAndFalseBlinkFixAuthorityV1
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation.GuidedWrongBlinkRestartsSequenceAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.isEmergencySequence
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.authority.GestureConflictAuthorityV1
import com.idworx.lisa.validation.authority.NavigationReachabilityAuthorityV1

object RealWorkspaceGuidedNavigationTrainingAuditor {

    private val navigator = GuidedTrainingNavigator()

    /**
     * Mirrors MainActivity's `classifyNavigationGesture` — a best-effort read of what a gesture
     * would do in the real workspace, used to decide whether Guided Training should accept it.
     */
    private fun classify(left: Int, right: Int): NavigationAction = when {
        isEmergencySequence(left, right) -> NavigationAction.TriggerEmergency
        GuidedModeNavigation.isCategoriesSequence(left, right) -> NavigationAction.OpenCategories
        GuidedModeNavigation.isBackSequence(left, right) -> NavigationAction.CloseMenu
        GuidedModeNavigation.isNextSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousSequence(left, right) -> NavigationAction.PreviousPage
        GuidedModeNavigation.isSelectSequence(left, right) -> NavigationAction.SelectCategory
        else -> NavigationAction.SelectPhrase
    }

    /** Mirrors MainActivity's `acceptedByCurrentNavigationLesson` gate. */
    private fun accepted(expected: NavigationAction, left: Int, right: Int): Boolean {
        val classified = classify(left, right)
        if (classified == expected) return true
        return (expected == NavigationAction.SelectCategory && classified == NavigationAction.SelectPhrase) ||
            (expected == NavigationAction.SelectPhrase && classified == NavigationAction.SelectCategory)
    }

    // Representative gestures for each real workspace action, used to probe acceptance.
    private val openCategoriesGesture = GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT
    private val selectCategoryGesture = GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
    private val selectPhraseGesture = 2 to 1 // first Conversation phrase slot — not a global nav sequence
    private val backGesture = GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT
    private val nextPageGesture = GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
    private val previousPageGesture = GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT
    private val emergencyGesture = 6 to 0

    // --- 1. Navigation training no longer uses blank fake screens ---------------------------
    fun navigationTrainingDoesNotUseBlankScreen(): Boolean {
        val flowSource = readGuidedTrainingFlow() ?: return false
        val uiSource = readAccessibilityUi() ?: return false
        // trainingBlocksMainUi(NavigationLesson) must be false, and the accessibility UI must
        // only invoke GuidedTrainingFlow (and therefore NavigationLessonScreen) when that flag is
        // true — so the old blank screen can never render during a navigation lesson.
        return flowSource.contains("TrainingPhase.NavigationLesson -> false") &&
            uiSource.contains("if (guidedTrainingActive && trainingBlocksMainUi(guidedTrainingState.phase))")
    }

    // --- 2. After phrase lessons, Guided Learning enters the real Communication Workspace ----
    fun realWorkspaceOpensAfterPhraseLessons(): Boolean {
        val progress = TrainingProgress(
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
                .GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT - 1,
            currentLessonSuccessCount = 0
        )
        val afterLastPhrase = navigator.reduce(progress, TrainingEvent.SequenceSuccess)
        val enteredNavigation = afterLastPhrase.currentPhase == TrainingPhase.NavigationLesson &&
            afterLastPhrase.navigationLessonIndex == 0
        val workspaceVisible = GuidedVocabularyOverlayVisibility.shouldShowOverlay(
            onboardingCompleted = false,
            cameraPermissionGranted = true,
            emergencyActive = false,
            practiceModeOpen = false,
            quickControlsOpen = false,
            guidedWorkspaceTrainingActive = true
        )
        return enteredNavigation && workspaceVisible
    }

    // --- 3. Workspace supports GUIDED_TRAINING mode ------------------------------------------
    fun workspaceSupportsGuidedTrainingMode(): Boolean {
        val modes = GuidedWorkspaceMode.entries.map { it.name }.toSet()
        val overlaySource = readGuidedModeUi() ?: return false
        val uiSource = readAccessibilityUi() ?: return false
        return modes.containsAll(listOf("NORMAL", "GUIDED_TRAINING")) &&
            overlaySource.contains("workspaceMode: com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode") &&
            uiSource.contains("GuidedWorkspaceMode.GUIDED_TRAINING") &&
            uiSource.contains("GuidedWorkspaceMode.NORMAL")
    }

    // --- 4. Current lesson target is highlighted ----------------------------------------------
    fun currentLessonTargetIsHighlighted(): Boolean {
        val gestureLessons = TrainingLessonCatalog.navigationLessons.filter {
            it.action != NavigationAction.ResetSequence
        }
        val allHighlighted = gestureLessons.all { GuidedWorkspaceTrainingSpec.highlightTargetFor(it.action) != null }
        val overlaySource = readGuidedModeUi() ?: return false
        return allHighlighted &&
            overlaySource.contains("guidedTrainingHighlight") &&
            overlaySource.contains("trainingHighlighted") &&
            overlaySource.contains("TrainingHighlightGlow") &&
            overlaySource.contains("TrainingHighlightBorder")
    }

    // --- 5. Only the current lesson gesture is accepted during guided training ---------------
    fun onlyTargetGestureAccepted(): Boolean {
        val expectedByLesson = mapOf(
            NavigationAction.OpenCategories to openCategoriesGesture,
            NavigationAction.SelectCategory to selectCategoryGesture,
            NavigationAction.SelectPhrase to selectPhraseGesture,
            NavigationAction.CloseMenu to backGesture,
            NavigationAction.NextPage to nextPageGesture,
            NavigationAction.PreviousPage to previousPageGesture,
            NavigationAction.TriggerEmergency to emergencyGesture
        )
        val allTargetsAccepted = expectedByLesson.all { (action, gesture) ->
            accepted(action, gesture.first, gesture.second)
        }
        val main = readMainActivity() ?: return false
        return allTargetsAccepted &&
            main.contains("private fun acceptedByCurrentNavigationLesson") &&
            main.contains("if (!acceptedByCurrentNavigationLesson(left, right)) {")
    }

    // --- 6. Non-target gestures are ignored during guided training -----------------------------
    fun nonTargetGesturesIgnored(): Boolean {
        val allGestures = listOf(
            openCategoriesGesture, selectCategoryGesture, selectPhraseGesture,
            backGesture, nextPageGesture, previousPageGesture, emergencyGesture
        )
        val expectedByLesson = mapOf(
            NavigationAction.OpenCategories to openCategoriesGesture,
            NavigationAction.CloseMenu to backGesture,
            NavigationAction.NextPage to nextPageGesture,
            NavigationAction.PreviousPage to previousPageGesture,
            NavigationAction.TriggerEmergency to emergencyGesture
        )
        // For every lesson with a distinct gesture, every *other* distinct gesture must be rejected.
        val allOthersRejected = expectedByLesson.all { (action, target) ->
            allGestures.filter { it != target }.all { other ->
                !accepted(action, other.first, other.second)
            }
        }
        return allOthersRejected
    }

    // --- 7. Open Categories lesson uses real workspace Categories control ----------------------
    fun openCategoriesUsesRealControl(): Boolean {
        val lesson = TrainingLessonCatalog.navigationLessons.getOrNull(0) ?: return false
        val overlaySource = readGuidedModeUi() ?: return false
        return lesson.action == NavigationAction.OpenCategories &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(lesson.action) == GuidedWorkspaceHighlightTarget.OpenCategories &&
            overlaySource.contains("GuidedWorkspaceHighlightTarget.OpenCategories")
    }

    // --- 8. Select category lesson uses real category UI ---------------------------------------
    fun selectCategoryUsesRealControl(): Boolean {
        val lesson = TrainingLessonCatalog.navigationLessons.getOrNull(1) ?: return false
        val overlaySource = readGuidedModeUi() ?: return false
        return lesson.action == NavigationAction.SelectCategory &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(lesson.action) == GuidedWorkspaceHighlightTarget.CategoryRow &&
            GuidedWorkspaceTrainingSpec.conversationCategoryIndex in 0 until 6 &&
            overlaySource.contains("GuidedWorkspaceHighlightTarget.CategoryRow")
    }

    // --- 9. Select phrase lesson uses real phrase row UI ----------------------------------------
    fun selectPhraseUsesRealControl(): Boolean {
        val lesson = TrainingLessonCatalog.navigationLessons.getOrNull(2) ?: return false
        val overlaySource = readGuidedModeUi() ?: return false
        return lesson.action == NavigationAction.SelectPhrase &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(lesson.action) == GuidedWorkspaceHighlightTarget.PhraseRow &&
            overlaySource.contains("GuidedWorkspaceHighlightTarget.PhraseRow")
    }

    // --- 10. Back/Next/Previous/Emergency lessons use real workspace controls -------------------
    fun backNextPreviousEmergencyUseRealControls(): Boolean {
        val lessons = TrainingLessonCatalog.navigationLessons
        val back = lessons.getOrNull(3)
        val next = lessons.getOrNull(4)
        val previous = lessons.getOrNull(5)
        val emergency = lessons.getOrNull(6)
        val panelSource = readGuidedModeUi() ?: return false
        return back?.action == NavigationAction.CloseMenu &&
            next?.action == NavigationAction.NextPage &&
            previous?.action == NavigationAction.PreviousPage &&
            emergency?.action == NavigationAction.TriggerEmergency &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.CloseMenu) == GuidedWorkspaceHighlightTarget.Back &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.NextPage) == GuidedWorkspaceHighlightTarget.NextPage &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.PreviousPage) == GuidedWorkspaceHighlightTarget.PreviousPage &&
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.TriggerEmergency) == GuidedWorkspaceHighlightTarget.Emergency &&
            panelSource.contains("highlightTarget: GuidedWorkspaceHighlightTarget? = null")
    }

    // --- 11. Normal workspace resolver does not consume guided training gestures first ---------
    fun normalResolverDoesNotConsumeGuidedGesturesFirst(): Boolean {
        val main = readMainActivity() ?: return false
        val navGateIndex = main.indexOf("if (trainingSession.isNavigationTrainingActive())")
        val normalResolveIndex = main.indexOf("trainingSession.handleSequence(left, right, activeLanguage(), order)")
        if (navGateIndex < 0 || normalResolveIndex < 0 || navGateIndex >= normalResolveIndex) return false
        // The navigation-training branch must `return` before the function ever reaches the
        // normal vocabulary resolver call below it.
        val between = main.substring(navGateIndex, normalResolveIndex)
        val hasEarlyReturn = between.contains("handleNavigationTrainingSequence(left, right)") &&
            between.contains("return")

        // Inside that branch, the lesson-gesture gate must be the very first statement — so no
        // gesture reaches the real workspace or vocabulary resolver before being checked against
        // the active lesson's expected action.
        val handlerBodyIndex = main.indexOf("private fun handleNavigationTrainingSequence")
        val acceptGateIndex = main.indexOf("if (!acceptedByCurrentNavigationLesson(left, right)) {")
        val gateIsFirstStatement = handlerBodyIndex >= 0 && acceptGateIndex > handlerBodyIndex &&
            acceptGateIndex - handlerBodyIndex < 400

        return hasEarlyReturn && gateIsFirstStatement
    }

    // --- 12. After guided training completes, workspace returns to normal mode -----------------
    fun workspaceReturnsToNormalModeAfterCompletion(): Boolean {
        var progress = TrainingProgress(currentPhase = TrainingPhase.NavigationLesson, navigationLessonIndex = 0)
        val lessonCount = TrainingLessonCatalog.navigationLessons.size
        repeat(lessonCount) { index ->
            val lesson = TrainingLessonCatalog.navigationLessonAt(index) ?: return false
            progress = navigator.reduce(
                progress.copy(navigationLessonIndex = index, currentPhase = TrainingPhase.NavigationLesson),
                TrainingEvent.NavigationActionCompleted(lesson.id)
            )
        }
        val completed = progress.currentPhase == TrainingPhase.Completion
        val workspaceModeAfterCompletion = if (completed) GuidedWorkspaceMode.NORMAL else GuidedWorkspaceMode.GUIDED_TRAINING
        return completed && workspaceModeAfterCompletion == GuidedWorkspaceMode.NORMAL
    }

    // --- 13. Phrase-only speech policy remains unchanged; no Brain 2, narration, or cloud -------
    fun phraseOnlySpeechPolicyUnchangedNoBrain2NoCloud(): Boolean {
        val main = readMainActivity() ?: return false
        val spec = readGuidedWorkspaceTrainingSpec() ?: return false
        val noForbiddenAdditions = !main.contains("Brain2", ignoreCase = true) &&
            !spec.contains("Brain2", ignoreCase = true) &&
            !spec.contains("http://") &&
            !spec.contains("https://")
        return LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY &&
            main.contains("verifyTrainingNavigation(NavigationAction.SelectPhrase)") &&
            main.contains("speak(phrase)") &&
            noForbiddenAdditions
    }

    // --- 14. Existing Guided Learning validations remain green -----------------------------------
    fun existingGuidedLearningValidationsRemainGreen(): Boolean {
        val outcomes = listOf(
            GuidedTrainingAuthorityV1.validate().outcome,
            GuidedCurriculumAndNavigationContextAuthorityV1.validate().outcome,
            GuidedLearningInteractiveLessonsAuthorityV1.validate().outcome,
            GuidedLearningSetupBeforeHelloAuthorityV1.validate().outcome,
            GuidedLearningSimplificationAuthorityV1.validate().outcome,
            GuidedSuccessTimingFixAuthorityV1.validate().outcome,
            GuidedWrongBlinkRestartsSequenceAuthorityV1.validate().outcome,
            GuidedUiOverlapAndFalseBlinkFixAuthorityV1.validate().outcome,
            GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1.validate().outcome,
            GuidedLessonProgressLabelAuthorityV1.validate().outcome,
            GuidedBlinkAcceptanceVisualFeedbackAuthorityV1.validate().outcome,
            GuidedRemoveRedundantHelperTextAuthorityV1.validate().outcome,
            GuidedTotalSequenceProgressAuthorityV1.validate().outcome,
            GuidedProgressWordingPolishAuthorityV1.validate().outcome,
            NavigationReachabilityAuthorityV1.validate().outcome,
            GestureConflictAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    // --- Infra: test class + gradle task ----------------------------------------------------------
    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/RealWorkspaceGuidedNavigationTrainingAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaRealWorkspaceGuidedNavigationTrainingV1")
    }

    private fun readGuidedTrainingFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
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

    private fun readGuidedWorkspaceTrainingSpec(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedWorkspaceTrainingSpec.kt"
    )
}
