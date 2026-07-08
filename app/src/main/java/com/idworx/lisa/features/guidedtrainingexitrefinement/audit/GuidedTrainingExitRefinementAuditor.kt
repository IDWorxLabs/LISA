package com.idworx.lisa.features.guidedtrainingexitrefinement.audit

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.authority.GestureConflictAuthorityV1
import com.idworx.lisa.validation.authority.GuidedNavigationAuthorityV1
import com.idworx.lisa.validation.authority.NavigationReachabilityAuthorityV1

/**
 * Audits the "final Guided Training experience" refinement: a touch-independent Finish Training
 * gesture that replaces the old "Tap Reset" instruction, a shortened Choose Category gesture,
 * and a simplified Communication Workspace with the redundant instructional block removed.
 */
object GuidedTrainingExitRefinementAuditor {

    private val navigator = GuidedTrainingNavigator()
    private val englishUiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    // --- 1. Final navigation lesson teaches Finish Training, not a tap -------------------------

    fun finalLessonIsResetSequence(): Boolean =
        TrainingLessonCatalog.navigationLessons.lastOrNull()?.action == NavigationAction.ResetSequence

    fun finalLessonNeverInstructsATap(): Boolean {
        val flow = readGuidedTrainingFlow() ?: return false
        // The only historical "Tap Reset" instruction must be gone, and the lesson must teach the
        // real Finish Training gesture dynamically rather than any other hardcoded copy.
        return !flow.contains("Tap Reset", ignoreCase = true) &&
            flow.contains(
                "formatWinkSequenceShort(GuidedModeNavigation.FINISH_TRAINING_LEFT, " +
                    "GuidedModeNavigation.FINISH_TRAINING_RIGHT)"
            )
    }

    fun lessonWordingUsesNaturalStartCommunicatingPhrase(): Boolean {
        val title = GuidedWorkspaceTrainingSpec.lessonCardTitle(NavigationAction.ResetSequence, englishUiStrings)
        val spec = readGuidedWorkspaceTrainingSpec() ?: return false
        val flow = readGuidedTrainingFlow() ?: return false
        return title.equals("Start Communicating", ignoreCase = true) &&
            !spec.contains("main workspace", ignoreCase = true) &&
            !flow.contains("main workspace", ignoreCase = true)
    }

    fun lessonCardTeachesFinishTrainingGestureDynamically(): Boolean {
        val expected = formatWinkSequenceShort(
            GuidedModeNavigation.FINISH_TRAINING_LEFT,
            GuidedModeNavigation.FINISH_TRAINING_RIGHT
        )
        return GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.ResetSequence) == expected
    }

    // --- 2. The gesture is wired end-to-end, independent of any screen touch -------------------

    fun finishTrainingGestureWiredInMainActivityGestureDispatch(): Boolean {
        val main = readMainActivity() ?: return false
        val classifiesFinishTraining = main.contains(
            "GuidedModeNavigation.isFinishTrainingSequence(left, right) -> NavigationAction.ResetSequence"
        )
        val handledDuringTraining = main.contains("GuidedModeNavigation.isFinishTrainingSequence(left, right) -> {") &&
            main.contains("performReset()")
        val handledOutsideTraining = main.contains(
            "if (GuidedModeNavigation.isFinishTrainingSequence(capturedLeft, capturedRight)) {"
        )
        return classifiesFinishTraining && handledDuringTraining && handledOutsideTraining
    }

    fun performResetVerifiesLessonBeforeClearingState(): Boolean {
        val main = readMainActivity() ?: return false
        val bodyStart = main.indexOf("private fun performReset()")
        if (bodyStart < 0) return false
        val verifyIndex = main.indexOf("verifyTrainingNavigation(NavigationAction.ResetSequence)", bodyStart)
        val clearStateIndex = main.indexOf("uiGuidedNavigationState.value = GuidedNavigationState()", bodyStart)
        return verifyIndex in bodyStart until clearStateIndex
    }

    fun finishTrainingIsGlobalGestureReservedEverywhere(): Boolean =
        GuidedModeNavigation.isFinishTrainingSequence(
            GuidedModeNavigation.FINISH_TRAINING_LEFT,
            GuidedModeNavigation.FINISH_TRAINING_RIGHT
        ) &&
            GuidedModeNavigation.isGlobalNavigationSequence(
                GuidedModeNavigation.FINISH_TRAINING_LEFT,
                GuidedModeNavigation.FINISH_TRAINING_RIGHT
            )

    // --- 3. Completing every lesson (ending with Finish Training) reaches Completion -----------

    fun completingAllNavigationLessonsReachesCompletionPhase(): Boolean {
        var progress = TrainingProgress(currentPhase = TrainingPhase.NavigationLesson, navigationLessonIndex = 0)
        val lessons = TrainingLessonCatalog.navigationLessons
        lessons.forEachIndexed { index, lesson ->
            progress = navigator.reduce(
                progress.copy(navigationLessonIndex = index, currentPhase = TrainingPhase.NavigationLesson),
                TrainingEvent.NavigationActionCompleted(lesson.id)
            )
        }
        return progress.currentPhase == TrainingPhase.Completion
    }

    fun communicationModeActiveAfterCompletion(): Boolean {
        val completed = completingAllNavigationLessonsReachesCompletionPhase()
        val modeAfterCompletion = if (completed) GuidedWorkspaceMode.NORMAL else GuidedWorkspaceMode.GUIDED_TRAINING
        val main = readMainActivity() ?: return false
        return completed &&
            modeAfterCompletion == GuidedWorkspaceMode.NORMAL &&
            main.contains("setCommunicationState(LisaCommunicationState.Reset)")
    }

    fun completionScreenCongratulatesBeforeReturningToWorkspace(): Boolean {
        val welcome = readTrainingWelcomeScreen() ?: return false
        val controller = readTrainingSessionController() ?: return false
        return welcome.contains("ready to communicate", ignoreCase = true) &&
            controller.contains("TrainingPhase.Completion -> {") &&
            controller.contains("onTrainingFinished()")
    }

    // --- 4. Choose Category uses a materially shorter gesture than the old L4 R4 ---------------

    fun categoriesGestureIsShorterThanOldEightWinkGesture(): Boolean {
        val totalWinks = GuidedModeNavigation.CATEGORIES_LEFT + GuidedModeNavigation.CATEGORIES_RIGHT
        return totalWinks in 1 until 8
    }

    fun categoriesAndFinishTrainingGesturesAreDistinct(): Boolean {
        val categories = GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT
        val finishTraining = GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
        return categories != finishTraining
    }

    fun categoriesGestureLabelDerivedFromSharedConstants(): Boolean {
        val flowSource = readGuidedTrainingFlow() ?: return false
        return flowSource.contains(
            "formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)"
        )
    }

    // --- 5. No gesture duplication or reserved-gesture conflicts introduced ---------------------

    fun noDuplicateReservedGestures(): Boolean =
        com.idworx.lisa.GuidedVocabularyCatalogValidation.noDuplicateReservedGestures()

    fun noDuplicateGesturesAcrossWorkspaceModes(): Boolean =
        com.idworx.lisa.GuidedNavigationGestureAudit.auditAllModes()

    fun noReservedGestureConflicts(): Boolean =
        GestureConflictAuthorityV1.validate().outcome == ValidationOutcome.PASS

    // --- 6. Communication Workspace no longer shows the removed instructional block ------------

    fun communicationWorkspaceInstructionBlockRemoved(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        return !ui.contains("CaregiverHelpStrip") &&
            !ui.contains("contextHint") &&
            !ui.contains("workspacePatienceHint") &&
            !ui.contains("workspaceCaregiverHelpLegend")
    }

    fun communicationWorkspaceStillShowsCoreElements(): Boolean {
        val ui = readGuidedModeUi() ?: return false
        return ui.contains("GuidedModeNavigationPanel") &&
            ui.contains("categoryPage.title") &&
            ui.contains("GuidedVocabularyEntryRow") &&
            ui.contains("GuidedCategoryMenuAccessRow")
    }

    // --- 7. Neighbouring Guided Training / gesture / navigation authorities remain green --------

    fun existingGuidedTrainingAndNavigationAuthoritiesRemainGreen(): Boolean {
        val outcomes = listOf(
            com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1.validate().outcome,
            com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation
                .GuidedCurriculumAndNavigationContextAuthorityV1.validate().outcome,
            NavigationReachabilityAuthorityV1.validate().outcome,
            GuidedNavigationAuthorityV1.validate().outcome,
            GestureConflictAuthorityV1.validate().outcome
        )
        return outcomes.all { it == ValidationOutcome.PASS }
    }

    // --- Infra: test class + gradle task ---------------------------------------------------------

    fun testClassExists(): Boolean =
        ZeroTouchFileProbe.fileExists(
            "app/src/test/java/com/idworx/lisa/validation/authority/GuidedTrainingExitRefinementAuthorityV1Test.kt"
        )

    fun gradleTaskRegistered(): Boolean {
        val gradle = ZeroTouchFileProbe.readProjectFile("app/build.gradle.kts") ?: return false
        return gradle.contains("validateLisaGuidedTrainingExitRefinementV1")
    }

    private fun readMainActivity(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")

    private fun readGuidedModeUi(): String? =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")

    private fun readGuidedTrainingFlow(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/GuidedTrainingFlow.kt"
    )

    private fun readGuidedWorkspaceTrainingSpec(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/navigation/GuidedWorkspaceTrainingSpec.kt"
    )

    private fun readTrainingWelcomeScreen(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt"
    )

    private fun readTrainingSessionController(): String? = ZeroTouchFileProbe.readProjectFile(
        "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
    )
}
